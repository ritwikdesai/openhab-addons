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
package org.openhab.binding.sony.internal.ircc.models;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.scalarweb.gson.GsonUtilities;
import org.openhab.binding.sony.internal.transports.SonyHttpTransport;
import org.openhab.binding.sony.internal.transports.SonyTransport;
import org.openhab.binding.sony.internal.transports.TransportOptionHeader;
import org.openhab.binding.sony.internal.upnp.models.UpnpScpd;
import org.openhab.binding.sony.internal.upnp.models.UpnpService;
import org.openhab.binding.sony.internal.upnp.models.UpnpXmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class represents an IRCC client. The client will gather all the necessary about an IRCC device and provide an
 * interface to query information and to manipulate the IRCC device.
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class IrccClient {

    /** The logger used by the client */
    private Logger logger = LoggerFactory.getLogger(IrccClient.class);

    /** The constant representing the REGISTER action name */
    public static final String AN_REGISTER = "register";

    /** The constant representing the GET TEXT action name */
    public static final String AN_GETTEXT = "getText";

    /** The constant representing the SEND TEXT action name */
    public static final String AN_SENDTEXT = "sendText";

    /** The constant representing the GET CONTENT INFORMATION action name */
    public static final String AN_GETCONTENTINFORMATION = "getContentInformation";

    /** The constant representing the GET SYSTEM INFORMATION action name */
    private static final String AN_GETSYSTEMINFORMATION = "getSystemInformation";

    /** The constant representing the GET REMOTE COMMANDS action name */
    private static final String AN_GETREMOTECOMMANDLIST = "getRemoteCommandList";

    /** The constant representing the GET STATUS action name */
    public static final String AN_GETSTATUS = "getStatus";

    /** The constant representing the GET CONTENT URL action name */
    public static final String AN_GETCONTENTURL = "getContentUrl";

    /** The constant representing the SEND CONTENT URL action name */
    public static final String AN_SENDCONTENTURL = "sendContentUrl";

    /** The constant representing the IRCC service name */
    public static final String SRV_IRCC = "urn:schemas-sony-com:serviceId:IRCC";

    /** The constant representing the IRCC service action to send an IRCC command */
    public static final String SRV_ACTION_SENDIRCC = "X_SendIRCC";

    /** The services mapped by service id */
    private final Map<String, UpnpService> services = new HashMap<String, UpnpService>();

    /** The action list for the IRCC device */
    private final IrccActionList actions;

    /** The system information for the IRCC device */
    private final IrccSystemInformation sysInfo;

    /** The remote commands supported by the IRCC device */
    private final IrccRemoteCommands remoteCommands;

    /** The IRCC device definition */
    private final IrccDevice irccDevice;

    /** The scpd mapped by service id */
    private final Map<String, UpnpScpd> scpdByService = new HashMap<String, UpnpScpd>();

    /** The base URL for the IRCC device */
    private final URL baseUrl;

    /**
     * Instantiates a new IRCC client give the IRCC URL
     *
     * @param irccUrl the non-null, non-empty IRCC URL
     * @throws IOException if an IO exception occurs getting information from the client
     */
    public IrccClient(String irccUrl) throws IOException, URISyntaxException {
        Validate.notEmpty(irccUrl, "irccUrl cannot be empty");

        try (SonyTransport transport = new SonyHttpTransport(irccUrl, GsonUtilities.getDefaultGson())) {
            
            final HttpResponse resp = transport.executeGet(irccUrl);
            if (resp.getHttpCode() != HttpStatus.OK_200) {
                throw resp.createException();
            }
    
            this.baseUrl = new URL(irccUrl);
    
            final String irccResponse = resp.getContent();
            final IrccRoot irccRoot = IrccXmlReader.ROOT.fromXML(irccResponse);
            if (irccRoot == null) {
                throw new IOException("IRCC response (" + irccUrl + ") was not valid: " + irccResponse);
            }
    
            final IrccDevice irccDevice = irccRoot.getDevice();
            if (irccDevice == null) {
                throw new IOException("IRCC response (" + irccUrl + ") didn't contain an IRCC device");
            }
            this.irccDevice = irccDevice;
    
            for (UpnpService service : irccDevice.getServices()) {
                final String serviceId = service.getServiceId();
    
                if (serviceId == null || StringUtils.isEmpty(serviceId)) {
                    logger.info("Found a service with no service id - ignoring: {}", service);
                    continue;
                }
    
                services.put(serviceId, service);
    
                final URL scpdUrl = service.getScpdUrl(baseUrl);
                if (scpdUrl != null) {
                    final HttpResponse spcdResponse = transport.executeGet(scpdUrl.toExternalForm());
                    if (spcdResponse.getHttpCode() != HttpStatus.OK_200) {
                        throw spcdResponse.createException();
                    }
    
                    final String scpdResponse = spcdResponse.getContent();
                    final UpnpScpd scpd = UpnpXmlReader.SCPD.fromXML(scpdResponse);
                    if (scpd == null) {
                        logger.debug("spcd url '{}' didn't contain a valid response (and is being ignored): {}", scpdUrl,
                                spcdResponse);
                    } else {
                        scpdByService.put(serviceId, scpd);
                    }
                }
            }
    
            final IrccUnrDeviceInfo deviceInfo = irccDevice.getUnrDeviceInfo();
            final String actionsUrl = deviceInfo == null ? null : deviceInfo.getActionListUrl();
    
            IrccActionList actionsList;
            IrccSystemInformation sysInfo;
    
            // If empty - likely version 1.0 or 1.1
            if (actionsUrl == null || StringUtils.isEmpty(actionsUrl)) {
                logger.debug("No actionsurl - generating default");
                actionsList = new IrccActionList();
                sysInfo = new IrccSystemInformation();
            } else {
                final HttpResponse actionsResp = transport.executeGet(actionsUrl);
                if (actionsResp.getHttpCode() == HttpStatus.OK_200) {
                    final String actionXml = actionsResp.getContent();
                    final IrccActionList actionList = IrccXmlReader.ACTIONS.fromXML(actionXml);
                    if (actionList == null) {
                        throw new IOException("IRCC Actions response (" + actionsUrl + ")  was not valid: " + actionXml);
                    }
                    actionsList = actionList;
                } else {
                    throw actionsResp.createException();
                }
    
                final String sysUrl = actionsList.getUrlForAction(AN_GETSYSTEMINFORMATION);
                if (sysUrl == null || StringUtils.isEmpty(sysUrl)) {
                    throw new NotImplementedException(AN_GETSYSTEMINFORMATION + " is not supported");
                }
    
                final HttpResponse sysResp = transport.executeGet(sysUrl);
                if (sysResp.getHttpCode() == HttpStatus.OK_200) {
                    final String sysXml = sysResp.getContent();
                    final IrccSystemInformation sys = IrccXmlReader.SYSINFO.fromXML(sysXml);
                    if (sys == null) {
                        throw new IOException("IRCC systems info response (" + sysUrl + ")  was not valid: " + sysXml);
                    }
                    sysInfo = sys;
                } else {
                    throw sysResp.createException();
                }
            }
    
            this.actions = actionsList;
            this.sysInfo = sysInfo;
    
            IrccRemoteCommands remoteCommands;
    
            final IrccCodeList codeList = irccDevice.getCodeList();
            final String remoteCommandsUrl = getUrlForAction(IrccClient.AN_GETREMOTECOMMANDLIST);
            if (remoteCommandsUrl == null || StringUtils.isEmpty(remoteCommandsUrl)) {
                remoteCommands = new IrccRemoteCommands().withCodeList(codeList);
            } else {
                final HttpResponse rcResp = transport.executeGet(remoteCommandsUrl);
                if (rcResp.getHttpCode() == HttpStatus.OK_200) {
                    final String rcXml = rcResp.getContent();
                    final IrccRemoteCommands rcCmds = IrccXmlReader.REMOTECOMMANDS.fromXML(rcXml);
                    if (rcCmds == null) {
                        throw new IOException(
                                "IRCC systems info response (" + remoteCommandsUrl + ")  was not valid: " + rcXml);
                    }
                    remoteCommands = rcCmds;
                } else {
                    remoteCommands = new IrccRemoteCommands().withCodeList(codeList);
                }
            }
    
            this.remoteCommands = remoteCommands;
                
        }
    }

    /**
     * Returns the base URL of the IRCC device
     *
     * @return the non-null, non-empty base URL
     */
    public URL getBaseUrl() {
        return baseUrl;
    }

    /**
     * Gets the URL for the given action name or null if not found
     *
     * @param actionName the non-null, non-empty action name
     * @return the url for action or null if not found
     */
    public @Nullable String getUrlForAction(String actionName) {
        Validate.notEmpty(actionName, "actionName cannot be empty");
        return actions.getUrlForAction(actionName);
    }

    /**
     * Gets the registration mode of the IRCC device
     *
     * @return the registration mode (most likely >= 0)
     */
    public int getRegistrationMode() {
        return actions.getRegistrationMode();
    }

    /**
     * Gets the service related to the service ID
     *
     * @param serviceId the non-null, non-empty service id
     * @return the service or null if not found
     */
    public @Nullable UpnpService getService(String serviceId) {
        Validate.notEmpty(serviceId, "serviceId cannot be empty");
        return services.get(serviceId);
    }

    /**
     * Gets the system information for the IRCC device
     *
     * @return the non-null system information
     */
    public IrccSystemInformation getSystemInformation() {
        return sysInfo;
    }

    /**
     * Gets the UNR Device information for the IRCC device
     *
     * @return the non-null unr device information
     */
    public IrccUnrDeviceInfo getUnrDeviceInformation() {
        final IrccUnrDeviceInfo iudi = irccDevice.getUnrDeviceInfo();
        return iudi == null ? new IrccUnrDeviceInfo() : iudi;
    }

    /**
     * Gets the remote commands supported by the IRCC device
     *
     * @return the non-null remote commands
     */
    public IrccRemoteCommands getRemoteCommands() {
        return remoteCommands;
    }

    /**
     * Get's the SOAP command for the given service ID, action name and possibly parameters
     *
     * @param serviceId  the non-null, non-empty service id
     * @param actionName the non-null, non-empty action name
     * @param parms      the possibly null, possibly empty list of action parameters
     * @return the possibly null (if not service/action is not found) SOAP command
     */
    public @Nullable String getSOAP(String serviceId, String actionName, String... parms) {
        Validate.notEmpty(serviceId, "serviceId cannot be empty");
        Validate.notEmpty(actionName, "actionName cannot be empty");

        final UpnpService service = services.get(serviceId);

        if (service == null) {
            logger.info("Unable to getSOAP for service id {} - service not found", serviceId);
            return null;
        }
        final UpnpScpd scpd = scpdByService.get(serviceId);
        if (scpd == null) {
            logger.info("Unable to getSOAP for service id {} - scpd not found", serviceId);
            return null;
        }
        final String serviceType = service.getServiceType();
        if (serviceType == null || StringUtils.isEmpty(serviceType)) {
            logger.info("Unable to getSOAP for service id {} - serviceType was empty", serviceId);
            return null;
        }
        return scpd.getSoap(serviceType, actionName, parms);
    }

    /**
     * Executes a SOAP command 
     * @param transport a non-null transport to use
     * @param cmd a non-null, non-empty cmd to execute
     * @return an HttpResponse indicating the results of the execution
     */
    public HttpResponse executeSoap(SonyTransport transport, String cmd) {
        Objects.requireNonNull(transport, "transport cannot be null");
        Validate.notEmpty(cmd, "cmd cannot be empty");

        final UpnpService service = getService(IrccClient.SRV_IRCC);
        if (service == null) {
            logger.debug("IRCC Service was not found");
            return new HttpResponse(HttpStatus.NOT_FOUND_404, "IRCC Service was not found");
        }

        final String soap = getSOAP(IrccClient.SRV_IRCC, IrccClient.SRV_ACTION_SENDIRCC, cmd);
        if (soap == null || StringUtils.isEmpty(soap)) {
            logger.debug("Unable to find the IRCC service/action to send IRCC");
            return new HttpResponse(HttpStatus.NOT_FOUND_404, "Unable to find the IRCC service/action to send IRCC");
        }

        final URL baseUrl = getBaseUrl();
        final URL controlUrl = service.getControlUrl(baseUrl);
        if (controlUrl == null) {
            logger.debug("ControlURL for IRCC service wasn't found: {}", baseUrl);
            return new HttpResponse(HttpStatus.NOT_FOUND_404, "ControlURL for IRCC service wasn't found: " + baseUrl);
        } else {
            return transport.executePostXml(controlUrl.toExternalForm(), soap, new TransportOptionHeader("SOAPACTION",
            "\"" + service.getServiceType() + "#" + IrccClient.SRV_ACTION_SENDIRCC + "\""));
        }
    }
}
