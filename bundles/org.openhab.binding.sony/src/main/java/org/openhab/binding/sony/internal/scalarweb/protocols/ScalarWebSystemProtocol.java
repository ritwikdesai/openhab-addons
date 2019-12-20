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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.transform.TransformationException;
import org.eclipse.smarthome.core.transform.TransformationService;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.ircc.IrccClientFactory;
import org.openhab.binding.sony.internal.ircc.models.IrccClient;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.net.HttpResponse.SOAPError;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannel;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelDescriptor;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelTracker;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebContext;
import org.openhab.binding.sony.internal.scalarweb.VersionUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebEvent;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.openhab.binding.sony.internal.scalarweb.models.api.CurrentTime;
import org.openhab.binding.sony.internal.scalarweb.models.api.Language;
import org.openhab.binding.sony.internal.scalarweb.models.api.LedIndicatorStatus;
import org.openhab.binding.sony.internal.scalarweb.models.api.PostalCode;
import org.openhab.binding.sony.internal.scalarweb.models.api.PowerSavingMode;
import org.openhab.binding.sony.internal.scalarweb.models.api.PowerStatusRequest_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.PowerStatusRequest_1_1;
import org.openhab.binding.sony.internal.scalarweb.models.api.PowerStatusResult_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.PowerStatusResult_1_1;
import org.openhab.binding.sony.internal.scalarweb.models.api.SystemInformation;
import org.openhab.binding.sony.internal.scalarweb.models.api.WolMode;
import org.openhab.binding.sony.internal.transports.SonyTransport;
import org.openhab.binding.sony.internal.transports.SonyTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class ScalarWebSystemProtocol.
 *
 * @author Tim Roberts - Initial contribution
 * @param <T> the generic type for the callback
 */
@NonNullByDefault
class ScalarWebSystemProtocol<T extends ThingCallback<String>> extends AbstractScalarWebProtocol<T> {

    /** The logger */
    private Logger logger = LoggerFactory.getLogger(ScalarWebSystemProtocol.class);

    // Constants used by protocol
    private static final String CURRENTTIME = "currenttime";
    private static final String LEDINDICATORSTATUS = "ledindicatorstatus";
    private static final String POWERSAVINGMODE = "powersavingmode";
    private static final String POWERSTATUS = "powerstatus";
    private static final String WOLMODE = "wolmode";
    private static final String LANGUAGE = "language";
    private static final String REBOOT = "reboot";
    private static final String SYSCMD = "sysCmd";
    private static final String POSTALCODE = "postalcode";

    /** The url for the IRCC service */
    private final @Nullable String irccUrl;

    /**
     * Instantiates a new scalar web system protocol.
     *
     * @param factory  the non-null factory
     * @param context  the non-null context
     * @param service  the non-null service
     * @param callback the non-null callback
     * @param irccUrl  the possibly null, possibly empty ircc url
     */
    ScalarWebSystemProtocol(ScalarWebProtocolFactory<T> factory, ScalarWebContext context, ScalarWebService service,
            T callback, @Nullable String irccUrl) {
        super(factory, context, service, callback);

        this.irccUrl = irccUrl;

        enableNotifications(ScalarWebEvent.NOTIFYPOWERSTATUS);
    }

