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
package org.openhab.binding.sony.internal.scalarweb.protocols;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.transform.TransformationService;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.ircc.models.IrccClient;
import org.openhab.binding.sony.internal.ircc.models.IrccRemoteCommand;
import org.openhab.binding.sony.internal.ircc.models.IrccRemoteCommands;
import org.openhab.binding.sony.internal.net.Header;
import org.openhab.binding.sony.internal.net.HttpRequest;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.net.NetUtil;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebClient;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebConfig;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebConstants;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebError;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.openhab.binding.sony.internal.scalarweb.models.api.InterfaceInformation;
import org.openhab.binding.sony.internal.scalarweb.models.api.NetIf;
import org.openhab.binding.sony.internal.scalarweb.models.api.NetworkSetting;
import org.openhab.binding.sony.internal.scalarweb.models.api.RemoteControllerInfo;
import org.openhab.binding.sony.internal.scalarweb.models.api.RemoteControllerInfo.RemoteCommand;
import org.openhab.binding.sony.internal.scalarweb.models.api.SystemInformation;
import org.openhab.binding.sony.internal.scalarweb.transports.ScalarAuthFilter;
import org.openhab.binding.sony.internal.scalarweb.transports.SonyTransport;
import org.openhab.binding.sony.internal.simpleip.SimpleIpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * This is the login protocol handler for scalar web systems. The login handler will handle both registration and login.
 * Additionally, the handler will also perform initial connection logic (like writing scalar/IRCC commands to the file).
 *
 * @author Tim Roberts - Initial contribution
 * @param <T> the generic type callback
 */
@NonNullByDefault
public class ScalarWebLoginProtocol<T extends ThingCallback<String>> {

    public static final String NOTAVAILABLE = "NA";
    /** The logger */
    private Logger logger = LoggerFactory.getLogger(ScalarWebLoginProtocol.class);

    /** The configuration */
    private final ScalarWebConfig config;

    /** The callback to set state and status. */
    private final T callback;

    /** The scalar state */
    private final ScalarWebClient scalarClient;

    /** The transformation service */
    private final @Nullable TransformationService transformService;

    /**
     * Constructs the protocol handler from given parameters.
     *
     * @param client           a non-null {@link ScalarWebClient}
     * @param config           a non-null {@link SimpleIpConfig} (may be connected or disconnected)
     * @param callback         a non-null {@link RioHandlerCallback} to callback
     * @param transformService a potentially null transformation service
     */
    public ScalarWebLoginProtocol(ScalarWebClient client, ScalarWebConfig config, T callback,
            @Nullable TransformationService transformService) {
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");

        this.scalarClient = client;
        this.config = config;
        this.callback = callback;
        this.transformService = transformService;
    }

    /**
     * Gets the callback.
     *
     * @return the callback
     */
    T getCallback() {
        return callback;
    }

