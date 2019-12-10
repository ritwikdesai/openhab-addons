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
package org.openhab.binding.sony.internal.scalarweb;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.transform.TransformationService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.sony.internal.AbstractThingHandler;
import org.openhab.binding.sony.internal.AccessResult;
import org.openhab.binding.sony.internal.SonyBindingConstants;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.providers.SonyDefinitionProvider;
import org.openhab.binding.sony.internal.providers.SonyProviderListener;
import org.openhab.binding.sony.internal.providers.SonyDynamicStateProvider;
import org.openhab.binding.sony.internal.providers.models.SonyDeviceCapability;
import org.openhab.binding.sony.internal.providers.models.SonyServiceCapability;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.openhab.binding.sony.internal.scalarweb.protocols.ScalarWebLoginProtocol;
import org.openhab.binding.sony.internal.scalarweb.protocols.ScalarWebProtocol;
import org.openhab.binding.sony.internal.scalarweb.protocols.ScalarWebProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * The thing handler for a Sony Webscalar device. This is the entry point provides a full two interaction between
 * openhab and the webscalar system.
 *
 * @author Tim Roberts - Initial contribution
 *
 */
@NonNullByDefault
public class ScalarWebHandler extends AbstractThingHandler<ScalarWebConfig> {

    /** The logger */
    private Logger logger = LoggerFactory.getLogger(ScalarWebHandler.class);

    /** The tracker */
    private final ScalarWebChannelTracker tracker = new ScalarWebChannelTracker();

    /** The protocol handler being used - will be null if not initialized. */
    private final AtomicReference<@Nullable ScalarWebClient> scalarClient = new AtomicReference<>(null);

    /** The protocol handler being used - will be null if not initialized. */
    private final AtomicReference<@Nullable ScalarWebProtocolFactory<ThingCallback<String>>> protocolFactory = new AtomicReference<>(
            null);

    /** The thing callback */
    private final ThingCallback<String> callback;

    /** The transformation service to use */
    private final @Nullable TransformationService transformationService;

    /** The websocket client to use */
    private final WebSocketClient webSocketClient;

    /** The definition provider to use */
    private final SonyDefinitionProvider sonyDefinitionProvider;

    /** The dynamic state provider to use */
    private final SonyDynamicStateProvider sonyDynamicStateProvider;

    /** The channel mapper to use */
    private final ScalarWebChannelMapper mapper;

    /** The definition listener */
    private final DefinitionListener definitionListener = new DefinitionListener();

    /**
     * Constructs the web handler
     *
     * @param thing                    a non-null thing
     * @param transformationService    a possibly null transformation service
     * @param webSocketClient          a non-null websocket client
     * @param sonyDefinitionProvider   a non-null definition provider
     * @param sonyDynamicStateProvider a non-null dynamic state provider
     */
    public ScalarWebHandler(Thing thing, @Nullable TransformationService transformationService,
            WebSocketClient webSocketClient, SonyDefinitionProvider sonyDefinitionProvider,
            SonyDynamicStateProvider sonyDynamicStateProvider) {
        super(thing, ScalarWebConfig.class);

        Objects.requireNonNull(thing, "thing cannot be null");
        Objects.requireNonNull(webSocketClient, "webSocketClient cannot be null");
        Objects.requireNonNull(sonyDefinitionProvider, "sonyDefinitionProvider cannot be null");
        Objects.requireNonNull(sonyDynamicStateProvider, "sonyDynamicStateProvider cannot be null");

        this.transformationService = transformationService;
        this.webSocketClient = webSocketClient;
        this.sonyDefinitionProvider = sonyDefinitionProvider;
        this.sonyDynamicStateProvider = sonyDynamicStateProvider;
        this.mapper = new ScalarWebChannelMapper(getThing());

        callback = new ThingCallback<String>() {
            @Override
            public void statusChanged(ThingStatus state, ThingStatusDetail detail, @Nullable String msg) {
                updateStatus(state, detail, msg);

            }

            @Override
            public void stateChanged(String channelId, State newState) {
                final String mappedId = mapper.getMappedChannelId(channelId);
                updateState(mappedId, newState);
            }

            @Override
            public void setProperty(String propertyName, @Nullable String propertyValue) {
                // change meaning of null propertyvalue
                // setProperty says remove - here we are ignoring
                if (propertyValue != null && StringUtils.isNotEmpty(propertyValue)) {
                    getThing().setProperty(propertyName, propertyValue);
                }

                // Update the discovered model name if found
                if (StringUtils.equals(propertyName, ScalarWebConstants.PROP_MODEL) 
                        && propertyValue != null 
                        && StringUtils.isNotEmpty(propertyValue)) {
                    final ScalarWebConfig swConfig = getSonyConfig();
                    swConfig.setDiscoveredModelName(propertyValue);

                    final Configuration config = getConfig();
                    config.setProperties(swConfig.asProperties());
                    
                    updateConfiguration(config);
                }
            }
        };

    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Objects.requireNonNull(channelUID, "channelUID cannot be null");
        Objects.requireNonNull(command, "command cannot be null");

        final Channel channel = getThing().getChannel(channelUID.getId());
        if (channel == null) {
            logger.debug("Channel for {} could not be found", channelUID);
            return;
        }
        final ScalarWebChannel scalarChannel = new ScalarWebChannel(mapper.getBaseChannelId(channelUID), channel);

        final ScalarWebProtocolFactory<ThingCallback<String>> localProtocolFactory = protocolFactory.get();
        if (localProtocolFactory == null) {
            logger.debug("Trying to handle a channel command before a protocol factory has been created");
            return;
        }

        final ScalarWebProtocol<ThingCallback<String>> protocol = localProtocolFactory
                .getProtocol(scalarChannel.getService());
        if (protocol == null) {
            logger.debug("Unknown channel service: {} for {} and command {}", scalarChannel.getService(), channelUID,
                    command);
        } else {
            if (command instanceof RefreshType) {
                protocol.refreshChannel(scalarChannel);
            } else {
                protocol.setChannel(scalarChannel, command);
            }
        }
    }