    @Override
    public Collection<ScalarWebChannelDescriptor> getChannelDescriptors() {
        final List<ScalarWebChannelDescriptor> descriptors = new ArrayList<ScalarWebChannelDescriptor>();

        if (getService().hasMethod(ScalarWebMethod.GETCURRENTTIME)) {
            try {
                execute(ScalarWebMethod.GETCURRENTTIME);
                descriptors.add(createDescriptor(createChannel(CURRENTTIME), "DateTime", "scalarsystemcurrenttime"));
            } catch (IOException e) {
                logger.info("Exception getting current time: {}", e.getMessage());
            }
        }

        if (getService().hasMethod(ScalarWebMethod.GETSYSTEMINFORMATION)) {
            try {
                execute(ScalarWebMethod.GETSYSTEMINFORMATION);
                descriptors.add(createDescriptor(createChannel(LANGUAGE), "String", "scalarsystemlanguage"));
            } catch (IOException e) {
                logger.info("Exception getting system information: {}", e.getMessage());
            }
        }

        if (getService().hasMethod(ScalarWebMethod.GETLEDINDICATORSTATUS)) {
            try {
                execute(ScalarWebMethod.GETLEDINDICATORSTATUS);
                descriptors.add(createDescriptor(createChannel(LEDINDICATORSTATUS), "String",
                        "scalarsystemledindicatorstatus"));
            } catch (IOException e) {
                logger.info("Exception getting led indicator status: {}", e.getMessage());
            }
        }

        if (getService().hasMethod(ScalarWebMethod.GETPOWERSAVINGMODE)) {
            try {
                execute(ScalarWebMethod.GETPOWERSAVINGMODE);
                descriptors
                        .add(createDescriptor(createChannel(POWERSAVINGMODE), "String", "scalarsystempowersavingmode"));
            } catch (IOException e) {
                logger.info("Exception getting power savings mode: {}", e.getMessage());
            }
        }

        if (getService().hasMethod(ScalarWebMethod.GETPOWERSTATUS)) {
            try {
                execute(ScalarWebMethod.GETPOWERSTATUS);
                descriptors.add(createDescriptor(createChannel(POWERSTATUS), "Switch", "scalarsystempowerstatus"));
            } catch (IOException e) {
                logger.info("Exception getting power status: {}", e.getMessage());
            }
        }

        if (getService().hasMethod(ScalarWebMethod.GETWOLMODE)) {
            try {
                execute(ScalarWebMethod.GETWOLMODE);
                descriptors.add(createDescriptor(createChannel(WOLMODE), "Switch", "scalarsystemwolmode"));
            } catch (IOException e) {
                logger.info("Exception getting wol mode: {}", e.getMessage());
            }
        }

        if (getService().hasMethod(ScalarWebMethod.GETPOSTALCODE)) {
            try {
                execute(ScalarWebMethod.GETPOSTALCODE);
                descriptors.add(createDescriptor(createChannel(POSTALCODE), "String", "scalarsystempostalcode"));
            } catch (IOException e) {
                logger.info("Exception getting postal code: {}", e.getMessage());
            }
        }

        if (service.hasMethod(ScalarWebMethod.REQUESTREBOOT)) {
            descriptors.add(createDescriptor(createChannel(REBOOT), "Switch", "scalarsystemreboot"));
        }

        // IRCC should be available by default
        descriptors.add(createDescriptor(createChannel(SYSCMD), "String", "scalarsystemircc"));

        return descriptors;
    }

    @Override
    public void refreshState() {
        final ScalarWebChannelTracker tracker = getChannelTracker();
        if (tracker.isCategoryLinked(CURRENTTIME)) {
            refreshCurrentTime();
        }
        if (tracker.isCategoryLinked(LEDINDICATORSTATUS)) {
            refreshLedIndicator();
        }
        if (tracker.isCategoryLinked(LANGUAGE)) {
            refreshLanguage();
        }
        if (tracker.isCategoryLinked(POWERSAVINGMODE)) {
            refreshPowerSavingsMode();
        }
        if (tracker.isCategoryLinked(POWERSTATUS)) {
            refreshPowerStatus();
        }
        if (tracker.isCategoryLinked(WOLMODE)) {
            refreshWolMode();
        }
        if (tracker.isCategoryLinked(REBOOT)) {
            refreshReboot();
        }
        if (tracker.isCategoryLinked(POSTALCODE)) {
            refreshPostalCode();
        }
        if (tracker.isCategoryLinked(SYSCMD)) {
            refreshSysCmd();
        }
    }

    @Override
    public void refreshChannel(ScalarWebChannel channel) {
        Objects.requireNonNull(channel, "channel cannot be null");

        switch (channel.getCategory()) {
            case CURRENTTIME:
                refreshCurrentTime();
                break;

            case LANGUAGE:
                refreshLanguage();
                break;

            case LEDINDICATORSTATUS:
                refreshLedIndicator();
                break;

            case POWERSAVINGMODE:
                refreshPowerSavingsMode();
                break;

            case POWERSTATUS:
                refreshPowerStatus();
                break;

            case WOLMODE:
                refreshWolMode();
                break;

            case REBOOT:
                refreshReboot();
                break;

            case POSTALCODE:
                refreshPostalCode();
                break;

            case SYSCMD:
                refreshSysCmd();
                break;

            default:
                logger.debug("Unknown refresh channel: {}", channel);
                break;
        }
    }

