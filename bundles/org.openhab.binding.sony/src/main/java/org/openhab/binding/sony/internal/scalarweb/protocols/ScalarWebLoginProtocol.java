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
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.transform.TransformationService;
import org.openhab.binding.sony.internal.AccessCheckResult;
import org.openhab.binding.sony.internal.SonyAuth;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.ircc.models.IrccClient;
import org.openhab.binding.sony.internal.ircc.models.IrccRemoteCommand;
import org.openhab.binding.sony.internal.ircc.models.IrccRemoteCommands;
import org.openhab.binding.sony.internal.net.HttpResponse;
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
import org.openhab.binding.sony.internal.simpleip.SimpleIpConfig;
import org.openhab.binding.sony.internal.transports.TransportOptionAutoAuth;
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

    private final SonyAuth sonyAuth;

    /**
     * Constructs the protocol handler from given parameters.
     *
     * @param client           a non-null {@link ScalarWebClient}
     * @param config           a non-null {@link SimpleIpConfig} (may be connected or disconnected)
     * @param callback         a non-null {@link RioHandlerCallback} to callback
     * @param transformService a potentially null transformation service
     * @throws IOException
     */
    public ScalarWebLoginProtocol(ScalarWebClient client, ScalarWebConfig config, T callback,
            @Nullable TransformationService transformService) throws IOException {
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");

        this.scalarClient = client;
        this.config = config;
        this.callback = callback;
        this.transformService = transformService;

        final ScalarWebService accessControlService = scalarClient.getService(ScalarWebService.ACCESSCONTROL);

        sonyAuth = new SonyAuth(() -> {
            final String irccUrl = config.getIrccUrl();
            try {
                SonyUtil.sendWakeOnLan(logger, config.getDeviceIpAddress(), config.getDeviceMacAddress());
                return irccUrl == null || StringUtils.isEmpty(irccUrl) ? null : new IrccClient(irccUrl);
            } catch (IOException e) {
                logger.debug("Cannot create IRCC Client: {}", e.getMessage());
                return null;
            }
        }, accessControlService);
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
     * @return the access check result
     * @throws IOException                  Signals that an I/O exception has occurred.
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException                 the SAX exception
     */
    public AccessCheckResult login() throws IOException, ParserConfigurationException, SAXException {
        final ScalarWebService accessControl = scalarClient.getService(ScalarWebService.ACCESSCONTROL);
        if (accessControl == null) {
            return AccessCheckResult.SERVICEMISSING;
        }

        // turn off auto authorization for all services
        for (ScalarWebService srv : scalarClient.getDevice().getServices()) {
            srv.getTransport().setOption(TransportOptionAutoAuth.FALSE);
        }

        SonyUtil.sendWakeOnLan(logger, config.getDeviceIpAddress(), config.getDeviceMacAddress());

        final AccessCheckResult result = checkAccess();
        if (result != AccessCheckResult.OK) {
            if (result == AccessCheckResult.DISPLAYOFF) {
                return result;
            }

            if (result == AccessCheckResult.NEEDSPAIRING) {
                final String accessCode = config.getAccessCode();
                if (StringUtils.isEmpty(accessCode)) {
                    return new AccessCheckResult(AccessCheckResult.OTHER, "Access code cannot be blank");
                } else {
                    final AccessCheckResult res = sonyAuth.requestAccess(accessControl.getTransport(),
                            StringUtils.equalsIgnoreCase(ScalarWebConstants.ACCESSCODE_RQST, accessCode) ? null
                                    : accessCode);
                    if (res == AccessCheckResult.OK) {
                        // GOOD!
                    } else {
                        return res;
                    }
                }
            } else {
                final AccessCheckResult resp = sonyAuth.registerRenewal(accessControl.getTransport());
                if (resp != AccessCheckResult.OK) {
                    // Use configuration_error - prevents handler from continually trying to
                    // reconnect
                    return resp;
                }
            }
        }

        postLogin();
        return AccessCheckResult.OK;
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
            srv.getTransport().setOption(TransportOptionAutoAuth.TRUE);
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
            return AccessCheckResult.SERVICEMISSING;
        }

        // Some devices have power status unprotected - so check device mode first
        // if device mode isn't implement, check power status (which will probably be protected
        // or not protected if webconnect)
        ScalarWebResult result = systemService.execute(ScalarWebMethod.GETDEVICEMODE);
        if (result.getDeviceErrorCode() == ScalarWebError.NOTIMPLEMENTED) {
            result = systemService.execute(ScalarWebMethod.GETPOWERSTATUS);
        }

        if (result.getDeviceErrorCode() == ScalarWebError.DISPLAYISOFF) {
            return AccessCheckResult.DISPLAYOFF;
        }

        final HttpResponse httpResponse = result.getHttpResponse();

        // Either a 200 (good call) or an illegalargument (it tried to run it but it's arguments were not good) is good
        if (httpResponse.getHttpCode() == HttpStatus.OK_200
                || result.getDeviceErrorCode() == ScalarWebError.ILLEGALARGUMENT) {
            return AccessCheckResult.OK;
        }

        if (result.getDeviceErrorCode() == ScalarWebError.NOTIMPLEMENTED
                || httpResponse.getHttpCode() == HttpStatus.UNAUTHORIZED_401
                || httpResponse.getHttpCode() == HttpStatus.FORBIDDEN_403
                || result.getDeviceErrorCode() == ScalarWebError.FORBIDDEN) {
            return AccessCheckResult.NEEDSPAIRING;
        }

        return new AccessCheckResult(httpResponse);
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
                    try {
                        final IrccClient irccClient = new IrccClient(irccUrl);
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
}