    @Override
    protected void connect() {

        final ScalarWebConfig config = getSonyConfig();

        final String scalarWebUrl = config.getDeviceAddress();
        if (scalarWebUrl == null || StringUtils.isEmpty(scalarWebUrl)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "ScalarWeb URL is missing from configuration");
            return;
        }

        logger.info("Attempting connection to Scalar Web device...");
        try {
            SonyUtil.checkInterrupt();

            final ScalarWebContext context = new ScalarWebContext(() -> getThing(), config, tracker, scheduler,
                    sonyDynamicStateProvider, webSocketClient, transformationService, mapper);

            final ScalarWebClient client = new ScalarWebClient(scalarWebUrl, context);
            scalarClient.set(client);

            final ScalarWebLoginProtocol<ThingCallback<String>> loginHandler = new ScalarWebLoginProtocol<>(client,
                    config, callback, transformationService);

            final AccessResult result = loginHandler.login();
            SonyUtil.checkInterrupt();

            if (result == AccessResult.OK) {
                final ScalarWebProtocolFactory<ThingCallback<String>> factory = new ScalarWebProtocolFactory<>(context,
                        client, callback);

                SonyUtil.checkInterrupt();

                final ThingBuilder thingBuilder = editThing();
                thingBuilder.withChannels(getChannels(factory));
                updateThing(thingBuilder.build());

                SonyUtil.checkInterrupt();

                SonyUtil.close(protocolFactory.getAndSet(factory));
                updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);

                this.scheduler.submit(() -> {
                    // Refresh the state right away
                    refreshState();

                    // after state is refreshed - write the definition
                    // (which could include dynamic state from refresh)
                    writeThingDefinition();
                    writeDeviceCapabilities(client);

                    final String modelName = getModelName();
                    if (modelName != null && StringUtils.isNotEmpty(modelName)) {
                        sonyDefinitionProvider.addListener(modelName, getThing().getThingTypeUID(), definitionListener);
                    }

                });
            } else {
                // If it's a pending access (or code not accepted), update with a configuration error
                // this prevents a reconnect (which will cancel any current registration code)
                // Note: there are other access code type errors that probably should be trapped here
                // as well - but those are the major two (probably represent 99% of the cases)
                // and we handle them separately
                if (result == AccessResult.PENDING || result == AccessResult.NOTACCEPTED) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, result.getMsg());
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, result.getMsg());
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Initialization was interrupted");
            // don't try to reconnect
        } catch (IOException | ParserConfigurationException | SAXException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error connecting to Scalar Web device (may need to turn it on manually)");
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Unhandled exception connecting to Scalar Web device (may need to turn it on manually): "
                            + e.getMessage());
        }
    }

    /**
     * Helper method to get the channels to configure
     *
     * @param factory the non-null factory to use
     * @return a non-null, possibly empty list of channels
     */
    private Channel[] getChannels(ScalarWebProtocolFactory<ThingCallback<String>> factory) {
        Objects.requireNonNull(factory, "factory cannot be null");

        final ThingUID thingUid = getThing().getUID();
        final ThingTypeUID typeUID = getThing().getThingTypeUID();
        final boolean genericThing = ScalarWebConstants.THING_TYPE_SCALAR.equals(typeUID);

        final Map<String, Channel> channels = genericThing ? new HashMap<>()
                : getThing().getChannels().stream().collect(Collectors.toMap(chl -> chl.getUID().getId(), chl -> chl));

        // Get all channel descriptors if we are generic
        // Get ONLY the app control descriptors (which are dynamic) if not
        for (ScalarWebChannelDescriptor descriptor : factory.getChannelDescriptors(!genericThing)) {
            final Channel channel = descriptor.createChannel(thingUid, mapper).build();
            final String channelId = channel.getUID().getId();
            if (channels.containsKey(channelId)) {
                logger.debug("Channel definition already exists for {}: {}", channel.getUID().getId(), descriptor);
            } else {
                logger.debug("Creating channel: {}", descriptor);
                channels.put(channelId, channel);
            }
        }
        return channels.values().toArray(new Channel[0]);
    }

    /**
     * Helper method to get a model name
     *
     * @return a possibly null model name
     */
    private @Nullable String getModelName() {
        final String modelName = getSonyConfig().getModelName();
        if (modelName != null && StringUtils.isNotEmpty(modelName) && SonyUtil.isValidModelName(modelName)) {
            return modelName;
        }

        final String thingLabel = thing.getLabel();
        return thingLabel != null && StringUtils.isNotEmpty(thingLabel) && SonyUtil.isValidModelName(thingLabel)
                ? thingLabel
                : null;
    }

    /**
     * Helper method to write out a device capability
     *
     * @param client a non-null client
     */
    private void writeDeviceCapabilities(ScalarWebClient client) {
        Objects.requireNonNull(client, "client cannot be null");

        final String modelName = getModelName();
        if (modelName == null || StringUtils.isEmpty(modelName)) {
            logger.debug("Could not write device capabilities file - model name was missing from properties");
        } else {
            final URL baseUrl = client.getDevice().getBaseUrl();

            final List<SonyServiceCapability> srvCapabilities = client.getDevice().getServices().stream()
                    .map(srv -> new SonyServiceCapability(srv.getServiceName(), srv.getVersion(),
                            srv.getTransport().getProtocolType(), srv.getMethods(), srv.getNotifications()))
                    .collect(Collectors.toList());

            sonyDefinitionProvider
                    .writeDeviceCapabilities(new SonyDeviceCapability(modelName, baseUrl, srvCapabilities));
        }
    }

    /**
     * Helper method to write thing definition from our thing
     */
    private void writeThingDefinition() {
        final String modelName = getModelName();
        if (modelName == null || StringUtils.isEmpty(modelName)) {
            logger.debug("Could not write thing type file - model name was missing from properties");
        } else {
            // Only write things that are state channels, have a valid channel type and are not
            // from the app control service (which is too dynamic - what apps are installed)
            final Predicate<Channel> chlFilter = chl -> chl.getKind() == ChannelKind.STATE
                    && chl.getChannelTypeUID() != null
                    && !StringUtils.equalsIgnoreCase(chl.getUID().getGroupId(), ScalarWebService.APPCONTROL);

            sonyDefinitionProvider.writeThing(SonyBindingConstants.SCALAR_THING_TYPE_PREFIX, ScalarWebConstants.CFG_URI,
                    modelName, getThing(), chlFilter);
        }
    }

    @Override
    protected void refreshState() {
        final ScalarWebProtocolFactory<ThingCallback<String>> protocolHandler = protocolFactory.get();
        if (protocolHandler == null) {
            logger.debug("Protocol factory wasn't set");
        } else {
            protocolHandler.refreshAllState(scheduler);
        }
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        Objects.requireNonNull(channelUID, "channelUID cannot be null");

        tracker.channelUnlinked(mapper.getBaseChannelId(channelUID));
        super.channelUnlinked(channelUID);
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        Objects.requireNonNull(channelUID, "channelUID cannot be null");
        final Channel channel = getThing().getChannel(channelUID.getId());
        if (channel == null) {
            logger.debug("channel linked called but channelUID {} could not be found", channelUID);
        } else {
            tracker.channelLinked(new ScalarWebChannel(mapper.getBaseChannelId(channelUID), channel));
        }
        super.channelLinked(channelUID);
    }

    @Override
    public void dispose() {
        super.dispose();
        sonyDefinitionProvider.removeListener(definitionListener);
        SonyUtil.close(protocolFactory.getAndSet(null));
        SonyUtil.close(scalarClient.getAndSet(null));
    }

    private class DefinitionListener implements SonyProviderListener {
        @Override
        public void thingTypeFound(ThingTypeUID uid) {
            final String modelName = getModelName();
            if (modelName != null 
                    && StringUtils.isNotEmpty(modelName)
                    && SonyUtil.isModelMatch(uid, SonyBindingConstants.SCALAR_THING_TYPE_PREFIX, modelName)) {
                changeThingType(uid, getConfig());
            }
        }
    }
}
