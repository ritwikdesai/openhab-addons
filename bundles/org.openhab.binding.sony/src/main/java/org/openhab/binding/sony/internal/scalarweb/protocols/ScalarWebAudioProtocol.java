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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannel;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelDescriptor;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelTracker;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebContext;
import org.openhab.binding.sony.internal.scalarweb.VersionUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebEvent;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.openhab.binding.sony.internal.scalarweb.models.api.AudioMute_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.AudioMute_1_1;
import org.openhab.binding.sony.internal.scalarweb.models.api.AudioVolume_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.AudioVolume_1_1;
import org.openhab.binding.sony.internal.scalarweb.models.api.AudioVolume_1_2;
import org.openhab.binding.sony.internal.scalarweb.models.api.CurrentExternalTerminalsStatus_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.Output;
import org.openhab.binding.sony.internal.scalarweb.models.api.VolumeInformation_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.VolumeInformation_1_1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of the protocol handles the Audio service
 *
 * @author Tim Roberts - Initial contribution
 * @param <T> the generic type for the callback
 */
@NonNullByDefault
class ScalarWebAudioProtocol<T extends ThingCallback<String>> extends AbstractScalarWebProtocol<T> {

    /** The logger */
    private Logger logger = LoggerFactory.getLogger(ScalarWebAudioProtocol.class);

    // Constants used by this protocol
    private static final String CUSTOMEQUALIZER = "customequalizer";
    private static final String SPEAKERSETTING = "speakersetting";
    private static final String SOUNDSETTING = "soundsetting";
    private static final String MUTE = "mute";
    private static final String VOLUME = "volume";
    private static final String DEFAULTKEY = "main";

    /**
     * Instantiates a new scalar web audio protocol.
     *
     * @param factory  the non-null factory to use
     * @param context  the non-null context to use
     * @param service  the non-null service to use
     * @param callback the non-null callback to use
     */
    ScalarWebAudioProtocol(ScalarWebProtocolFactory<T> factory, ScalarWebContext context, ScalarWebService audioService,
            T callback) {
        super(factory, context, audioService, callback);
        enableNotifications(ScalarWebEvent.NOTIFYVOLUMEINFORMATION);
    }