    /**
     * Attempts to log into the system.
     *
     * @return null to indicate a successful login, the error message otherwise
     * @throws IOException                  Signals that an I/O exception has occurred.
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException                 the SAX exception
     */
    public @Nullable String login() throws IOException, ParserConfigurationException, SAXException {
        final String ipAddress = config.getIpAddress();
        final String macAddress = config.getDeviceMacAddress();
        if (ipAddress != null && StringUtils.isNotBlank(ipAddress) && macAddress != null
                && StringUtils.isNotBlank(macAddress)) {
            NetUtil.sendWol(ipAddress, macAddress);
        }

        final ScalarWebService systemWebService = scalarClient.getService(ScalarWebService.SYSTEM);
        if (systemWebService == null) {
            return "Device doesn't implement the system web service and is required";
        }

        final AccessCheckResult result = checkAccess();

        if (StringUtils.equalsIgnoreCase(result.getCode(), AccessCheckResult.OK)) {
            postLogin();
            return null;
        }

        if (StringUtils.equalsIgnoreCase(result.getCode(), AccessCheckResult.DISPLAYOFF)) {
            return result.getMsg();
        }

        if (StringUtils.equalsIgnoreCase(result.getCode(), AccessCheckResult.NEEDPAIRING)) {
            String accessCode = config.getAccessCode();
            if (StringUtils.equalsIgnoreCase(ScalarWebConstants.ACCESSCODE_RQST, accessCode)) {
                final HttpResponse accessCodeResponse = requestAccess(null);
                if (accessCodeResponse.getHttpCode() == HttpStatus.OK_200) {
                    // already registered!
                    accessCode = null;
                } else if (accessCodeResponse.getHttpCode() == HttpStatus.UNAUTHORIZED_401) {
                    return ScalarWebConstants.ACCESSCODE_PENDING;
                } else if (accessCodeResponse.getHttpCode() == HttpStatus.SERVICE_UNAVAILABLE_503) {
                    return "Unable to request an access code - HOME menu not displayed on device. Please display the home menu and try again.";
                } else if (accessCodeResponse.getHttpCode() == HttpStatus.SERVICE_UNAVAILABLE_503) {
                    return "Unable to request an access code - HOME menu not displayed on device. Please display the home menu and try again.";
                } else if (accessCodeResponse.getHttpCode() == ScalarWebError.DISPLAYISOFF) {
                    return "Unable to request an access code - Display is turned off (must be on to see code).";
                } else {
                    return "Access code request error: " + accessCodeResponse + ")";
                }

            }

            if (accessCode != null && accessCode.trim().length() != 0) {
                try {
                    final int accessCodeNbr = Integer.parseInt(accessCode);
                    if (accessCodeNbr > 9999) {
                        return "Access code cannot be greater than 4 digits";
                    }
                    final HttpResponse registerResponse = requestAccess(accessCodeNbr);
                    if (registerResponse.getHttpCode() == HttpStatus.OK_200) {
                        // GOOD!
                    } else if (registerResponse.getHttpCode() == HttpStatus.UNAUTHORIZED_401) {
                        return ScalarWebConstants.ACCESSCODE_NOTACCEPTED;
                    } else {
                        return "Access code was not accepted: " + registerResponse.getHttpCode() + " ("
                                + registerResponse.getContent() + ")";
                    }

                } catch (NumberFormatException e) {
                    return "Access code is not " + ScalarWebConstants.ACCESSCODE_RQST + " or a number!";
                }
            }

            postLogin();
            return null;
        }

        return result.getMsg();
    }

