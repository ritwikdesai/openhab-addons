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
package org.openhab.binding.sony.internal.simpleip;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.transform.TransformationException;
import org.eclipse.smarthome.core.transform.TransformationService;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.net.NetUtil;
import org.openhab.binding.sony.internal.net.SocketChannelSession;
import org.openhab.binding.sony.internal.net.SocketSession;
import org.openhab.binding.sony.internal.net.SocketSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the protocol handler for the Simple IP System. This handler will issue the protocol commands and will
 * process the responses from the Simple IP system. The Simple IP system is a little flacky and doesn't seem to handle
 * multiple commands in a single session. For this reason, we create a single {@link SocketSession} to listen for any
 * notifications (whose lifetime matches that of this handler) and then create separate {@link SocketSession} for each
 * request. Special care must be taken to differentiate between a Control request result and the Inquiry/Notification
 * results to avoid misinterpreting the result (the control "success" message will have all zeroes - which has a form
 * that matches some inquiry/notification results (like volume could be interpreted as 0!).
 *
 * Additional documentation: https://pro-bravia.sony.net/develop/integrate/ssip/overview/
 *
 * @author Tim Roberts - Initial contribution
 *
 *
 */
@NonNullByDefault
class SimpleIpProtocol implements SocketSessionListener, AutoCloseable {

    // Logger
    private Logger logger = LoggerFactory.getLogger(SimpleIpProtocol.class);

    // Protocol Constants
    private static final char TYPE_CONTROL = 'C';
    private static final char TYPE_QUERY = 'E';
    private static final char TYPE_ANSWER = 'A';
    private static final char TYPE_NOTIFY = 'N';

    private static final String IRCC = "IRCC";
    private static final String POWER = "POWR";
    private static final String TOGGLE_POWER = "TPOW";
    private static final String VOLUME = "VOLU";
    private static final String AUDIO_MUTE = "AMUT";
    private static final String INPUT = "INPT";
    private static final String PICTURE_MUTE = "PMUT";
    private static final String TOGGLE_PICTURE_MUTE = "TPMU";
    private static final String BROADCAST_ADDRESS = "BADR";
    private static final String MACADDRESS = "MADR";
    private static final String SCENE = "SCEN";

    private static final String CHANNEL = "CHNN";
    private static final String TRIPLET_CHANNEL = "TCHN";
    private static final String INPUT_SOURCE = "ISRC";
    private static final String PICTURE_IN_PICTURE = "PIPI";
    private static final String TOGGLE_PICTURE_IN_PICTURE = "TPIP";
    private static final String TOGGLE_PIP_POSITION = "TPPP";

    private static final String NO_PARM = "################";

    // Size of the parameter area as defined by the spec
    private static final int PARMSIZE = 16;

    // Response strings/patterns
    private static final String RSP_SUCCESS = "0000000000000000";
    private static final String RSP_ERROR = "FFFFFFFFFFFFFFFF";
    private static final String RSP_NOSUCHTHING = "NNNNNNNNNNNNNNNN";
    private static final Pattern RSP_NOTIFICATION = Pattern
            .compile("^\\*S([" + TYPE_ANSWER + TYPE_NOTIFY + "])(\\w{4})(.*{16})");

    /** The {@link SocketSession} that will listen for notifications. */
    private final SocketSession listeningSession;

    /** The {@link SimpleIpConfig} for creating new {@link SocketSession}. */
    private final SimpleIpConfig config;

    /** The {@link ThingCallback} that we can callback to set state and status. */
    private final ThingCallback<String> callback;

    /** The {@link TransformationService} to use */
    private final @Nullable TransformationService transformService;

    /** The constant for the TV Input in {@link #INPUT_TYPES} */
    private static final int INPUT_TV = 0;

    /** Represents a lookup between the known input source types and their corresponding simple IP identifier. */
    private static final Map<Integer, String> INPUT_TYPES = new HashMap<Integer, String>();
    static {
        INPUT_TYPES.put(INPUT_TV, "TV");
        INPUT_TYPES.put(10000, "HDMI");
        INPUT_TYPES.put(20000, "SCART");
        INPUT_TYPES.put(30000, "Composite");
        INPUT_TYPES.put(40000, "Component");
        INPUT_TYPES.put(50000, "Screen Mirroring");
        INPUT_TYPES.put(60000, "PC RGB Input");
    }

    /**
     * Constructs the protocol handler from given parameters. This constructor will create the
     * {@link #listeningSession} to listen to notifications sent by the Simple IP device (adding ourselfs as the
     * listener).
     *
     * @param config           a non-null {@link SimpleIpConfig} (may be connected or disconnected)
     * @param transformService a possibly null {@link TransformationService}
     * @param callback         a non-null {@link ThingCallback} to callback
     * @throws IOException Signals that an I/O exception has occurred.
     */
    SimpleIpProtocol(SimpleIpConfig config, @Nullable TransformationService transformService,
            ThingCallback<String> callback) throws IOException {
        Objects.requireNonNull(config, "config cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");

        this.config = config;
        this.transformService = transformService;
        this.callback = callback;

        final Integer devicePort = config.getDevicePort();
        Objects.requireNonNull(devicePort, "devicePort cannot be null");

        listeningSession = new SocketChannelSession(config.getDeviceIpAddress(), devicePort);
        listeningSession.addListener(this);
    }

    /**
     * Attempts to log into the system. The login will connect the {@link #listeningSession} and immediately call
     * {@link #postLogin()} since there is no authentication mechanisms
     *
     * @return always null to indicate a successful login
     * @throws IOException if an exception occurs trying to connect our {@link #listeningSession} or writing the
     *                         command file
     */
    @Nullable
    String login() throws IOException {
        listeningSession.connect();
        return null;
    }

    /**
     * Post successful login stuff - mark us online!.
     *
     * @throws IOException if an IO exception occurs writing the map file
     */
    void postLogin() throws IOException {
        writeCommands();

        final String netIf = config.getNetInterface();
        refreshBroadcastAddress(netIf);
        refreshMacAddress(netIf);
    }

    /**
     * Writes the commands to the commands map file if it doesn't exist.
     *
     * @throws IOException if an IO exception occurs writing the map file
     */
    private void writeCommands() throws IOException {
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

            logger.info("Writing remote commands to {}", file);
            Files.write(file, getDefaultCommands(), Charset.forName("UTF-8"));
        }
    }

    /**
     * Called to refresh some of the state of the simple IP system (mainly state that either we won't get notifications
     * for
     * or whose state may commonly change due to remote actions.
     */
    void refreshState() {
        refreshVolume();
        refreshChannel();
        refreshTripletChannel();
        refreshInputSource();
        refreshPictureInPicture();
    }

    /**
     * Sends the command and puts the thing into {@link ThingStatus#OFFLINE} if an IOException occurs. This method will
     * create a new {@link SocketSession} for the command and a anonymous {@link SocketSessionListener} to listen for
     * the result. The connection will then be closed/disposed of when a valid response is received (if the listener is
     * more than 10 seconds old, it will be disposed of to avoid memory leaks).
     *
     * @param type    the type of command ({@link #TYPE_CONTROL} or {@link #TYPE_QUERY})
     * @param command a non-null, non-empty command to send
     * @param parm    the non-null, non-empty parameters for the command. Must be exactly {@link #PARMSIZE} in length
     */
    private void sendCommand(final char type, final String command, String parm) {
        Validate.notEmpty(command, "command cannot be empty");
        Validate.notEmpty(parm, "parm cannot be empty");

        if (parm.length() != PARMSIZE) {
            throw new IllegalArgumentException("parm must be exactly " + PARMSIZE + " in length: " + parm);
        }

        final String ipAddress = config.getDeviceIpAddress();
        if (ipAddress == null || StringUtils.isEmpty(ipAddress)) {
            throw new IllegalArgumentException("ipAddress cannot be empty");
        }

        final Integer devicePort = config.getDevicePort();
        Objects.requireNonNull(devicePort, "devicePort cannot be null");

        // Create our command
        final String cmd = "*S" + type + command + parm;

        // SimpleIP seems to need each request on it's own socket - so provide that here.
        try {
            logger.debug("Sending '{}'", cmd);
            NetUtil.sendSocketRequest(ipAddress, devicePort, cmd, new SocketSessionListener() {
                @Override
                public boolean responseReceived(String response) {
                    final String rsp = StringUtils.trim(response); // remove whitespace
                    // See if the response is valid
                    final Matcher m = RSP_NOTIFICATION.matcher(rsp);
                    if (m.matches() && m.groupCount() == 3) {
                        // make sure we only process responses for our command
                        if (m.group(1).equals(Character.toString(TYPE_ANSWER)) && m.group(2).equals(command)) {
                            logger.debug("Send '{}' result: '{}'", cmd, rsp);
                            if (type == TYPE_CONTROL) {
                                handleResponse(m, rsp, cmd);
                            } else if (type == TYPE_QUERY) {
                                handleNotification(m, rsp);
                            } else {
                                logger.error("Unknown command type: {}", cmd);
                            }
                            return true;
                        }
                    } else if (StringUtils.isEmpty(rsp)) {
                        logger.debug("Empty reponse (or unsupported command): '{}'", rsp, cmd);
                    } else {
                        logger.warn("Unparsable response '{}' to command '{}'", rsp, cmd);
                    }
                    return false;
                }

                @Override
                public void responseException(Exception e) {
                    if (e instanceof SocketTimeoutException) {
                        logger.debug("(SocketTimeoutException) Response took too long - ignoring");
                    } else {
                        SimpleIpProtocol.this.responseException(e);
                    }

                }
            });
        } catch (IOException e) {
            callback.statusChanged(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Exception occurred sending command: " + e);
        }
    }

    /**
     * Refreshes the power status.
     */
    void refreshPower() {
        sendCommand(TYPE_QUERY, POWER, NO_PARM);
    }

    /**
     * Refreshes the volume status.
     */
    void refreshVolume() {
        sendCommand(TYPE_QUERY, VOLUME, NO_PARM);
    }

    /**
     * Refreshes the audio mute status.
     */
    void refreshAudioMute() {
        sendCommand(TYPE_QUERY, AUDIO_MUTE, NO_PARM);
    }

    /**
     * Refreshes the channel.
     */
    void refreshChannel() {
        sendCommand(TYPE_QUERY, CHANNEL, NO_PARM);
    }

    /**
     * Refreshes the channel triplet.
     */
    void refreshTripletChannel() {
        sendCommand(TYPE_QUERY, TRIPLET_CHANNEL, NO_PARM);
    }

    /**
     * Refreshes the input source.
     */
    void refreshInputSource() {
        sendCommand(TYPE_QUERY, INPUT_SOURCE, NO_PARM);
    }

    /**
     * Refreshes the input.
     */
    void refreshInput() {
        sendCommand(TYPE_QUERY, INPUT, NO_PARM);
    }

    /**
     * Refreshes the input.
     */
    void refreshScene() {
        sendCommand(TYPE_QUERY, SCENE, NO_PARM);
    }

    /**
     * Refreshes the picture mute.
     */
    void refreshPictureMute() {
        sendCommand(TYPE_QUERY, PICTURE_MUTE, NO_PARM);
    }

    /**
     * Refreshes the PIP.
     */
    void refreshPictureInPicture() {
        sendCommand(TYPE_QUERY, PICTURE_IN_PICTURE, NO_PARM);
    }

    /**
     * Refreshes the broadcast address.
     *
     * @param netInterface the non-null, non-empty network interface to inquire
     */
    private void refreshBroadcastAddress(String netInterface) {
        Validate.notEmpty(netInterface, "netInterface cannot be empty");
        sendCommand(TYPE_QUERY, BROADCAST_ADDRESS, StringUtils.rightPad(netInterface, PARMSIZE, '#'));
    }

    /**
     * Refreshes the mac address of the given network interface.
     *
     * @param netInterface the non-null, non-empty interface
     */
    private void refreshMacAddress(String netInterface) {
        Validate.notEmpty(netInterface, "netInterface cannot be empty");
        sendCommand(TYPE_QUERY, MACADDRESS, StringUtils.rightPad(netInterface, PARMSIZE, '#'));
    }

    /**
     * Sends the specified IR command to the device
     *
     * @param irCmd the non-null, non-empty IR command to send
     */
    void setIR(String irCmd) {
        Validate.notEmpty(irCmd, "irCmd cannot be empty");

        final String cmdMap = config.getCommandsMapFile();

        String code = irCmd;
        try {
            if (transformService != null && cmdMap != null) {
                code = transformService.transform(cmdMap, irCmd);
                if (!StringUtils.equalsIgnoreCase(code, irCmd)) {
                    logger.debug("Transformed {} with map file '{}' to {}", irCmd, cmdMap, code);
                }
            }
        } catch (TransformationException e) {
            logger.error("Failed to transform {} using map file '{}', exception={}", irCmd, cmdMap, e.getMessage());
            return;
        }

        if (StringUtils.isEmpty(code)) {
            logger.warn("No entry for {} in map file '{}'", irCmd, cmdMap);
            return;
        }

        try {
            Integer.parseInt(code);
        } catch (NumberFormatException e) {
            logger.warn("The resulting code {} was not an integer - transformed from {}", code, irCmd);
        }

        logger.debug("Transformed {} with map file '{}' to {}", irCmd, cmdMap, code);

        sendCommand(TYPE_CONTROL, IRCC, StringUtils.leftPad(code, PARMSIZE, '0'));
        refreshPower();
        refreshState();
    }

    /**
     * Sets the power on/off to the device
     *
     * @param on true if on, false off
     */
    void setPower(boolean on) {
        if (on) {
            final String ipAddress = config.getDeviceIpAddress();
            final String macAddress = config.getDeviceMacAddress();
            if (macAddress != null && StringUtils.isNotEmpty(macAddress) && ipAddress != null
                    && StringUtils.isNotEmpty(ipAddress)) {
                try {
                    logger.debug("Sending WOL packet to {} ({})", macAddress, ipAddress);
                    NetUtil.sendWol(ipAddress, macAddress);
                } catch (IOException e) {
                    logger.debug("IOException during sending of WOL packet - ignored: {}", e.getMessage());
                }
            }
        }
        sendCommand(TYPE_CONTROL, POWER, StringUtils.leftPad(on ? "1" : "0", PARMSIZE, '0'));
    }

    /**
     * Toggles the power
     */
    void togglePower() {
        sendCommand(TYPE_CONTROL, TOGGLE_POWER, NO_PARM);
    }

    /**
     * Sets the volume level
     *
     * @param volume a volume between 0-100
     * @throws IllegalArgumentException if < 0 or > 100
     */
    void setAudioVolume(int volume) {
        if (volume < 0 || volume > 100) {
            throw new IllegalArgumentException("volume must be between 0-100");
        }
        sendCommand(TYPE_CONTROL, VOLUME, StringUtils.leftPad(Integer.toString(volume), PARMSIZE, '0'));
    }

    /**
     * Sets the audio mute
     *
     * @param on true for muted, false otherwise
     */
    void setAudioMute(boolean on) {
        sendCommand(TYPE_CONTROL, AUDIO_MUTE, StringUtils.leftPad(on ? "1" : "0", PARMSIZE, '0'));
    }

    /**
     * Sets the channel. Channel must be in the form of "x.x" or simply "x" where x must be numeric
     *
     * @param channel the non-null, non-empty channel in the form of "x.x" or "x" (such as 50.1 or 6)
     * @throws IllegalArgumentException if channel is not in the form of "x.x" or "x" where x is numeric
     */
    void setChannel(String channel) {
        Validate.notEmpty(channel, "channel cannot be empty");

        final int period = channel.indexOf('.');
        final String pre = StringUtils.trimToNull(period < 0 ? channel : channel.substring(0, period));
        final String post = StringUtils.trimToNull(period < 0 ? "0" : channel.substring(period + 1));
        try {
            final int preNum = pre == null ? 0 : Integer.parseInt(pre);
            final int postNum = post == null ? 0 : Integer.parseInt(post);
            final String cmd = StringUtils.leftPad(Integer.toString(preNum), 8, '0') + "."
                    + StringUtils.rightPad(Integer.toString(postNum), 7, '0');
            sendCommand(TYPE_CONTROL, CHANNEL, cmd);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("channel could not be parsed: " + channel);
        }
    }

    /**
     * Sets the triplet channel. Channel must be in the form of "x.x.x" where x must be numeric
     *
     * @param channel the non-null, non-empty channel in the form of "x.x.x" (such as 32736.32736.1024)
     * @throws IllegalArgumentException if channel is not in the form of "x.x.x"
     */
    void setTripletChannel(String channel) {
        Validate.notEmpty(channel, "channel cannot be empty");

        final int firstPeriod = channel.indexOf('.');
        if (firstPeriod < 0) {
            throw new IllegalArgumentException(
                    "Could not find the number of the first part of the triplet channel: " + channel);
        }

        final int secondPeriod = channel.indexOf(',', firstPeriod + 1);
        if (firstPeriod < 0) {
            throw new IllegalArgumentException(
                    "Could not find the number of the second part of the triplet channel: " + channel);
        }

        final String first = StringUtils.trimToNull(channel.substring(0, firstPeriod));
        final String second = StringUtils.trimToNull(channel.substring(firstPeriod + 1, secondPeriod));
        final String third = StringUtils.trimToNull(channel.substring(secondPeriod + 1));
        try {
            final int firstNum = first == null ? 0 : Integer.parseInt(first);
            final int secondNum = second == null ? 0 : Integer.parseInt(second);
            final int thirdNum = third == null ? 0 : Integer.parseInt(third);

            final String firstHex = StringUtils.leftPad(Integer.toHexString(firstNum), 4, '0');
            final String secondHex = StringUtils.leftPad(Integer.toHexString(secondNum), 4, '0');
            final String thirdHex = StringUtils.leftPad(Integer.toHexString(thirdNum), 4, '0');

            sendCommand(TYPE_CONTROL, CHANNEL, firstHex + secondHex + thirdHex + "####");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("channel could not be parsed: " + channel);
        }
    }

    /**
     * Sets the input source and will refresh channel and triplet channel afterwards. This must be a valid string
     * recognized by the simple IP device.
     *
     * @param source a non-null, non-empty input source
     */
    void setInputSource(String source) {
        Validate.notEmpty(source, "source cannot be empty");

        sendCommand(TYPE_CONTROL, INPUT_SOURCE, StringUtils.rightPad(source, PARMSIZE, '#'));
    }

    /**
     * Sets the scene. This must be a valid string recognized by the simple IP device.
     *
     * @param scene a non-null, non-empty scene name
     */
    void setScene(String scene) {
        Validate.notEmpty(scene, "scene cannot be empty");

        sendCommand(TYPE_CONTROL, SCENE, StringUtils.rightPad(scene, PARMSIZE, '#'));
    }

    /**
     * Sets the input port and will refresh input source, channel and triplet channel afterwards. This must be a valid
     * in the form of "xxxxyyyy" where xxxx is the name of the input and yyyy is the port number (like "hdmi1"). The
     * valid input names are "TV", "HDMI", "SCART", "Composite", "Component", "Screen Mirroring", and "PC RGB Input"
     * (case doesn't matter). The port number does NOT apply to "TV".
     *
     * @param input a non-null, non-empty input port
     */
    void setInput(String input) {
        Validate.notEmpty(input, "input cannot be empty");

        int typeCode = -1;
        int portNbr = -1;
        final String lowerInput = input.toLowerCase();
        for (Entry<Integer, String> entry : INPUT_TYPES.entrySet()) {
            if (lowerInput.startsWith(entry.getValue().toLowerCase())) {
                typeCode = entry.getKey();
                if (typeCode != INPUT_TV) {
                    try {
                        String portS = StringUtils.trimToNull(input.substring(entry.getValue().length()));
                        portNbr = portS == null ? 0 : Integer.parseInt(portS);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "The port number on the input is invalid (not an integer): " + input);
                    }
                } else {
                    portNbr = 0;
                }
                break;
            }
        }
        if (typeCode == -1) {
            throw new IllegalArgumentException("Unknown input: " + input);
        }
        sendCommand(TYPE_CONTROL, INPUT, StringUtils.leftPad(Integer.toString(typeCode), 12, '0')
                + StringUtils.leftPad(Integer.toString(portNbr), 4, '0'));
    }

    /**
     * Sets the picture mute
     *
     * @param on true for muted, false otherwise
     */
    void setPictureMute(boolean on) {
        sendCommand(TYPE_CONTROL, PICTURE_MUTE, StringUtils.leftPad(on ? "1" : "0", PARMSIZE, '0'));
    }

    /**
     * Toggles the picture mute
     */
    void togglePictureMute() {
        sendCommand(TYPE_CONTROL, TOGGLE_PICTURE_MUTE, NO_PARM);
    }

    /**
     * Sets the PIP enabling
     *
     * @param on true to enable, false otherwise
     */
    void setPictureInPicture(boolean on) {
        sendCommand(TYPE_CONTROL, PICTURE_IN_PICTURE, StringUtils.leftPad(on ? "1" : "0", PARMSIZE, '0'));
    }

    /**
     * Toggles PIP enabling
     */
    void togglePictureInPicture() {
        sendCommand(TYPE_CONTROL, TOGGLE_PICTURE_IN_PICTURE, NO_PARM);
    }

    /**
     * Toggles the PIP position
     */
    void togglePipPosition() {
        sendCommand(TYPE_CONTROL, TOGGLE_PIP_POSITION, NO_PARM);
    }

    /**
     * Handles control responses from commands (*SC->*SA).
     *
     * @param m        a non-null matcher
     * @param response the non-null, possibly empty response
     * @param command  the possibly null, possibly empty command that triggered this response
     */
    private void handleResponse(Matcher m, String response, @Nullable String command) {
        Objects.requireNonNull(m, "m cannot be null");
        Objects.requireNonNull(response, "response cannot be null");

        if (m.groupCount() == 3) {
            final String cmd = m.group(2);
            final String parms = m.group(3);

            if (IRCC.equalsIgnoreCase(cmd)) {
                handleIRResponse(parms);
            } else if (POWER.equalsIgnoreCase(cmd)) {
                handlePowerResponse(parms);
            } else if (TOGGLE_POWER.equalsIgnoreCase(cmd)) {
                handleTogglePowerResponse(parms);
            } else if (VOLUME.equalsIgnoreCase(cmd)) {
                handleAudioVolumeResponse(parms);
            } else if (AUDIO_MUTE.equalsIgnoreCase(cmd)) {
                handleAudioMuteResponse(parms);
            } else if (CHANNEL.equalsIgnoreCase(cmd)) {
                handleChannelResponse(parms);
            } else if (TRIPLET_CHANNEL.equalsIgnoreCase(cmd)) {
                handleTripletChannelResponse(parms);
            } else if (INPUT_SOURCE.equalsIgnoreCase(cmd)) {
                handleInputSourceResponse(parms);
            } else if (INPUT.equalsIgnoreCase(cmd)) {
                handleInputResponse(parms);
            } else if (SCENE.equalsIgnoreCase(cmd)) {
                handleSceneResponse(parms);
            } else if (PICTURE_MUTE.equalsIgnoreCase(cmd)) {
                handlePictureMuteResponse(parms);
            } else if (TOGGLE_PICTURE_MUTE.equalsIgnoreCase(cmd)) {
                handleTogglePictureMuteResponse(parms);
            } else if (PICTURE_IN_PICTURE.equalsIgnoreCase(cmd)) {
                handlePictureInPictureResponse(parms);
            } else if (TOGGLE_PICTURE_IN_PICTURE.equalsIgnoreCase(cmd)) {
                handleTogglePictureInPictureResponse(parms);
            } else if (TOGGLE_PIP_POSITION.equalsIgnoreCase(cmd)) {
                handleTogglePIPPosition(parms);
            } else {
                logger.warn("Unknown command response '{}' to command '{}' ", response, command);
            }
        }
    }

    /**
     * Handles notification messages (*SN) and query responses (*SE->*SA).
     *
     * @param m        a non-null matcher
     * @param response the non-null, possibly empty response
     */
    private void handleNotification(Matcher m, String response) {
        Objects.requireNonNull(m, "m cannot be null");
        Objects.requireNonNull(response, "response cannot be null");

        if (m.groupCount() == 3) {
            final String cmd = m.group(2);
            final String parms = m.group(3);

            if (POWER.equalsIgnoreCase(cmd)) {
                handlePowerNotification(parms);
            } else if (VOLUME.equalsIgnoreCase(cmd)) {
                handleAudioVolumeNotification(parms);
            } else if (AUDIO_MUTE.equalsIgnoreCase(cmd)) {
                handleAudioMuteNotification(parms);
            } else if (CHANNEL.equalsIgnoreCase(cmd)) {
                handleChannelNotification(parms);
            } else if (TRIPLET_CHANNEL.equalsIgnoreCase(cmd)) {
                handleTripletChannelNotification(parms);
            } else if (INPUT_SOURCE.equalsIgnoreCase(cmd)) {
                handleInputSourceNotification(parms);
            } else if (INPUT.equalsIgnoreCase(cmd)) {
                handleInputNotification(parms);
            } else if (SCENE.equalsIgnoreCase(cmd)) {
                handleSceneNotification(parms);
            } else if (PICTURE_MUTE.equalsIgnoreCase(cmd)) {
                handlePictureMuteNotification(parms);
            } else if (PICTURE_IN_PICTURE.equalsIgnoreCase(cmd)) {
                handlePictureInPictureNotification(parms);
            } else if (BROADCAST_ADDRESS.equalsIgnoreCase(cmd)) {
                handleBroadcastAddressResponse(parms);
            } else if (MACADDRESS.equalsIgnoreCase(cmd)) {
                handleMacAddressResponse(parms);
            } else {
                logger.warn("Unknown notification: {}", response);
            }
        }

    }

    /**
     * Handles the IRCC command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleIRResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", IRCC, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else {
            logger.warn("Unknown {} response: {}", IRCC, parms);
        }
    }

    /**
     * Handles the POWR command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handlePowerResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", POWER, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else {
            logger.warn("Unknown {} response: {}", POWER, parms);
        }
    }

    /**
     * Handles the TPOW command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleTogglePowerResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", TOGGLE_POWER, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else {
            logger.warn("Unknown {} response: {}", TOGGLE_POWER, parms);
        }
    }

    /**
     * Handles the power notification/query response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handlePowerNotification(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", POWER, parms);
        } else {
            try {
                final int power = StringUtils.isEmpty(parms) ? 0 : Integer.parseInt(parms);
                if (power == 0) {
                    callback.stateChanged(SimpleIpConstants.CHANNEL_POWER, OnOffType.OFF);
                } else if (power == 1) {
                    callback.stateChanged(SimpleIpConstants.CHANNEL_POWER, OnOffType.ON);
                } else {
                    logger.warn("Unknown {} response: {}", POWER, parms);
                }
            } catch (NumberFormatException e) {
                logger.warn("Unparsable {} response: {}", POWER, parms);
            }

            refreshState();
        }
    }

    /**
     * Handles the audio volume command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleAudioVolumeResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", VOLUME, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else {
            logger.warn("Unknown {} response: {}", VOLUME, parms);
        }
    }

    /**
     * Handles the audio volume notification/query response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleAudioVolumeNotification(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", VOLUME, parms);
            // you'll get error when tv is off/muted
            callback.stateChanged(SimpleIpConstants.CHANNEL_VOLUME, new PercentType(0));
        } else {
            try {
                final int volume = StringUtils.isEmpty(parms) ? 0 : Integer.parseInt(parms);
                callback.stateChanged(SimpleIpConstants.CHANNEL_VOLUME, new PercentType(volume));
            } catch (NumberFormatException e) {
                logger.warn("Unparsable {} response: {}", VOLUME, parms);
            }
        }
    }

    /**
     * Handles the audio mute command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleAudioMuteResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", AUDIO_MUTE, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else {
            logger.warn("Unknown {} response: {}", AUDIO_MUTE, parms);
        }
    }

    /**
     * Handles the audio mute notification/query response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleAudioMuteNotification(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", AUDIO_MUTE, parms);
        } else {
            try {
                final int mute = StringUtils.isEmpty(parms) ? 0 : Integer.parseInt(parms);
                if (mute == 0) {
                    callback.stateChanged(SimpleIpConstants.CHANNEL_AUDIOMUTE, OnOffType.OFF);
                } else if (mute == 1) {
                    callback.stateChanged(SimpleIpConstants.CHANNEL_AUDIOMUTE, OnOffType.ON);
                } else {
                    logger.warn("Unknown {} response: {}", AUDIO_MUTE, parms);
                }
            } catch (NumberFormatException e) {
                logger.warn("Unparsable {} response: {}", AUDIO_MUTE, parms);
            }
        }
    }

    /**
     * Handles the channel command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleChannelResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", CHANNEL, parms);
            callback.stateChanged(SimpleIpConstants.CHANNEL_CHANNEL, new StringType(""));
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else if (RSP_NOSUCHTHING.equals(parms)) {
            logger.debug("{} command invalid: {}", CHANNEL, parms);
            callback.stateChanged(SimpleIpConstants.CHANNEL_CHANNEL, new StringType(""));
        } else {
            logger.warn("Unknown {} response: {}", CHANNEL, parms);
        }
    }

    /**
     * Handles the channel notification/query response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleChannelNotification(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", CHANNEL, parms);
            callback.stateChanged(SimpleIpConstants.CHANNEL_CHANNEL, new StringType(""));
        } else {
            try {
                final int idx = parms.indexOf('.');
                if (idx >= 0) {
                    final String preS = StringUtils.trimToNull(StringUtils.stripStart(parms.substring(0, idx), "0"));
                    final String postS = StringUtils.trimToNull(StringUtils.stripEnd(parms.substring(idx + 1), "0"));
                    final int pre = preS == null ? 0 : Integer.parseInt(preS);
                    final int post = postS == null ? 0 : Integer.parseInt(postS);
                    callback.stateChanged(SimpleIpConstants.CHANNEL_CHANNEL, new StringType(pre + "." + post));
                } else {
                    logger.warn("Unparsable {} response: {}", CHANNEL, parms);
                }
            } catch (NumberFormatException e) {
                logger.warn("Unparsable {} response: {}", CHANNEL, parms);
            }
        }
    }

    /**
     * Handles the triplet channel command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleTripletChannelResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", TRIPLET_CHANNEL, parms);
            callback.stateChanged(SimpleIpConstants.CHANNEL_TRIPLETCHANNEL, new StringType(""));
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else if (RSP_NOSUCHTHING.equals(parms)) {
            logger.debug("{} command invalid: {}", TRIPLET_CHANNEL, parms);
            callback.stateChanged(SimpleIpConstants.CHANNEL_TRIPLETCHANNEL, new StringType(""));
        } else {
            logger.warn("Unknown {} response: {}", TRIPLET_CHANNEL, parms);
        }
    }

    /**
     * Handles the triplet channel command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleTripletChannelNotification(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            callback.stateChanged(SimpleIpConstants.CHANNEL_TRIPLETCHANNEL, new StringType(""));
        } else {
            if (parms.length() >= 12) {
                try {
                    final String firstS = StringUtils.trimToNull(parms.substring(0, 4));
                    final String secondS = StringUtils.trimToNull(parms.substring(4, 8));
                    final String thirdS = StringUtils.trimToNull(StringUtils.stripEnd(parms.substring(9, 13), "#"));
                    final int first = firstS == null ? 0 : Integer.parseInt(firstS, 16);
                    final int second = secondS == null ? 0 : Integer.parseInt(secondS, 16);
                    final int third = thirdS == null ? 0 : Integer.parseInt(thirdS, 16);

                    callback.stateChanged(SimpleIpConstants.CHANNEL_TRIPLETCHANNEL,
                            new StringType(first + "." + second + "." + third));

                } catch (NumberFormatException e) {
                    logger.warn("Unparsable triplet channel response: {}", parms);
                }
            } else {
                logger.warn("Unparsable triplet channel response: {}", parms);
            }
        }
    }

    /**
     * Handles the input command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleInputSourceResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", INPUT_SOURCE, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else if (RSP_NOSUCHTHING.equals(parms)) {
            logger.warn("{} response is no such input: {}", INPUT_SOURCE, parms);
        } else {
            logger.warn("Unknown {} response: {}", INPUT_SOURCE, parms);
        }
    }

    /**
     * Handles the input source command response/notification.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleInputSourceNotification(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", INPUT_SOURCE, parms);
            callback.stateChanged(SimpleIpConstants.CHANNEL_INPUTSOURCE, new StringType(""));
        } else {
            final int del = parms.indexOf('#');
            if (del >= 0) {
                callback.stateChanged(SimpleIpConstants.CHANNEL_INPUTSOURCE, new StringType(parms.substring(0, del)));
            } else {
                callback.stateChanged(SimpleIpConstants.CHANNEL_INPUTSOURCE, new StringType(parms));
            }

            refreshChannel();
            refreshTripletChannel();
        }
    }

    /**
     * Handles the input command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleInputResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", INPUT, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else if (RSP_NOSUCHTHING.equals(parms)) {
            logger.warn("{} response is no such input: {}", INPUT, parms);
        } else {
            logger.warn("Unknown {} response: {}", INPUT, parms);
        }
    }

    /**
     * Handles the input notification/inquiry response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleInputNotification(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", INPUT, parms);
        } else {
            if (parms.length() >= 13) {
                try {
                    final String inputS = StringUtils.trimToNull(parms.substring(0, 12));
                    final String portS = StringUtils.trimToNull(parms.substring(13));
                    final int inputType = inputS == null ? 0 : Integer.parseInt(inputS);
                    final int portNbr = portS == null ? 0 : Integer.parseInt(portS);

                    // workaround to @NonNullByDefault and maps.get issue
                    final String inputName = INPUT_TYPES.containsKey(inputType) ? INPUT_TYPES.get(inputType) : null;
                    if (inputName == null) {
                        logger.warn("Unknown {} name for code: {}", INPUT, parms);
                    } else {
                        callback.stateChanged(SimpleIpConstants.CHANNEL_INPUT,
                                new StringType(inputName + (inputType != INPUT_TV ? portNbr : "")));

                        refreshChannel();
                        refreshTripletChannel();
                        refreshInputSource();
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Unparsable {} response: {}", INPUT, parms);
                }
            } else {
                logger.warn("Unparsable {} response: {}", INPUT, parms);
            }
        }
    }

    /**
     * Handles the scene command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleSceneResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", SCENE, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else if (RSP_NOSUCHTHING.equals(parms)) {
            logger.warn("{} response is no such input: {}", SCENE, parms);
        } else {
            logger.warn("Unknown {} response: {}", SCENE, parms);
        }
    }

    /**
     * Handles the scene notification/inquiry response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleSceneNotification(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", SCENE, parms);
        } else {
            final int del = parms.indexOf('#');
            if (del >= 0) {
                callback.stateChanged(SimpleIpConstants.CHANNEL_SCENE, new StringType(parms.substring(0, del)));
            } else {
                callback.stateChanged(SimpleIpConstants.CHANNEL_SCENE, new StringType(parms));
            }
        }
    }

    /**
     * Handles the picture mute command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handlePictureMuteResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", PICTURE_MUTE, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else {
            logger.warn("Unknown {} response: {}", PICTURE_MUTE, parms);
        }
    }

    /**
     * Handles the picture mute notification/query response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handlePictureMuteNotification(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", PICTURE_MUTE, parms);
        } else {
            try {
                final int mute = StringUtils.isEmpty(parms) ? 0 : Integer.parseInt(parms);
                if (mute == 0) {
                    callback.stateChanged(SimpleIpConstants.CHANNEL_PICTUREMUTE, OnOffType.OFF);
                } else if (mute == 1) {
                    callback.stateChanged(SimpleIpConstants.CHANNEL_PICTUREMUTE, OnOffType.ON);
                } else {
                    logger.warn("Unknown {} response: {}", PICTURE_MUTE, parms);
                }
            } catch (NumberFormatException e) {
                logger.warn("Unparsable {} response: {}", PICTURE_MUTE, parms);
            }
        }
    }

    /**
     * Handles the toggle picture mute command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleTogglePictureMuteResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", TOGGLE_PICTURE_MUTE, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else {
            logger.warn("Unknown {} response: {}", TOGGLE_PICTURE_MUTE, parms);
        }
    }

    /**
     * Handles the PIP command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handlePictureInPictureResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", PICTURE_IN_PICTURE, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else {
            logger.warn("Unknown {} response: {}", PICTURE_IN_PICTURE, parms);
        }
    }

    /**
     * Handles the PIP notification/query response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handlePictureInPictureNotification(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", PICTURE_IN_PICTURE, parms);
        } else {
            try {
                final int enabled = StringUtils.isEmpty(parms) ? 0 : Integer.parseInt(parms);
                if (enabled == 0) {
                    callback.stateChanged(SimpleIpConstants.CHANNEL_PICTUREINPICTURE, OnOffType.OFF);
                } else if (enabled == 1) {
                    callback.stateChanged(SimpleIpConstants.CHANNEL_PICTUREINPICTURE, OnOffType.ON);
                } else {
                    logger.warn("Unknown {} response: {}", PICTURE_IN_PICTURE, parms);
                }
            } catch (NumberFormatException e) {
                logger.warn("Unparsable {} response: {}", PICTURE_IN_PICTURE, parms);
            }
        }
    }

    /**
     * Handles the toggle PIP command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleTogglePictureInPictureResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", TOGGLE_PICTURE_IN_PICTURE, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else {
            logger.warn("Unknown {} response: {}", TOGGLE_PICTURE_IN_PICTURE, parms);
        }
    }

    /**
     * Handles the toggle PIP position command response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleTogglePIPPosition(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", TOGGLE_PIP_POSITION, parms);
        } else if (RSP_SUCCESS.equals(parms)) {
            // success!
        } else {
            logger.warn("Unknown {} response: {}", TOGGLE_PIP_POSITION, parms);
        }
    }

    /**
     * Handles the broadcast query response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleBroadcastAddressResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", BROADCAST_ADDRESS, parms);
        } else {
            final int del = parms.indexOf('#');
            if (del >= 0) {
                callback.setProperty(SimpleIpConstants.PROP_BROADCASTADDRESS, parms.substring(0, del));
            } else {
                callback.setProperty(SimpleIpConstants.PROP_BROADCASTADDRESS, parms);
            }
        }
    }

    /**
     * Handles the mac address query response.
     *
     * @param parms a non-null, possibly empty response
     */
    private void handleMacAddressResponse(String parms) {
        Objects.requireNonNull(parms, "parms cannot be null");

        if (RSP_ERROR.equals(parms)) {
            logger.debug("{} command failed: {}", MACADDRESS, parms);
        } else {
            final StringBuffer sb = new StringBuffer();

            final int max = parms.length();
            for (int x = 0; x < max; x++) {
                char myChar = parms.charAt(x);
                if (myChar == '#') {
                    break;
                }
                if (x > 0 && x % 2 == 0) {
                    sb.append(':');
                }
                sb.append(myChar);
            }
            callback.setProperty(SimpleIpConstants.PROP_MACADDRESS, sb.toString());
        }
    }

    @Override
    public boolean responseReceived(String response) {
        Objects.requireNonNull(response, "response cannot be null");

        if (StringUtils.isEmpty(response)) {
            return true;
        }

        final Matcher m = RSP_NOTIFICATION.matcher(response);
        if (m.matches()) {
            handleNotification(m, response);
            return true;
        }

        logger.warn("Unparsable notification: {}", response);
        return true;
    }

    @Override
    public void responseException(Exception e) {
        Objects.requireNonNull(e, "e cannot be null");

        logger.debug("Exception occurred reading from the socket: {}", e.getMessage(), e);
        callback.statusChanged(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Exception occurred reading from the socket: " + e.getMessage());
    }

    /**
     * Helper method to simply return the default commands for Simple IP control.
     *
     * @return a non-null, non-empty list of commands
     */
    private List<String> getDefaultCommands() {
        final List<String> cmds = new ArrayList<String>();
        // cmds.add("Power-Off=0"); - doesn't seem to be power-off
        cmds.add("Input=1");
        cmds.add("Guide=2");
        cmds.add("EPG=3");
        cmds.add("Favorites=4");
        cmds.add("Display=5");
        cmds.add("Home=6");
        cmds.add("Options=7");
        cmds.add("Return=8");
        cmds.add("Up=9");
        cmds.add("Down=10");
        cmds.add("Right=11");
        cmds.add("Left=12");
        cmds.add("Confirm=13");
        cmds.add("Red=14");
        cmds.add("Green=15");
        cmds.add("Yellow=16");
        cmds.add("Blue=17");
        cmds.add("Num1=18");
        cmds.add("Num2=19");
        cmds.add("Num3=20");
        cmds.add("Num4=21");
        cmds.add("Num5=22");
        cmds.add("Num6=23");
        cmds.add("Num7=24");
        cmds.add("Num8=25");
        cmds.add("Num9=26");
        cmds.add("Num0=27");
        cmds.add("Num11=28");
        cmds.add("Num12=29");
        cmds.add("Volume-Up=30");
        cmds.add("Volume-Down=31");
        cmds.add("Mute=32");
        cmds.add("Channel-Up=33");
        cmds.add("Channel-Down=34");
        cmds.add("Subtitle=35");
        cmds.add("Closed-Caption=36");
        cmds.add("Enter=37");
        cmds.add("DOT=38");
        cmds.add("Analog=39");
        cmds.add("Teletext=40");
        cmds.add("Exit=41");
        cmds.add("Analog2=42");
        cmds.add("*AD=43");
        cmds.add("Digital=44");
        cmds.add("Analog?=45");
        cmds.add("BS=46");
        cmds.add("CS=47");
        cmds.add("BS/CS=48");
        cmds.add("Ddata=49");
        cmds.add("Pic-Off=50");
        cmds.add("Tv_Radio=51");
        cmds.add("Theater=52");
        cmds.add("SEN=53");
        cmds.add("Internet-Widgets=54");
        cmds.add("Internet-Video=55");
        cmds.add("Netflix=56");
        cmds.add("Scene-Select=57");
        cmds.add("Model3D=58");
        cmds.add("iManual=59");
        cmds.add("Audio=60");
        cmds.add("Wide=61");
        cmds.add("Jump=62");
        cmds.add("PAP=63");
        cmds.add("MyEPG=64");
        cmds.add("Program-Description=65");
        cmds.add("Write-Chapter=66");
        cmds.add("TrackID=67");
        cmds.add("Ten-Key=68");
        cmds.add("AppliCast=69");
        cmds.add("acTVila=70");
        cmds.add("Delete-Video=71");
        cmds.add("Photo-Frame=72");
        cmds.add("TV-Pause=73");
        cmds.add("Key-Pad=74");
        cmds.add("Media=75");
        cmds.add("Sync-Menu=76");
        cmds.add("Forward=77");
        cmds.add("Play=78");
        cmds.add("Rewind=79");
        cmds.add("Prev=80");
        cmds.add("Stop=81");
        cmds.add("Next=82");
        cmds.add("Rec=83");
        cmds.add("Pause=84");
        cmds.add("Eject=85");
        cmds.add("Flash-Plus=86");
        cmds.add("Flash-Minus=87");
        cmds.add("Top-Menus=88");
        cmds.add("Popup-Menu=89");
        cmds.add("Rakuraku-Start=90");
        cmds.add("One-Touch-Time-Rec=91");
        cmds.add("One-Touch-View=92");
        cmds.add("One-Touch-Rec=93");
        cmds.add("One-Touch-Stop=94");
        cmds.add("DUX=95");
        cmds.add("Football-Mode=96");
        cmds.add("Social=97");
        cmds.add("Power=98");
        cmds.add("Power-On=103");
        cmds.add("Sleep=104");
        cmds.add("Sleep-Timer=105");
        cmds.add("Composite1=107");
        cmds.add("Video2=108");
        cmds.add("Picture-Mode=110");
        cmds.add("Demo-Surround=121");
        cmds.add("Hdmi1=124");
        cmds.add("Hdmi2=125");
        cmds.add("Hdmi3=126");
        cmds.add("Hdmi4=127");
        cmds.add("Action-Menu=129");
        cmds.add("Help=130");
        return cmds;
    }

    @Override
    public void close() throws IOException {
        listeningSession.removeListener(this);
        listeningSession.disconnect();
    }
}