    /**
     * Refresh current time
     */
    private void refreshCurrentTime() {
        try {
            final CurrentTime ct = execute(ScalarWebMethod.GETCURRENTTIME).as(CurrentTime.class);
            stateChanged(CURRENTTIME,
                    new DateTimeType(
                            ZonedDateTime.ofInstant(ct.getDateTime().toCalendar(Locale.getDefault()).toInstant(),
                                    TimeZone.getDefault().toZoneId()).withFixedOffsetZone()));
        } catch (IOException e) {
            logger.debug("Cannot get the current time: {}", e.getMessage());
        }
    }

    /**
     * Refresh the language
     */
    private void refreshLanguage() {
        try {
            final SystemInformation sysInfo = execute(ScalarWebMethod.GETSYSTEMINFORMATION).as(SystemInformation.class);
            stateChanged(LANGUAGE, SonyUtil.newStringType(sysInfo.getLanguage()));
        } catch (IOException e) {
            logger.debug("Cannot get the get system information for refresh langauge: {}", e.getMessage());
        }
    }

    /**
     * Refresh led indicator
     */
    private void refreshLedIndicator() {
        try {
            final LedIndicatorStatus ledStatus = execute(ScalarWebMethod.GETLEDINDICATORSTATUS)
                    .as(LedIndicatorStatus.class);
            stateChanged(LEDINDICATORSTATUS, SonyUtil.newStringType(ledStatus.getMode()));
        } catch (IOException e) {
            logger.debug("Cannot get the get led indicator status: {}", e.getMessage());
        }
    }

    /**
     * Refresh power savings mode
     */
    private void refreshPowerSavingsMode() {
        try {
            final PowerSavingMode mode = execute(ScalarWebMethod.GETPOWERSAVINGMODE).as(PowerSavingMode.class);
            stateChanged(POWERSAVINGMODE, SonyUtil.newStringType(mode.getMode()));
        } catch (IOException e) {
            logger.debug("Cannot get the get power savings mode: {}", e.getMessage());
        }
    }

    /**
     * Refresh postal code
     */
    private void refreshPostalCode() {
        try {
            final PostalCode postalCode = execute(ScalarWebMethod.GETPOSTALCODE).as(PostalCode.class);
            stateChanged(POSTALCODE, SonyUtil.newStringType(postalCode.getPostalCode()));
        } catch (IOException e) {
            logger.debug("Cannot get the get postal code: {}", e.getMessage());
        }
    }

    /**
     * Refresh power status
     */
    private void refreshPowerStatus() {
        try {
            if (VersionUtilities.equals(getVersion(ScalarWebMethod.GETPOWERSTATUS), ScalarWebMethod.V1_0)) {
                notifyPowerStatus(execute(ScalarWebMethod.GETPOWERSTATUS).as(PowerStatusResult_1_0.class));
            } else {
                notifyPowerStatus(execute(ScalarWebMethod.GETPOWERSTATUS).as(PowerStatusResult_1_1.class));
            }
        } catch (IOException e) {
            logger.debug("Cannot refresh the power status: {}", e.getMessage());
        }
    }

