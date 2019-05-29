/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.sony.internal.scalarweb.models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.sony.internal.net.Header;
import org.openhab.binding.sony.internal.net.NetUtil;
import org.openhab.binding.sony.internal.scalarweb.ScalarUtilities;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebConstants;
import org.openhab.binding.sony.internal.scalarweb.models.api.ActRegisterId;
import org.openhab.binding.sony.internal.scalarweb.models.api.ActRegisterOptions;
import org.openhab.binding.sony.internal.scalarweb.models.api.MethodTypes;
import org.openhab.binding.sony.internal.scalarweb.models.api.ServiceProtocol;
import org.openhab.binding.sony.internal.scalarweb.models.api.SupportedApi;
import org.openhab.binding.sony.internal.scalarweb.models.api.SupportedApiInfo;
import org.openhab.binding.sony.internal.scalarweb.models.api.SupportedApiVersionInfo;
import org.openhab.binding.sony.internal.scalarweb.transports.SonyTransport;
import org.openhab.binding.sony.internal.scalarweb.transports.SonyTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the different web services available
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarWebService implements AutoCloseable {

    /** The logger. */
    private Logger logger = LoggerFactory.getLogger(ScalarWebService.class);

    // The various well know service names (must be unique as they are channel groups)
    public static final String ACCESSCONTROL = "accessControl";
    public static final String APPCONTROL = "appControl";
    public static final String AUDIO = "audio";
    public static final String AVCONTENT = "avContent";
    public static final String BROWSER = "browser";
    public static final String CEC = "cec";
    public static final String CONTENTSHARE = "contentshare";
    public static final String ENCRYPTION = "encryption";
    public static final String GUIDE = "guide";
    public static final String SYSTEM = "system";
    public static final String VIDEOSCREEN = "videoScreen";

    /** The service name */
    private final String serviceName;

    /** The service version */
    private final String version;

    /** Transport Factory */
    private final SonyTransportFactory transportFactory;

    /** Transport used for communication */
    private final SonyTransport<ScalarWebRequest> transport;

    /** The API supported by this service */
    private final SupportedApi supportedApi;

    /** The current request identifier */
    private int currId = 1;

    /**
     * Instantiates a new scalar web service.
     *
     * @param serviceName  the non-null, non-empty service name to use
     * @param version      the non-null, non-empty service version
     * @param supportedApi the non-null supported api
     * @param transport    the non-null transport to use
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public ScalarWebService(SonyTransportFactory transportFactory, ServiceProtocol serviceProtocol, String version,
            SupportedApi supportedApi) {
        Validate.notEmpty(version, "version cannot be empty");
        Objects.requireNonNull(supportedApi, "supportedApi cannot be null");

        this.transportFactory = transportFactory;
        this.serviceName = serviceProtocol.getServiceName();
        this.version = version;
        this.supportedApi = supportedApi;

        final SonyTransport<ScalarWebRequest> transport = transportFactory.getSonyTransport(serviceProtocol);
        if (transport == null) {
            throw new IllegalArgumentException("No transport found for " + serviceProtocol);
        }
        this.transport = transport;
    }

    public List<ScalarWebMethod> getMethods() {
        final List<ScalarWebMethod> methods = new ArrayList<>();
        try {
            // Retrieve the versions for the service
            final List<String> versionResult = execute(
                    new ScalarWebRequest(++currId, ScalarWebMethod.GETVERSIONS, version)).asArray(String.class);

            // For each version, retrieve the methods for the service
            for (String apiVersion : versionResult) {
                final MethodTypes mtdResults = execute(
                        new ScalarWebRequest(++currId, ScalarWebMethod.GETMETHODTYPES, version, apiVersion))
                                .as(MethodTypes.class);
                methods.addAll(mtdResults.getMethods());
            }
        } catch (IOException e) {
            if (StringUtils.contains(e.getMessage(), "404")) {
                logger.debug("Could not retrieve methods - missing method (or service unavilable): {}", e.getMessage());
            } else {
                logger.debug("Could not retrieve methods: {}", e.getMessage(), e);
            }
        }

        // Merge in any methods reported that weren't returned by getmethodtypes/version
        supportedApi.getApis().forEach(api -> {
            api.getVersions().forEach(v -> {
                if (!methods.stream().anyMatch(m -> StringUtils.equalsIgnoreCase(m.getMethodName(), api.getName())
                        && StringUtils.equalsIgnoreCase(v.getVersion(), m.getVersion()))) {
                    methods.add(
                            new ScalarWebMethod(api.getName(), new ArrayList<>(), new ArrayList<>(), v.getVersion()));
                }

            });
        });
        return methods;
    }

    public List<ScalarWebMethod> getNotifications() {
        final List<ScalarWebMethod> notifications = new ArrayList<>();
        // add in any supported api that has no match above (shouldn't really be any but we are being complete)
        supportedApi.getNotifications().forEach(api -> {
            api.getVersions().forEach(v -> {
                notifications
                        .add(new ScalarWebMethod(api.getName(), new ArrayList<>(), new ArrayList<>(), v.getVersion()));
            });
        });
        return notifications;
    }

    /**
     * Gets the latest version for method name
     *
     * @param methodName the non-null, non-empty method name
     * @return the latest version or null if not found
     */
    public @Nullable String getVersion(String methodName) {
        Validate.notEmpty(methodName, "methodName cannto be empty");
        final SupportedApiInfo api = supportedApi.getMethod(methodName);
        final SupportedApiVersionInfo vers = api == null ? null : api.getLatestVersion();
        return vers == null ? null : vers.getVersion();
    }

    /**
     * Gets all the versions for a given method
     *
     * @param methodName the non-null, non-empty method name
     * @return the non-null, possibly empty list of versions
     */
    public List<String> getVersions(String methodName) {
        Validate.notEmpty(methodName, "methodName cannto be empty");
        final SupportedApiInfo api = supportedApi.getMethod(methodName);
        return api == null ? new ArrayList<>()
                : api.getVersions().stream().map(v -> v.getVersion()).collect(Collectors.toList());
    }

    /**
     * Determines if the method name exists in the service
     *
     * @param methodName the non-null, non-empty method name
     * @return true if it exists, false otherwise
     */
    public boolean hasMethod(String methodName) {
        Validate.notEmpty(methodName, "methodName cannto be empty");
        return supportedApi.getMethod(methodName) != null;
    }

    /**
     * Gets the service name
     *
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Gets the service version
     *
     * @return the service version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the transport related to this service
     *
     * @return the non-null sony transport
     */
    public SonyTransport<ScalarWebRequest> getTransport() {
        return transport;
    }

    /**
     * Executes the latest method version using the specified parameters
     *
     * @param methodName the method name
     * @param parms      the parameters to use
     * @return the scalar web result
     */
    public ScalarWebResult execute(String methodName, Object... parms) {
        Validate.notEmpty(methodName, "methodName cannot be empty");
        return executeSpecific(methodName, null, parms);
    }

    /**
     * Executes a specific method/version using the specified parameters
     *
     * @param methodName the method name
     * @param version    the possibly null, possibly empty version (null/empty to use latest version)
     * @param parms      the parameters to use
     * @return the scalar web result
     */
    public ScalarWebResult executeSpecific(String methodName, @Nullable String version, Object... parms) {
        Validate.notEmpty(methodName, "methodName cannot be empty");

        if (version == null || StringUtils.isEmpty(version)) {
            final String mtdVersion = getVersion(methodName);
            if (mtdVersion == null) {
                logger.debug("Method {} doesn't exist in the service {}", methodName, serviceName);
                return ScalarWebResult.createNotImplemented(methodName);
            }
            return execute(new ScalarWebRequest(currId++, methodName, mtdVersion, parms));
        } else {
            return execute(new ScalarWebRequest(currId++, methodName, version, parms));
        }
    }

    /**
     * Execute the specified json request with the specified HTTP headers
     *
     * @param jsonRequest the json request
     * @param headers     the headers
     * @return the scalar web result
     */
    public ScalarWebResult execute(ScalarWebRequest request) {
        final Set<String> protocols = supportedApi.getProtocols(request.getMethod(), request.getVersion());
        try {
            if (protocols.contains(transport.getProtocolType())) {
                return transport.execute(request).get(ScalarWebConstants.RSP_WAIT_TIMEOUTSECONDS, TimeUnit.SECONDS);
            } else {
                final ServiceProtocol serviceProtocol = new ServiceProtocol(serviceName, protocols);
                try (final SonyTransport<ScalarWebRequest> mthdTransport = transportFactory
                        .getSonyTransport(serviceProtocol)) {
                    if (mthdTransport == null) {
                        logger.debug("No transport for {} with protocols: {}", request, protocols);
                        return ScalarUtilities.createErrorResult(HttpStatus.INTERNAL_SERVER_ERROR_500,
                                "No transport for " + request + " with protocols: " + protocols);
                    } else {
                        logger.debug("Execution of {} is using a different protocol {} than the service {}", request,
                                mthdTransport.getProtocolType(), transport.getProtocolType());
                        return mthdTransport.execute(request).get(ScalarWebConstants.RSP_WAIT_TIMEOUTSECONDS,
                                TimeUnit.SECONDS);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.debug("Execution of {} resulted in an exception: {}", request, e.getMessage(), e);
            return ScalarUtilities.createErrorResult(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
        }
    }

    /**
     * Helper method to execute an ActRegister to register the system
     *
     * @param accessCode the access code to use (or null to initiate the first step of ActRegister)
     * @return the scalar web result
     */
    public ScalarWebResult actRegister(@Nullable Integer accessCode) {
        final String actVersion = getVersion(ScalarWebMethod.ACTREGISTER);
        if (actVersion == null) {
            return ScalarWebResult.createNotImplemented(ScalarWebMethod.ACTREGISTER);
        }

        if (accessCode == null) {
            return execute(ScalarWebMethod.ACTREGISTER, new ActRegisterId(), new Object[] { new ActRegisterOptions() });
        } else {
            final Header authHeader = NetUtil.createAuthHeader(accessCode);
            try {
                getTransport().addOption(SonyTransport.OPTION_HEADER, authHeader);
                return execute(new ScalarWebRequest(currId++, ScalarWebMethod.ACTREGISTER, actVersion,
                        new ActRegisterId(), new Object[] { new ActRegisterOptions() }));

            } finally {
                getTransport().removeOption(SonyTransport.OPTION_HEADER, authHeader);
            }
        }
    }

    public static String labelFor(String serviceName) {
        switch (serviceName) {
            case ACCESSCONTROL:
                return "Access Control";
            case APPCONTROL:
                return "Application Control";
            case AUDIO:
                return "Audio";
            case AVCONTENT:
                return "A/V Content";
            case BROWSER:
                return "Browser";
            case CEC:
                return "CEC";
            case CONTENTSHARE:
                return "Content Share";
            case ENCRYPTION:
                return "Encryption";
            case GUIDE:
                return "Guide";
            case SYSTEM:
                return "System";
            case VIDEOSCREEN:
                return "Video Screen";
            default:
                return serviceName;
        }
    }

    public static Map<String, String> getServiceMap() {
        return Collections.unmodifiableMap(new HashMap<String, String>() {
            private static final long serialVersionUID = 5934100497468165317L;
            {
                put(ACCESSCONTROL, labelFor(ACCESSCONTROL));
                put(APPCONTROL, labelFor(APPCONTROL));
                put(AUDIO, labelFor(AUDIO));
                put(AVCONTENT, labelFor(AVCONTENT));
                put(BROWSER, labelFor(BROWSER));
                put(CEC, labelFor(CEC));
                put(CONTENTSHARE, labelFor(CONTENTSHARE));
                put(ENCRYPTION, labelFor(ENCRYPTION));
                put(GUIDE, labelFor(GUIDE));
                put(SYSTEM, labelFor(SYSTEM));
                put(VIDEOSCREEN, labelFor(VIDEOSCREEN));
            }
        });
    }

    @Override
    public void close() {
        transport.close();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(100);
        final String newLine = java.lang.System.lineSeparator();

        sb.append("Service: ");
        sb.append(serviceName);
        sb.append(newLine);

        for (ScalarWebMethod mthd : getMethods().stream().sorted(Comparator.comparing(ScalarWebMethod::getMethodName))
                .collect(Collectors.toList())) {
            sb.append("   ");
            sb.append(mthd);
            sb.append(newLine);
        }

        return sb.toString();
    }
}