    @Override
    public Collection<ScalarWebChannelDescriptor> getChannelDescriptors() {
        final List<ScalarWebChannelDescriptor> descriptors = new ArrayList<ScalarWebChannelDescriptor>();

        final Map<String, String> termTitles = new HashMap<>();
        final ScalarWebService avService = getService(ScalarWebService.AVCONTENT);
        if (avService != null) {
            try {
                for (CurrentExternalTerminalsStatus_1_0 term : avService
                        .execute(ScalarWebMethod.GETCURRENTEXTERNALTERMINALSSTATUS)
                        .asArray(CurrentExternalTerminalsStatus_1_0.class)) {
                    final String uri = term.getUri();
                    if (uri != null && StringUtils.isNotEmpty(uri)) {
                        termTitles.put(uri, term.getTitle(uri));
                    }
                }
            } catch (IOException e) {
                logger.debug("Could not retrieve {}: {}", ScalarWebMethod.GETCURRENTEXTERNALTERMINALSSTATUS,
                        e.getMessage());
            }
        }

        final ChannelIdCache cache = new ChannelIdCache();
        try {
            final String version = getService().getVersion(ScalarWebMethod.GETVOLUMEINFORMATION);
            if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
                for (VolumeInformation_1_0 vi : execute(ScalarWebMethod.GETVOLUMEINFORMATION)
                        .asArray(VolumeInformation_1_0.class)) {
                    addDescriptor(descriptors, cache, vi.getTarget(), vi.getTarget(), vi.getMinVolume(),
                            vi.getMaxVolume());
                }
            } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_1)) {
                for (VolumeInformation_1_1 vi : execute(ScalarWebMethod.GETVOLUMEINFORMATION, new Output())
                        .asArray(VolumeInformation_1_1.class)) {
                    addDescriptor(descriptors, cache, vi.getOutput(), termTitles.get(vi.getOutput()), vi.getMinVolume(),
                            vi.getMaxVolume());
                }

            } else {
                logger.debug("Unknown {} method version: {}", ScalarWebMethod.GETVOLUMEINFORMATION, version);
            }
        } catch (IOException e) {
            logger.debug("Exception getting volume information: {}", e.getMessage());
        }

        addGeneralSettingsDescriptor(descriptors, cache, ScalarWebMethod.GETSOUNDSETTINGS, SOUNDSETTING,
                "Sound Setting");

        addGeneralSettingsDescriptor(descriptors, cache, ScalarWebMethod.GETSPEAKERSETTINGS, SPEAKERSETTING,
                "Speaker Setting");

        addGeneralSettingsDescriptor(descriptors, cache, ScalarWebMethod.GETCUSTOMEQUALIZERSETTINGS, CUSTOMEQUALIZER,
                "Custom Equalizer");

        return descriptors;
    }

    private void addDescriptor(List<ScalarWebChannelDescriptor> descriptors, ChannelIdCache cache,
            @Nullable String viKey, @Nullable String outputLabel, @Nullable Integer min, @Nullable Integer max) {
        final String key = viKey == null || StringUtils.isEmpty(viKey) ? DEFAULTKEY : viKey;
        final String label = outputLabel == null ? WordUtils.capitalize(key) : outputLabel;
        final String id = cache.getUniqueChannelId(key).toLowerCase();
        final ScalarWebChannel volChannel = createChannel(VOLUME, id, key);

        descriptors.add(
                createDescriptor(volChannel, "Dimmer", "scalaraudiovolume", "Volume " + label, "Volume for " + label));

        descriptors.add(createDescriptor(createChannel(MUTE, id, key), "Switch", "scalaraudiomute", "Mute " + label,
                "Mute " + label));

        StateDescriptionFragmentBuilder bld = StateDescriptionFragmentBuilder.create().withStep(BigDecimal.ONE);
        if (min != null) {
            bld = bld.withMinimum(new BigDecimal(min));
        }
        if (max != null) {
            bld = bld.withMaximum(new BigDecimal(max));
        }

        final StateDescription sd = bld.build().toStateDescription();

        if (sd != null) {
            getContext().getStateProvider().addStateOverride(getContext().getThingUID(),
                    getContext().getMapper().getMappedChannelId(volChannel.getChannelId()), sd);
        }

    }

    @Override
    public void refreshState() {
        final ScalarWebChannelTracker tracker = getContext().getTracker();
        if (tracker.isCategoryLinked(VOLUME, MUTE)) {
            refreshVolume(getChannelTracker().getLinkedChannelsForCategory(VOLUME, MUTE));
        }

        if (tracker.isCategoryLinked(SOUNDSETTING)) {
            refreshGeneralSettings(tracker.getLinkedChannelsForCategory(SOUNDSETTING), ScalarWebMethod.GETSOUNDSETTINGS,
                    SOUNDSETTING);
        }
        if (tracker.isCategoryLinked(SPEAKERSETTING)) {
            refreshGeneralSettings(tracker.getLinkedChannelsForCategory(SPEAKERSETTING),
                    ScalarWebMethod.GETSPEAKERSETTINGS, SPEAKERSETTING);
        }
        if (tracker.isCategoryLinked(CUSTOMEQUALIZER)) {
            refreshGeneralSettings(tracker.getLinkedChannelsForCategory(CUSTOMEQUALIZER),
                    ScalarWebMethod.GETCUSTOMEQUALIZERSETTINGS, CUSTOMEQUALIZER);
        }
    }

    @Override
    public void refreshChannel(ScalarWebChannel channel) {
        Objects.requireNonNull(channel, "channel cannot be null");

        final String ctgy = channel.getCategory();
        if (StringUtils.equalsIgnoreCase(ctgy, MUTE) || StringUtils.equalsIgnoreCase(ctgy, VOLUME)) {
            refreshVolume(Collections.singletonList(channel));
        } else if (StringUtils.equalsIgnoreCase(ctgy, SOUNDSETTING)) {
            refreshGeneralSettings(Collections.singletonList(channel), ScalarWebMethod.GETSOUNDSETTINGS, SOUNDSETTING);
        } else if (StringUtils.equalsIgnoreCase(ctgy, SPEAKERSETTING)) {
            refreshGeneralSettings(Collections.singletonList(channel), ScalarWebMethod.GETSPEAKERSETTINGS,
                    SPEAKERSETTING);
        } else if (StringUtils.equalsIgnoreCase(ctgy, CUSTOMEQUALIZER)) {
            refreshGeneralSettings(Collections.singletonList(channel), ScalarWebMethod.GETCUSTOMEQUALIZERSETTINGS,
                    CUSTOMEQUALIZER);
        }
    }

    private void refreshVolume(List<ScalarWebChannel> channels) {
        try {
            final String version = getService().getVersion(ScalarWebMethod.GETVOLUMEINFORMATION);
            if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
                for (VolumeInformation_1_0 vi : handleExecute(ScalarWebMethod.GETVOLUMEINFORMATION)
                        .asArray(VolumeInformation_1_0.class)) {
                    notifyVolumeInformation(vi, channels);
                }
            } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_1)) {
                for (VolumeInformation_1_1 vi : handleExecute(ScalarWebMethod.GETVOLUMEINFORMATION, new Output())
                        .asArray(VolumeInformation_1_1.class)) {
                    notifyVolumeInformation(vi, channels);
                }
            } else {
                logger.debug("Unknown {} method version: {}", ScalarWebMethod.GETVOLUMEINFORMATION, version);
            }
        } catch (IOException e) {
            // already handled
        }
    }

    @Override
    public void setChannel(ScalarWebChannel channel, Command command) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(command, "command cannot be null");

        final String[] paths = channel.getPaths();
        if (paths.length != 1 || paths[0] == null) {
            logger.debug("Channel path invalid: {}", channel);
            return;
        }
        final String path0 = paths[0];
        final String key = StringUtils.equalsIgnoreCase(path0, DEFAULTKEY) ? "" : path0;

        switch (channel.getCategory()) {
        case MUTE:
            if (command instanceof OnOffType) {
                setMute(key, command == OnOffType.ON);
            } else {
                logger.debug("Mute command not an OnOffType: {}", command);
            }

            break;

        case VOLUME:
            if (command instanceof PercentType) {
                setVolume(key, channel, ((PercentType) command));
            } else if (command instanceof OnOffType) {
                setMute(key, command == OnOffType.ON);
            } else if (command instanceof IncreaseDecreaseType) {
                setVolume(key, ((IncreaseDecreaseType) command) == IncreaseDecreaseType.INCREASE);
            } else {
                logger.debug("Volume command not an PercentType/OnOffType/IncreaseDecreaseType: {}", command);
            }

            break;

        case SOUNDSETTING:
            setGeneralSetting(ScalarWebMethod.SETSOUNDSETTINGS, path0, channel, command);
            break;

        case SPEAKERSETTING:
            setGeneralSetting(ScalarWebMethod.SETSPEAKERSETTINGS, path0, channel, command);
            break;

        case CUSTOMEQUALIZER:
            setGeneralSetting(ScalarWebMethod.SETCUSTOMEQUALIZERSETTINGS, path0, channel, command);
            break;

        default:
            logger.debug("Unhandled channel command: {} - {}", channel, command);
            break;
        }
    }

    /**
     * Sets the mute for the specified target
     *
     * @param key   the non-null, possibly empty (default zone) key
     * @param muted the muted
     */
    private void setMute(String key, boolean muted) {
        Objects.requireNonNull(key, "key cannot be null");
        final String version = getService().getVersion(ScalarWebMethod.SETAUDIOMUTE);
        if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
            handleExecute(ScalarWebMethod.SETAUDIOMUTE, new AudioMute_1_0(key, muted));
        } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_1)) {
            handleExecute(ScalarWebMethod.SETAUDIOMUTE, new AudioMute_1_1(key, muted));
        } else {
            logger.debug("Unknown {} method version: {}", ScalarWebMethod.SETAUDIOMUTE, version);
        }
    }

    /**
     * Sets the volume for the specified target
     *
     * @param key    the non-null, possibly empty (default zone) key
     * @param volume the volume
     */
    private void setVolume(String key, ScalarWebChannel chl, PercentType cmd) {
        Objects.requireNonNull(key, "key cannot be null");

        final StateDescription sd = getContext().getStateProvider().getStateDescription(getContext().getThingUID(),
                chl.getChannelId());

        final BigDecimal min = sd == null ? BigDecimal.ZERO : sd.getMinimum();
        final BigDecimal max = sd == null ? SonyUtil.BIGDECIMAL_HUNDRED : sd.getMaximum();

        final int unscaled = SonyUtil.unscale(cmd.toBigDecimal(), min, max).setScale(0, RoundingMode.FLOOR).intValue();

        final String version = getService().getVersion(ScalarWebMethod.SETAUDIOVOLUME);
        if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
            handleExecute(ScalarWebMethod.SETAUDIOVOLUME, new AudioVolume_1_0(key, unscaled));
        } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_1)) {
            handleExecute(ScalarWebMethod.SETAUDIOVOLUME, new AudioVolume_1_1(key, unscaled));
        } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_2)) {
            handleExecute(ScalarWebMethod.SETAUDIOVOLUME, new AudioVolume_1_2(key, unscaled));
        } else {
            logger.debug("Unknown {} method version: {}", ScalarWebMethod.SETAUDIOVOLUME, version);
        }
    }

    /**
     * Adjust volume up or down on the specified target
     *
     * @param key the non-null, possibly empty (default zone) key
     * @param up  true to turn volume up by 1, false to turn volume down by 1
     */
    private void setVolume(String key, boolean up) {
        Objects.requireNonNull(key, "key cannot be null");
        final String adj = up ? "+1" : "-1";
        final String version = getService().getVersion(ScalarWebMethod.SETAUDIOVOLUME);
        if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
            handleExecute(ScalarWebMethod.SETAUDIOVOLUME, new AudioVolume_1_0(key, adj));
        } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_1)) {
            handleExecute(ScalarWebMethod.SETAUDIOVOLUME, new AudioVolume_1_1(key, adj));
        } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_2)) {
            handleExecute(ScalarWebMethod.SETAUDIOVOLUME, new AudioVolume_1_2(key, adj));
        } else {
            logger.debug("Unknown {} method version: {}", ScalarWebMethod.SETAUDIOVOLUME, version);
        }
    }

    @Override
    protected void eventReceived(ScalarWebEvent event) throws IOException {
        switch (event.getMethod()) {
        case ScalarWebEvent.NOTIFYVOLUMEINFORMATION:
            final String version = getVersion(ScalarWebMethod.GETVOLUMEINFORMATION);
            final List<ScalarWebChannel> channels = getChannelTracker().getLinkedChannelsForCategory(VOLUME, MUTE);

            if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
                final VolumeInformation_1_0 vi = event.as(VolumeInformation_1_0.class);
                notifyVolumeInformation(vi, channels);
            } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_1)) {
                final VolumeInformation_1_1 vi = event.as(VolumeInformation_1_1.class);
                notifyVolumeInformation(vi, channels);
            } else {
                logger.debug("Unknown {} method version: {}", ScalarWebEvent.NOTIFYVOLUMEINFORMATION, version);
            }

            break;

        default:
            logger.debug("Unhandled event received: {}", event);
            break;
        }
    }

    private void notifyVolumeInformation(VolumeInformation_1_0 vi, List<ScalarWebChannel> channels) {
        for (ScalarWebChannel chnl : channels) {
            final String viKey = vi.getTarget();
            final String key = viKey == null || StringUtils.isEmpty(viKey) ? DEFAULTKEY : viKey;
            if (StringUtils.equalsIgnoreCase(key, chnl.getPathPart(0))) {
                final String cid = chnl.getChannelId();
                switch (chnl.getCategory()) {
                case VOLUME:
                    final Integer vol = vi.getVolume();

                    final StateDescription sd = getContext().getStateProvider()
                            .getStateDescription(getContext().getThingUID(), cid);
                    final BigDecimal min = sd == null ? BigDecimal.ZERO : sd.getMinimum();
                    final BigDecimal max = sd == null ? SonyUtil.BIGDECIMAL_HUNDRED : sd.getMaximum();

                    final BigDecimal scaled = SonyUtil.scale(vol == null ? BigDecimal.ZERO : new BigDecimal(vol), min,
                            max);

                    callback.stateChanged(cid, SonyUtil.newPercentType(scaled));
                    break;

                case MUTE:
                    callback.stateChanged(cid, vi.isMute() ? OnOffType.ON : OnOffType.OFF);
                    break;

                default:
                    logger.debug("Unhandled channel category: {} - {}", chnl, chnl.getCategory());
                    break;
                }
            }
        }
    }

    private void notifyVolumeInformation(VolumeInformation_1_1 vi, List<ScalarWebChannel> channels) {
        for (ScalarWebChannel chnl : channels) {
            final String viKey = vi.getOutput();
            final String key = viKey == null || StringUtils.isEmpty(viKey) ? DEFAULTKEY : viKey;
            if (StringUtils.equalsIgnoreCase(key, chnl.getPathPart(0))) {
                final String cid = chnl.getChannelId();
                switch (chnl.getCategory()) {
                case VOLUME:
                    final Integer vol = vi.getVolume();

                    final StateDescription sd = getContext().getStateProvider()
                            .getStateDescription(getContext().getThingUID(), cid);
                    final BigDecimal min = sd == null ? BigDecimal.ZERO : sd.getMinimum();
                    final BigDecimal max = sd == null ? SonyUtil.BIGDECIMAL_HUNDRED : sd.getMaximum();

                    final BigDecimal scaled = SonyUtil
                            .scale(vol == null ? BigDecimal.ZERO : new BigDecimal(vol), min, max)
                            .setScale(0, RoundingMode.FLOOR);

                    callback.stateChanged(cid, SonyUtil.newPercentType(scaled));
                    break;

                case MUTE:
                    callback.stateChanged(cid, vi.isMute() ? OnOffType.ON : OnOffType.OFF);
                    break;

                default:
                    logger.debug("Unhandled channel category: {} - {}", chnl, chnl.getCategory());
                    break;
                }
            }
        }

    }
}