    /**
     * Refresh wol mode
     */
    private void refreshWolMode() {
        try {
            final WolMode mode = execute(ScalarWebMethod.GETWOLMODE).as(WolMode.class);
            stateChanged(WOLMODE, mode.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        } catch (IOException e) {
            logger.debug("Cannot get the get WOL mode: {}", e.getMessage());
        }
    }

    /**
     * Refresh reboot
     */
    private void refreshReboot() {
        callback.stateChanged(REBOOT, OnOffType.OFF);
    }

    /**
     * Refresh system command
     */
    private void refreshSysCmd() {
        callback.stateChanged(SYSCMD, StringType.EMPTY);
    }

    @Override
    public void setChannel(ScalarWebChannel channel, Command command) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(command, "command cannot be null");

        switch (channel.getCategory()) {
            case LEDINDICATORSTATUS:
                if (command instanceof StringType) {
                    setLedIndicatorStatus(command.toString());
                } else {
                    logger.debug("LEDINDICATORSTATUS command not an StringType: {}", command);
                }

                break;

            case LANGUAGE:
                if (command instanceof StringType) {
                    setLanguage(command.toString());
                } else {
                    logger.debug("LANGUAGE command not an StringType: {}", command);
                }

                break;

            case POWERSAVINGMODE:
                if (command instanceof StringType) {
                    setPowerSavingMode(command.toString());
                } else {
                    logger.debug("POWERSAVINGMODE command not an StringType: {}", command);
                }

                break;

            case POWERSTATUS:
                if (command instanceof OnOffType) {
                    setPowerStatus(command == OnOffType.ON);
                } else {
                    logger.debug("POWERSTATUS command not an OnOffType: {}", command);
                }

                break;

            case WOLMODE:
                if (command instanceof OnOffType) {
                    setWolMode(command == OnOffType.ON);
                } else {
                    logger.debug("WOLMODE command not an OnOffType: {}", command);
                }

                break;

            case REBOOT:
                if (command instanceof OnOffType && command == OnOffType.ON) {
                    requestReboot();
                } else {
                    logger.debug("REBOOT command not an OnOffType: {}", command);
                }

                break;

            case POSTALCODE:
                if (command instanceof StringType) {
                    setPostalCode(command.toString());
                } else {
                    logger.debug("POSTALCODE command not an StringType: {}", command);
                }

                break;

            case SYSCMD:
                if (command instanceof StringType) {
                    sendCommand(command.toString());
                } else {
                    logger.debug("SYSCMD command not an StringType: {}", command);
                }

                break;

            default:
                logger.debug("Unhandled channel command: {} - {}", channel, command);
                break;
        }
    }

    /**
     * Sets the led indicator status
     *
     * @param mode the non-null, non-empty new led indicator status
     */
    private void setLedIndicatorStatus(String mode) {
        Validate.notEmpty(mode, "mode cannot be empty");
        handleExecute(ScalarWebMethod.SETLEDINDICATORSTATUS, new LedIndicatorStatus(mode, ""));
    }

    /**
     * Sets the language
     *
     * @param language the non-null, non-empty new language
     */
    private void setLanguage(String language) {
        Validate.notEmpty(language, "language cannot be empty");
        handleExecute(ScalarWebMethod.SETLANGUAGE, new Language(language));
    }

    /**
     * Sets the power saving mode
     *
     * @param mode the non-null, non-empty new power saving mode
     */
    private void setPowerSavingMode(String mode) {
        Validate.notEmpty(mode, "mode cannot be empty");
        handleExecute(ScalarWebMethod.SETPOWERSAVINGMODE, new PowerSavingMode(mode));
    }