    /**
     * Post successful login stuff - set the properties and write out the commands.
     *
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException                 the SAX exception
     * @throws IOException                  Signals that an I/O exception has occurred.
     */
    private void postLogin() throws ParserConfigurationException, SAXException, IOException {
        logger.debug("WebScalar System now connected");

        // turn on auto authorization for all services
        for (ScalarWebService srv : scalarClient.getDevice().getServices()) {
            srv.getTransport().addOption(SonyTransport.OPTION_FILTER, ScalarAuthFilter.OPTION_AUTOAUTH);
        }

        final ScalarWebService sysService = scalarClient.getService(ScalarWebService.SYSTEM);
        Objects.requireNonNull(sysService, "sysService is null - shouldn't happen since it's checked in login()");

        if (sysService.hasMethod(ScalarWebMethod.GETSYSTEMINFORMATION)) {
            final ScalarWebResult sysResult = sysService.execute(ScalarWebMethod.GETSYSTEMINFORMATION);
            if (!sysResult.isError() && sysResult.hasResults()) {
                final SystemInformation sysInfo = sysResult.as(SystemInformation.class);
                callback.setProperty(ScalarWebConstants.PROP_PRODUCT,
                        SonyUtil.convertNull(sysInfo.getProduct(), NOTAVAILABLE));
                callback.setProperty(ScalarWebConstants.PROP_NAME,
                        SonyUtil.convertNull(sysInfo.getName(), NOTAVAILABLE));
                callback.setProperty(ScalarWebConstants.PROP_MODEL,
                        SonyUtil.convertNull(sysInfo.getModel(), NOTAVAILABLE));
                callback.setProperty(ScalarWebConstants.PROP_GENERATION,
                        SonyUtil.convertNull(sysInfo.getGeneration(), NOTAVAILABLE));
                callback.setProperty(ScalarWebConstants.PROP_SERIAL,
                        SonyUtil.convertNull(sysInfo.getSerial(), NOTAVAILABLE));
                callback.setProperty(ScalarWebConstants.PROP_MACADDR,
                        SonyUtil.convertNull(sysInfo.getMacAddr(), NOTAVAILABLE));
                callback.setProperty(ScalarWebConstants.PROP_AREA,
                        SonyUtil.convertNull(sysInfo.getArea(), NOTAVAILABLE));
                callback.setProperty(ScalarWebConstants.PROP_REGION,
                        SonyUtil.convertNull(sysInfo.getRegion(), NOTAVAILABLE));
            }
        }

        if (sysService.hasMethod(ScalarWebMethod.GETINTERFACEINFORMATION)) {
            final ScalarWebResult intResult = sysService.execute(ScalarWebMethod.GETINTERFACEINFORMATION);
            if (!intResult.isError() && intResult.hasResults()) {
                final InterfaceInformation intInfo = intResult.as(InterfaceInformation.class);
                callback.setProperty(ScalarWebConstants.PROP_INTERFACEVERSION,
                        SonyUtil.convertNull(intInfo.getInterfaceVersion(), NOTAVAILABLE));
                callback.setProperty(ScalarWebConstants.PROP_PRODUCTCATEGORY,
                        SonyUtil.convertNull(intInfo.getProductCategory(), NOTAVAILABLE));
                callback.setProperty(ScalarWebConstants.PROP_SERVERNAME,
                        SonyUtil.convertNull(intInfo.getServerName(), NOTAVAILABLE));
            }
        }

        if (sysService.hasMethod(ScalarWebMethod.GETNETWORKSETTINGS)) {
            for (String netIf : new String[] { "eth0", "wlan0", "eth1", "wlan1" }) {
                final ScalarWebResult swr = sysService.execute(ScalarWebMethod.GETNETWORKSETTINGS, new NetIf(netIf));
                if (!swr.isError() && swr.hasResults()) {
                    final NetworkSetting netSetting = swr.as(NetworkSetting.class);
                    callback.setProperty(ScalarWebConstants.PROP_NETIF,
                            SonyUtil.convertNull(netSetting.getNetif(), NOTAVAILABLE));
                    callback.setProperty(ScalarWebConstants.PROP_HWADDRESS,
                            SonyUtil.convertNull(netSetting.getHwAddr(), NOTAVAILABLE));
                    callback.setProperty(ScalarWebConstants.PROP_IPV4,
                            SonyUtil.convertNull(netSetting.getIpAddrV4(), NOTAVAILABLE));
                    callback.setProperty(ScalarWebConstants.PROP_IPV6,
                            SonyUtil.convertNull(netSetting.getIpAddrV6(), NOTAVAILABLE));
                    callback.setProperty(ScalarWebConstants.PROP_NETMASK,
                            SonyUtil.convertNull(netSetting.getNetmask(), NOTAVAILABLE));
                    callback.setProperty(ScalarWebConstants.PROP_GATEWAY,
                            SonyUtil.convertNull(netSetting.getGateway(), NOTAVAILABLE));
                    break;
                }
            }
        }

        writeCommands(sysService);
    }

