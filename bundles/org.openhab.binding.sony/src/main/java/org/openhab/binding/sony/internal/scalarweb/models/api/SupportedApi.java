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
package org.openhab.binding.sony.internal.scalarweb.models.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebConstants;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebRequest;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.openhab.binding.sony.internal.scalarweb.transports.SonyTransport;
import org.slf4j.Logger;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SupportedApi {
    private final Map<String, SupportedApiInfo> apis;
    private final Map<String, SupportedApiInfo> notifications;
    private final Set<String> protocols;
    private final String service;

    public SupportedApi(String service, List<SupportedApiInfo> apis, List<SupportedApiInfo> notifications,
            Set<String> protocols) {
        this.apis = Collections.unmodifiableMap(apis.stream().collect(Collectors.toMap(k -> k.name, v -> v)));
        this.notifications = Collections
                .unmodifiableMap(notifications.stream().collect(Collectors.toMap(k -> k.name, v -> v)));
        this.protocols = Collections.unmodifiableSet(protocols);
        this.service = service;
    }

    public Set<String> getProtocols() {
        return protocols;
    }

    public String getService() {
        return service;
    }

    public Collection<SupportedApiInfo> getApis() {
        return apis.values();
    }

    public Collection<SupportedApiInfo> getNotifications() {
        return notifications.values();
    }

    public @Nullable SupportedApiInfo getMethod(String methodName) {
        return apis.get(methodName);
    }

    public Set<String> getProtocols(String methodName, String version) {
        final SupportedApiInfo info = apis.get(methodName);
        final SupportedApiVersionInfo vers = info == null ? null : info.getVersions(version);
        final Set<String> mthdProtocols = vers == null ? null : vers.getProtocols();
        return mthdProtocols != null && mthdProtocols.size() > 0 ? mthdProtocols : protocols;
    }

    public static SupportedApi getSupportedApi(ScalarWebService guide, String serviceName,
            SonyTransport<ScalarWebRequest> transport, Logger logger) {
        final SupportedApi api = getSupportApi(guide, serviceName, logger);
        return api == null ? getSupportApiAlternate(serviceName, transport, logger) : api;
    }

    public static @Nullable SupportedApi getSupportApi(ScalarWebService guide, String serviceName, Logger logger) {
        Objects.requireNonNull(guide, "guide cannot be null");
        try {
            return guide.execute(ScalarWebMethod.GETSUPPORTEDAPIINFO, new SupportedApiServices(serviceName))
                    .as(SupportedApi.class);
        } catch (IOException e) {
            logger.trace("Exception getting supported api info: {}", e.getMessage(), e);
            return null;
        }
    }

    public static SupportedApi getSupportApiAlternate(String serviceName, SonyTransport<ScalarWebRequest> transport,
            Logger logger) {

        int currId = 0;

        final List<ScalarWebMethod> methods = new ArrayList<>();
        try {
            // Retrieve the versions for the service
            final List<String> versionResult = transport
                    .execute(new ScalarWebRequest(++currId, ScalarWebMethod.GETVERSIONS, ScalarWebMethod.V1_0))
                    .get(ScalarWebConstants.RSP_WAIT_TIMEOUTSECONDS, TimeUnit.SECONDS).asArray(String.class);

            // For each version, retrieve the methods for the service
            for (String apiVersion : versionResult) {
                final MethodTypes mtdResults = transport
                        .execute(new ScalarWebRequest(++currId, ScalarWebMethod.GETMETHODTYPES, ScalarWebMethod.V1_0,
                                apiVersion))
                        .get(ScalarWebConstants.RSP_WAIT_TIMEOUTSECONDS, TimeUnit.SECONDS).as(MethodTypes.class);
                methods.addAll(mtdResults.getMethods());
            }
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            logger.debug("Could not retrieve methods: {}", e.getMessage(), e);
        }

        final Map<String, Set<String>> mthdVersions = new HashMap<>();
        methods.stream().forEach(m -> {
            final String mthdName = m.getMethodName();
            Set<String> versions = mthdVersions.get(mthdName);
            if (versions == null) {
                versions = new HashSet<>();
                mthdVersions.put(mthdName, versions);
            }
            versions.add(m.getVersion());
        });

        final List<SupportedApiInfo> apis = mthdVersions.entrySet().stream()
                .map(e -> new SupportedApiInfo(e.getKey(), e.getValue().stream()
                        .map(v -> new SupportedApiVersionInfo("", new HashSet<>(), v)).collect(Collectors.toList())))
                .collect(Collectors.toList());

        final Set<String> protocols = new HashSet<>();
        protocols.add(transport.getProtocolType());

        List<SupportedApiInfo> notifications;
        try {
            final Notifications ntfs = transport
                    .execute(new ScalarWebRequest(++currId, ScalarWebMethod.SWITCHNOTIFICATIONS, "1.0",
                            new Notifications()))
                    .get(ScalarWebConstants.RSP_WAIT_TIMEOUTSECONDS, TimeUnit.SECONDS).as(Notifications.class);
            notifications = Stream.concat(ntfs.getEnabled().stream(), ntfs.getDisabled().stream())
                    .map(n -> new SupportedApiInfo(n.getName(),
                            Arrays.asList(new SupportedApiVersionInfo("", new HashSet<>(), n.getVersion()))))
                    .collect(Collectors.toList());
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            logger.debug("Exception getting notifications: {}", e.getMessage());
            notifications = new ArrayList<>();
        }

        return new SupportedApi(serviceName, apis, notifications, protocols);
    }

    @Override
    public String toString() {
        return "SupportedApi [apis=" + apis + ", notifications=" + notifications + ", protocols=" + protocols
                + ", service=" + service + "]";
    }
}