    /**
     * Sets the power status
     *
     * @param status true for on, false otherwise
     */
    private void setPowerStatus(boolean status) {
        if (status) {
            SonyUtil.sendWakeOnLan(logger, getContext().getConfig().getDeviceIpAddress(), getContext().getConfig().getDeviceMacAddress());
        }

        handleExecute(ScalarWebMethod.SETPOWERSTATUS, version -> {
            if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
                return new PowerStatusRequest_1_0(status);
            }
            return new PowerStatusRequest_1_1(status);
        });
    }

    /**
     * Sets the wol mode
     *
     * @param enabled true to enable WOL, false otherwise
     */
    private void setWolMode(boolean enabled) {
        handleExecute(ScalarWebMethod.SETWOLMODE, new WolMode(enabled));
    }

    /**
     * Sets the postal code
     *
     * @param postalCode the non-null, non-empty new postal code
     */
    private void setPostalCode(String postalCode) {
        Validate.notEmpty(postalCode, "postalCode cannot be empty");
        handleExecute(ScalarWebMethod.SETPOSTALCODE, new PostalCode(postalCode));
    }

    /**
     * Request reboot.
     */
    private void requestReboot() {
        handleExecute(ScalarWebMethod.REQUESTREBOOT);
    }

    /**
     * Send command.
     *
     * @param cmd the cmd
     */
    public void sendCommand(String cmd) {
        if (StringUtils.isEmpty(cmd)) {
            return;
        }

        final String localIrccUrl = irccUrl;
        if (localIrccUrl == null || StringUtils.isEmpty(localIrccUrl)) {
            logger.debug("IRCC URL was not specified in configuration");
        } else {
            try {
                final IrccClient irccClient = new IrccClientFactory().get(localIrccUrl);
                final ScalarWebContext context = getContext();
                String localCmd = cmd;

                final String cmdMap = context.getConfig().getCommandsMapFile();

                final TransformationService localTransformService = context.getTransformService();
                if (localTransformService != null && cmdMap != null) {
                    String code;
                    try {
                        code = localTransformService.transform(cmdMap, cmd);

                        if (StringUtils.isNotBlank(code)) {
                            logger.debug("Transformed {} with map file '{}' to {}", cmd, cmdMap, code);

                            try {
                                localCmd = URLDecoder.decode(code, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                localCmd = code;
                            }
                        }
                    } catch (TransformationException e) {
                        logger.error("Failed to transform {} using map file '{}', exception={}", cmd, cmdMap,
                                e.getMessage());
                        return;
                    }
                }

                if (localCmd == null || StringUtils.isEmpty(localCmd)) {
                    logger.debug("IRCC command was empty or null - ignoring");
                    return;
                }

                // Always use an http transport to execute soap
                HttpResponse httpResponse;
                try (final SonyTransport transport = SonyTransportFactory
                        .createHttpTransport(irccClient.getBaseUrl().toExternalForm())) {
                    // copy all the options from the parent one (authentication options)
                    getService().getTransport().getOptions().stream().forEach(o->transport.setOption(o));
                    httpResponse = irccClient.executeSoap(transport, localCmd);
                } catch (URISyntaxException e) {
                    logger.debug("URI syntax exception: {}", e.getMessage());
                    return;
                }

                switch (httpResponse.getHttpCode()) {
                    case HttpStatus.OK_200:
                        // everything is great!
                        break;

                    case HttpStatus.SERVICE_UNAVAILABLE_503:
                        logger.debug("IRCC service is unavailable (power off?)");
                        break;

                    case HttpStatus.FORBIDDEN_403:
                        logger.debug("IRCC methods have been forbidden on service {} ({}): {}",
                                service.getServiceName(), irccClient.getBaseUrl(), httpResponse);
                        break;

                    case HttpStatus.INTERNAL_SERVER_ERROR_500:
                        final SOAPError soapError = httpResponse.getSOAPError();
                        if (soapError == null) {
                            final IOException e = httpResponse.createException();
                            logger.debug("Communication error for IRCC method on service {} ({}): {}",
                                    service.getServiceName(), irccClient.getBaseUrl(), e.getMessage(), e);
                            callback.statusChanged(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                    e.getMessage());
                            break;
                        } else {
                            logger.debug("SOAP Error: ({}) {}", soapError.getSoapCode(),
                                    soapError.getSoapDescription());
                        }
                        break;

                    default:
                        final IOException e = httpResponse.createException();
                        logger.debug("Communication error for IRCC method on service {} ({}): {}",
                                service.getServiceName(), irccClient.getBaseUrl(), e.getMessage(), e);
                        callback.statusChanged(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                e.getMessage());
                        break;
                }

            } catch (IOException | URISyntaxException e) {
                logger.debug("Cannot create IRCC client: {}", e.getMessage(), e);
                return;
            }
        }
    }

    @Override
    protected void eventReceived(ScalarWebEvent event) throws IOException {
        switch (event.getMethod()) {
            case ScalarWebEvent.NOTIFYPOWERSTATUS:
                final String version = getVersion(ScalarWebMethod.GETPOWERSTATUS);
                if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
                    notifyPowerStatus(event.as(PowerStatusResult_1_0.class));
                } else {
                    notifyPowerStatus(event.as(PowerStatusResult_1_1.class));
                }

                break;

            default:
                logger.debug("Unhandled event received: {}", event);
                break;
        }
    }

    protected void notifyPowerStatus(PowerStatusResult_1_0 status) {
        stateChanged(POWERSTATUS, status.isActive() ? OnOffType.ON : OnOffType.OFF);
    }

    protected void notifyPowerStatus(PowerStatusResult_1_1 status) {
        stateChanged(POWERSTATUS, status.isActive() ? OnOffType.ON : OnOffType.OFF);
    }

    @Override
    public void close() {
        super.close();
    }
}