    /**
     * Check device access.
     *
     * @return the non-null access check result
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private AccessCheckResult checkAccess() throws IOException {
        final ScalarWebService systemService = scalarClient.getService(ScalarWebService.SYSTEM);
        if (systemService == null) {
            return new AccessCheckResult(AccessCheckResult.SERVICEMISSING,
                    "Device doesn't implement the system service");
        }

        // Some devices have power status unprotected - so check device mode first
        // if device mode isn't implement, check power status (which will probably be protected
        // or not protected if webconnect)
        ScalarWebResult result = systemService.execute(ScalarWebMethod.GETDEVICEMODE);
        if (result.getDeviceErrorCode() == ScalarWebError.NOTIMPLEMENTED) {
            result = systemService.execute(ScalarWebMethod.GETPOWERSTATUS);
        }

        if (result.getDeviceErrorCode() == ScalarWebError.DISPLAYISOFF) {
            return new AccessCheckResult(AccessCheckResult.DISPLAYOFF,
                    "Display must be on to start pairing process - please turn on to start pairing process");
        }

        final HttpResponse httpResponse = result.getHttpResponse();

        // Either a 200 (good call) or an illegalargument (it tried to run it but it's arguments were not good) is good
        if (httpResponse.getHttpCode() == HttpStatus.OK_200
                || result.getDeviceErrorCode() == ScalarWebError.ILLEGALARGUMENT) {
            return new AccessCheckResult(AccessCheckResult.OK, "OK");
        }

        if (result.getDeviceErrorCode() == ScalarWebError.NOTIMPLEMENTED
                || httpResponse.getHttpCode() == HttpStatus.UNAUTHORIZED_401
                || httpResponse.getHttpCode() == HttpStatus.FORBIDDEN_403) {
            return new AccessCheckResult(AccessCheckResult.NEEDPAIRING, "Needs pairing");
        }

        final String content = httpResponse.getContent();

        return new AccessCheckResult(AccessCheckResult.OTHER, httpResponse.getHttpCode() + " - "
                + (StringUtils.isEmpty(content) ? httpResponse.getHttpReason() : content));
    }

    /**
     * Request access by initiating the registration or doing the activation if on the second step
     *
     * @param accessCode the access code (null for initial setup)
     * @return the http response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private HttpResponse requestAccess(@Nullable Integer accessCode) throws IOException {
        final ScalarWebService accessControlService = scalarClient.getService(ScalarWebService.ACCESSCONTROL);
        if (accessControlService == null) {
            return registerAccessCode(null);
            // return new HttpResponse(HttpStatus.SERVICE_UNAVAILABLE_503,
            // "Device doesn't implement the access control service");
        }

        return accessControlService.actRegister(accessCode).getHttpResponse();
    }

    /**
     * Register the access code
     *
     * @param accessCode the potentially null access code
     * @return the http response
     */
    private HttpResponse registerAccessCode(@Nullable Integer accessCode) {
        final String irccUrl = config.getIrccUrl();
        if (irccUrl != null && StringUtils.isNotEmpty(irccUrl)) {
            try (HttpRequest httpRequest = NetUtil.createHttpRequest()) {
                try (final IrccClient irccClient = new IrccClient(irccUrl)) {
                    final String registerUrl = irccClient.getUrlForAction(IrccClient.AN_REGISTER);
                    if (registerUrl == null) {
                        return new HttpResponse(HttpStatus.SERVICE_UNAVAILABLE_503, "Register action is not supported");
                    }

                    // Do the registration first with what the mode says,
                    // then try it again with the other mode (so registration mode sometimes lie)
                    final String[] registrationTypes = new String[2];
                    if (irccClient.getRegistrationMode() == 2) {
                        registrationTypes[0] = "new";
                        registrationTypes[1] = "initial";
                    } else {
                        registrationTypes[0] = "initial";
                        registrationTypes[1] = "new";
                    }

                    final Header[] headers = accessCode == null ? new Header[0]
                            : new Header[] { NetUtil.createAuthHeader(accessCode) };
                    try {
                        final String rqst = "?name=" + URLEncoder.encode(NetUtil.getDeviceName(), "UTF-8")
                                + "&registrationType=" + registrationTypes[0] + "&deviceId="
                                + URLEncoder.encode(NetUtil.getDeviceId(), "UTF-8");
                        final HttpResponse resp = httpRequest.sendGetCommand(registerUrl + rqst, headers);
                        if (resp.getHttpCode() != HttpStatus.BAD_REQUEST_400) {
                            return resp;
                        }
                    } catch (UnsupportedEncodingException e) {
                        // do nothing for now
                    }

                    try {
                        final String rqst = "?name=" + URLEncoder.encode(NetUtil.getDeviceName(), "UTF-8")
                                + "&registrationType=" + registrationTypes[1] + "&deviceId="
                                + URLEncoder.encode(NetUtil.getDeviceId(), "UTF-8");
                        return httpRequest.sendGetCommand(registerUrl + rqst, headers);
                    } catch (UnsupportedEncodingException e) {
                        return new HttpResponse(HttpStatus.SERVICE_UNAVAILABLE_503, e.toString());
                    }
                }
            } catch (IOException e) {
                return new HttpResponse(HttpStatus.SERVICE_UNAVAILABLE_503, "Register action is not supported");
            }
        } else {
            return new HttpResponse(HttpStatus.SERVICE_UNAVAILABLE_503, "No IRCC url was specified in configuration");
        }
    }

    /**
     * Write commands.
     *
     * @param service the non-null service
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException                 the SAX exception
     * @throws IOException                  Signals that an I/O exception has occurred.
     */
    private void writeCommands(ScalarWebService service)
            throws ParserConfigurationException, SAXException, IOException {
        Objects.requireNonNull(service, "service cannot be null");

        if (transformService == null) {
            logger.debug("No MAP transformation service - skipping writing a map file");
        } else {
            final String cmdMap = config.getCommandsMapFile();
            if (StringUtils.isEmpty(cmdMap)) {
                logger.debug("No command map defined - ignoring");
                return;
            }

            final String filePath = ConfigConstants.getConfigFolder() + File.separator
                    + TransformationService.TRANSFORM_FOLDER_NAME + File.separator + cmdMap;
            Path file = Paths.get(filePath);
            if (file.toFile().exists()) {
                logger.info("Command map already defined - ignoring: {}", file);
                return;
            }

            try {
                final Set<String> cmds = new HashSet<String>();
                final List<String> lines = new ArrayList<String>();
                if (service.hasMethod(ScalarWebMethod.GETREMOTECONTROLLERINFO)) {
                    final RemoteControllerInfo rci = service.execute(ScalarWebMethod.GETREMOTECONTROLLERINFO)
                            .as(RemoteControllerInfo.class);

                    for (RemoteCommand v : rci.getCommands()) {
                        // Note: encode value in case it's a URL type
                        final String name = v.getName();
                        final String value = v.getValue();
                        if (name != null && value != null && !cmds.contains(name)) {
                            cmds.add(name);
                            lines.add(name + "=" + URLEncoder.encode(value, "UTF-8"));
                        }
                    }
                } else {
                    logger.debug("No {} method found", ScalarWebMethod.GETREMOTECONTROLLERINFO);
                }

                // add any ircc extended commands
                final String irccUrl = config.getIrccUrl();
                if (irccUrl != null && StringUtils.isNotEmpty(irccUrl)) {
                    try (final IrccClient irccClient = new IrccClient(irccUrl)) {
                        final IrccRemoteCommands remoteCmds = irccClient.getRemoteCommands();
                        for (IrccRemoteCommand v : remoteCmds.getRemoteCommands().values()) {
                            // Note: encode value in case it's a URL type
                            final String name = v.getName();
                            if (!cmds.contains(name)) {
                                cmds.add(name);
                                lines.add(v.getName() + "=" + URLEncoder.encode(v.getCmd(), "UTF-8"));
                            }
                        }
                    } catch (IOException e) {
                        logger.debug("Exception creating IRCC client: {}", e.getMessage(), e);
                    }
                }
                Collections.sort(lines, String.CASE_INSENSITIVE_ORDER);

                if (lines.size() > 0) {
                    logger.info("Writing remote commands to {}", file);
                    Files.write(file, lines, Charset.forName("UTF-8"));
                }
            } catch (IOException e) {
                logger.info("Remote commands are undefined: {}", e.getMessage());
            }
        }
    }

    /**
     * This enum represents what type of action is needed when we first connect to the device
     */
    private class AccessCheckResult {
        /** OK - device either needs no pairing or we have already paird */
        public static final String OK = "ok";
        /** Device needs pairing */
        public static final String NEEDPAIRING = "pairing";
        /** Device needs pairing but the display is off */
        public static final String DISPLAYOFF = "displayoff";
        /** Service not found */
        public static final String SERVICEMISSING = "servicemissing";
        /** Some other error */
        public static final String OTHER = "other";

        private String code;
        private String msg;

        /**
         * Creates the result from the code/msg
         *
         * @param code the non-null, non-empty code
         * @param msg  the non-null, non-empty msg
         */
        public AccessCheckResult(String code, String msg) {
            Validate.notEmpty(code, "code cannot be empty");
            Validate.notEmpty(msg, "msg cannot be empty");
            this.code = code;
            this.msg = msg;
        }

        /**
         * Returns the related code
         *
         * @return a non-null, non-empty code
         */
        public String getCode() {
            return this.code;
        }

        /**
         * Returns the related message
         *
         * @return a non-null, non-empty message
         */
        public String getMsg() {
            return this.msg;
        }
    }
}
