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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateOption;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannel;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelDescriptor;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelTracker;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebContext;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebError;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebEvent;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.openhab.binding.sony.internal.scalarweb.models.api.GeneralSetting;
import org.openhab.binding.sony.internal.scalarweb.models.api.GeneralSettingsCandidate;
import org.openhab.binding.sony.internal.scalarweb.models.api.GeneralSettingsRequest;
import org.openhab.binding.sony.internal.scalarweb.models.api.GeneralSettings_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.Notification;
import org.openhab.binding.sony.internal.scalarweb.models.api.Notifications;
import org.openhab.binding.sony.internal.scalarweb.models.api.Target;
import org.openhab.binding.sony.internal.transports.SonyTransportListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the base of all scalar web protocols
 *
 * @author Tim Roberts - Initial contribution
 * @param <T> the generic type for the callback
 */
@NonNullByDefault
public abstract class AbstractScalarWebProtocol<T extends ThingCallback<String>> implements ScalarWebProtocol<T> {

    /** The logger */
    private Logger logger = LoggerFactory.getLogger(AbstractScalarWebProtocol.class);

    // the property key to the setting type (boolean, number, etc)
    private static final String PROP_SETTINGTYPE = "settingType";

    // the property key to the device ui setting (slider, etc)
    private static final String PROP_DEVICEUI = "deviceUi";

    // following property only valid on device UI slider items
    // we need to save curr value if an increase/decrease type comes in
    private static final String PROP_CURRVALUE = "currValue";

    // for sound setting properties - we don't have a mute so we simulate
    // it by saving the current value when switching to "OFF"
    // and then restoring when "ON"
    private static final String PROP_OFFVALUE = "offValue";

    /** The context to use */
    private final ScalarWebContext context;

    /** The specific service for the protocol */
    protected final ScalarWebService service;

    /** The callback to use */
    protected final T callback;

    /** The factory used to for protocols */
    private final ScalarWebProtocolFactory<T> factory;

    /** The listener for transport events */
    private Listener listener = new Listener();

    /**
     * Instantiates a new abstract scalar web protocol.
     *
     * @param factory  the non-null factory to use
     * @param context  the non-null context to use
     * @param service  the non-null web service to use
     * @param callback the non-null callback to use
     */
    protected AbstractScalarWebProtocol(ScalarWebProtocolFactory<T> factory, ScalarWebContext context,
            ScalarWebService service, T callback) {
        Objects.requireNonNull(factory, "factory cannot be null");
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(service, "audioService cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");

        this.factory = factory;
        this.context = context;
        this.service = service;
        this.callback = callback;
    }

    /**
     * Helper method to enable a list of notifications
     * 
     * @param notificationEvents the list of notifications to enable
     */
    protected void enableNotifications(String... notificationEvents) {
        if (service.hasMethod(ScalarWebMethod.SWITCHNOTIFICATIONS)) {
            try {
                final Notifications notifications = execute(ScalarWebMethod.SWITCHNOTIFICATIONS, new Notifications())
                        .as(Notifications.class);

                final Set<String> registered = new HashSet<>(Arrays.asList(notificationEvents));

                final List<Notification> newEnabled = new ArrayList<>(notifications.getEnabled());
                final List<Notification> newDisabled = new ArrayList<>(notifications.getDisabled());
                for (Iterator<Notification> iter = newDisabled.listIterator(); iter.hasNext();) {
                    final Notification not = iter.next();
                    final String mthName = not.getName();

                    if (mthName != null && registered.contains(mthName)) {
                        newEnabled.add(not);
                        iter.remove();
                    }
                }

                if (newEnabled.size() > 0) {
                    this.service.getTransport().addListener(listener);
                    execute(ScalarWebMethod.SWITCHNOTIFICATIONS, new Notifications(newEnabled, newDisabled));
                }
            } catch (IOException e) {
                logger.debug("switchNotifications doesn't exist - ignoring event processing");
            }
        }
    }

    /**
     * Default implementation for the eventReceived and does nothing
     * 
     * @param event the event
     * @throws IOException never thrown by the default implementation
     */
    protected void eventReceived(ScalarWebEvent event) throws IOException {
        // do nothing
    }

    /**
     * Returns the {@link ScalarWebService} for the give service name
     * 
     * @param serviceName a non-null, non-empty service name
     * @return a {@link ScalarWebService} or null if not found
     */
    protected @Nullable ScalarWebService getService(String serviceName) {
        Validate.notEmpty(serviceName, "serviceName cannot be empty");
        if (StringUtils.equals(serviceName, service.getServiceName())) {
            return service;
        }

        final ScalarWebProtocol<T> protocol = factory.getProtocol(serviceName);
        return protocol == null ? null : protocol.getService();
    }

    /**
     * Returns the protocol factory used by this protocol
     * 
     * @return a non-null {@link ScalarWebProtocolFactory}
     */
    protected ScalarWebProtocolFactory<T> getFactory() {
        return factory;
    }

    /**
     * Returns the context used for the protocol
     * 
     * @return a non-null {@link ScalarWebContext}
     */
    protected ScalarWebContext getContext() {
        return context;
    }

    /**
     * Returns the service related to this protocol
     *
     * @return the non-null service
     */
    @Override
    public ScalarWebService getService() {
        return service;
    }

    /**
     * Execute the given method name with the specified parameters
     *
     * @param mthd     a non-null non-empty method
     * @param getParms a non-null get parameters method
     * @return the scalar web result
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected ScalarWebResult execute(String mthd, GetParms getParms) throws IOException {
        Validate.notEmpty(mthd, "mthd cannot be empty");
        Objects.requireNonNull(getParms, "getParms cannot be empty");
        final String version = getService().getVersion(mthd);
        if (version == null || StringUtils.isEmpty(version)) {
            logger.debug("Can't find a version for method {} - ignoring", mthd);
            return ScalarWebResult.createNotImplemented(mthd);
        }
        final Object parms = getParms.getParms(version);
        // if (parms == null) {
        // logger.debug("Unhandled version {} for method {} - ignoring", version, mthd);
        // return ScalarWebResult.createNotImplemented(mthd);
        // }
        return execute(mthd, parms == null ? new Object[0] : parms);
    }

    /**
     * Execute the given method name with the specified parameters
     *
     * @param mthd  a non-null non-empty method
     * @param parms the parameters to use
     * @return the scalar web result
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected ScalarWebResult execute(String mthd, Object... parms) throws IOException {
        Validate.notEmpty(mthd, "mthd cannot be empty");
        final ScalarWebResult result = handleExecute(mthd, parms);
        if (result.isError()) {
            throw result.getHttpResponse().createException();
        }

        return result;
    }

    /**
     * Handles the execution of a method with parameters
     *
     * @param mthd     a non-null non-empty method
     * @param getParms a non-null get parameters method
     * @return a non-null result
     */
    protected ScalarWebResult handleExecute(String mthd, GetParms getParms) {
        Validate.notEmpty(mthd, "mthd cannot be empty");
        Objects.requireNonNull(getParms, "getParms cannot be empty");

        final String version = getService().getVersion(mthd);
        if (version == null || StringUtils.isEmpty(version)) {
            logger.debug("Can't find a version for method {} - ignoring", mthd);
            return ScalarWebResult.createNotImplemented(mthd);
        }
        final Object parms = getParms.getParms(version);
        // if (parms == null) {
        // logger.debug("Unhandled version {} for method {} - ignoring", version, mthd);
        // return ScalarWebResult.createNotImplemented(mthd);
        // }
        return handleExecute(mthd, parms == null ? new Object[0] : parms);
    }

    /**
     * Handles the execution of a method with parameters
     *
     * @param mthd  the method name to execute
     * @param parms the parameters to use
     * @return the scalar web result
     */
    protected ScalarWebResult handleExecute(String mthd, Object... parms) {
        Validate.notEmpty(mthd, "mthd cannot be empty");
        final ScalarWebResult result = service.execute(mthd, parms);
        if (result.isError()) {
            switch (result.getDeviceErrorCode()) {
                case ScalarWebError.NOTIMPLEMENTED:
                    logger.debug("Method is not implemented on service {} - {}({}): {}", service.getServiceName(), mthd,
                            StringUtils.join(parms, ','), result.getDeviceErrorDesc());
                    break;

                case ScalarWebError.ILLEGALARGUMENT:
                    logger.debug("Method arguments are incorrect on service {} - {}({}): {}", service.getServiceName(),
                            mthd, StringUtils.join(parms, ','), result.getDeviceErrorDesc());
                    break;

                case ScalarWebError.ILLEGALSTATE:
                    logger.debug("Method state is incorrect on service {} - {}({}): {}", service.getServiceName(), mthd,
                            StringUtils.join(parms, ','), result.getDeviceErrorDesc());
                    break;

                case ScalarWebError.DISPLAYISOFF:
                    logger.debug("The display is off and command cannot be executed on service {} - {}({}): {}",
                            service.getServiceName(), mthd, StringUtils.join(parms, ','), result.getDeviceErrorDesc());
                    break;

                case ScalarWebError.FAILEDTOLAUNCH:
                    logger.debug("The application failed to launch (probably display is off) {} - {}({}): {}",
                            service.getServiceName(), mthd, StringUtils.join(parms, ','), result.getDeviceErrorDesc());
                    break;

                case ScalarWebError.HTTPERROR:
                    final IOException e = result.getHttpResponse().createException();
                    logger.debug("Communication error executing method {}([]) on service {}: {}", mthd,
                            StringUtils.join(parms, ','), service.getServiceName(), e.getMessage(), e);
                    callback.statusChanged(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                    break;

                default:
                    logger.debug("Device error ({}) on service {} - {}({}): {}", result.getDeviceErrorCode(),
                            service.getServiceName(), mthd, StringUtils.join(parms, ','), result.getDeviceErrorDesc());
                    break;
            }
        }

        return result;
    }

    /**
     * Creates a scalar web channel for the given id with potentially additional
     * paths
     *
     * @param id the non-null, non-empty channel identifier
     * @return the scalar web channel
     */
    protected ScalarWebChannel createChannel(String id) {
        Validate.notEmpty(id, "id cannot be empty");
        return createChannel(id, id, new String[0]);
    }

    /**
     * Creates a scalar web channel for the given id with potentially additional
     * paths
     *
     * @param category   the non-null, non-empty channel category
     * @param id         the non-null, non-empty channel identifier
     * @param addtlPaths the potential other paths
     * @return the scalar web channel
     */
    protected ScalarWebChannel createChannel(String category, String id, String... addtlPaths) {
        Validate.notEmpty(category, "category cannot be empty");
        Validate.notEmpty(id, "id cannot be empty");
        return new ScalarWebChannel(service.getServiceName(), category, id, addtlPaths);
    }

    /**
     * Creates the channel descriptor for the given channel, item type and channel
     * type
     *
     * @param channel          the non-null channel
     * @param acceptedItemType the non-null, non-empty accepted item type
     * @param channelType      the non-null, non-empty channel type
     * @return the scalar web channel descriptor
     */
    protected ScalarWebChannelDescriptor createDescriptor(ScalarWebChannel channel, String acceptedItemType,
            String channelType) {
        Objects.requireNonNull(channel, "channel cannot be empty");
        Validate.notEmpty(acceptedItemType, "acceptedItemType cannot be empty");
        Validate.notEmpty(channelType, "channelType cannot be empty");
        return createDescriptor(channel, acceptedItemType, channelType, null, null);
    }

    /**
     * Creates the descriptor from the given parameters
     *
     * @param channel          the non-null channel
     * @param acceptedItemType the non-null, non-empty accepted item type
     * @param channelType      the non-null, non-empty channel type
     * @param label            the potentially null, potentially empty label
     * @param description      the potentially null, potentially empty description
     * @return the scalar web channel descriptor
     */
    protected ScalarWebChannelDescriptor createDescriptor(ScalarWebChannel channel, String acceptedItemType,
            String channelType, @Nullable String label, @Nullable String description) {
        Objects.requireNonNull(channel, "channel cannot be empty");
        Validate.notEmpty(acceptedItemType, "acceptedItemType cannot be empty");
        Validate.notEmpty(channelType, "channelType cannot be empty");
        return new ScalarWebChannelDescriptor(channel, acceptedItemType, channelType, label, description);
    }

    /**
     * Helper method to issue a state changed for the simple id (where category=id)
     *
     * @param id    the non-null, non-empty id
     * @param state the non-null new state
     */
    protected void stateChanged(String id, State state) {
        Validate.notEmpty(id, "id cannot be empty");
        Objects.requireNonNull(state, "state cannot be empty");
        stateChanged(id, id, state);
    }

    /**
     * Helper method to issue a state changed for the ctgy/id
     *
     * @param category the non-null, non-empty category
     * @param id       the non-null, non-empty id
     * @param state    the non-null new state
     */
    protected void stateChanged(String category, String id, State state) {
        Validate.notEmpty(category, "category cannot be empty");
        Validate.notEmpty(id, "id cannot be empty");
        Objects.requireNonNull(state, "state cannot be empty");
        callback.stateChanged(
                SonyUtil.createChannelId(service.getServiceName(), ScalarWebChannel.createChannelId(category, id)),
                state);
    }

    /**
     * Returns the channel tracker
     *
     * @return the non-null channel tracker
     */
    protected ScalarWebChannelTracker getChannelTracker() {
        return context.getTracker();
    }

    /**
     * Helper method to return a method's latest version
     *
     * @param methodName a non-null, non-empty method name
     * @return a possibly null (if not found) method's latest version
     */
    protected @Nullable String getVersion(String methodName) {
        Validate.notEmpty(methodName, "methodName cannot be empty");
        return getService().getVersion(methodName);
    }

    /**
     * Refreshs the general sttings for a list of channels
     * 
     * @param channels      a non-null, possibly empty list of {@link ScalarWebChannel}
     * @param getMethodName a non-null, non-empty method name to get settings from
     * @param ctgy          a nno-null, non-emtpy openhab category to use
     */
    protected void refreshGeneralSettings(List<ScalarWebChannel> channels, String getMethodName, String ctgy) {
        Objects.requireNonNull(channels, "channels cannot be null");
        Validate.notEmpty(getMethodName, "getMethodName cannot be empty");
        Validate.notEmpty(ctgy, "ctgy cannot be empty");

        try {
            final GeneralSettings_1_0 ss = handleExecute(getMethodName, new Target()).as(GeneralSettings_1_0.class);
            refreshGeneralSettings(ss.getSettings(), channels, ctgy);
        } catch (IOException e) {
            logger.debug("Error in refreshing general settings: {}", e.getMessage(), e);
        }
    }

    /**
     * Adds one or more general settings descriptors based on the general settings retrived from the given menthod
     * 
     * @param descriptors   a non-null, possibly empty list of descriptors
     * @param cache         a non-null channel id cache
     * @param getMethodName a non-null, non-empty get method name to retrieve settings from
     * @param ctgy          a non-null, non-empty openhab category
     * @param prefix        a non-null, non-empty prefix for descriptor names/labels
     */
    protected void addGeneralSettingsDescriptor(List<ScalarWebChannelDescriptor> descriptors, ChannelIdCache cache,
            String getMethodName, String ctgy, String prefix) {
        Objects.requireNonNull(descriptors, "descriptors cannot be null");
        Objects.requireNonNull(cache, "cache cannot be null");
        Validate.notEmpty(getMethodName, "getMethodName cannot be empty");
        Validate.notEmpty(ctgy, "ctgy cannot be empty");
        Validate.notEmpty(prefix, "prefix cannot be empty");

        try {
            final GeneralSettings_1_0 ss = execute(getMethodName, new Target()).as(GeneralSettings_1_0.class);

            for (GeneralSetting set : ss.getSettings()) {
                // Seems isAvailable is not a reliable call (many false when in fact it's true)
                // if (!set.isAvailable()) {
                // logger.debug("{} isn't available {} - ignoring", prefix, set);
                // continue;
                // }
                final String target = set.getTarget();
                if (target == null || StringUtils.isEmpty(target)) {
                    logger.debug("Target not valid for {} {} - ignoring", prefix, set);
                    continue;
                }

                final String settingType = set.getType();
                if (settingType == null || StringUtils.isEmpty(settingType)) {
                    logger.debug("Setting Type not valid for {} {} - ignoring", prefix, set);
                    continue;
                }

                String title = textLookup(set.getTitle());

                final String label = title == null || StringUtils.isEmpty(title) ? target : title;
                final String id = cache.getUniqueChannelId(target).toLowerCase();
                final List<@Nullable GeneralSettingsCandidate> candidates = set.getCandidate();

                final ScalarWebChannel channel = createChannel(ctgy, id, target);

                final String ui = set.getDeviceUIInfo();
                if (ui != null) {
                    channel.addProperty(PROP_DEVICEUI, ui);
                }
                channel.addProperty(PROP_SETTINGTYPE, settingType);

                switch (settingType) {
                    case GeneralSetting.BOOLEANTARGET:
                        descriptors.add(createDescriptor(channel, "Switch", "scalaraudiogeneralsettingswitch",
                                prefix + " " + label, prefix + " for " + label));

                        break;
                    case GeneralSetting.DOUBLETARGET:
                        if (set.isUiSlider()) {
                            descriptors.add(createDescriptor(channel, "Dimmer", "scalaraudiogeneralsettingdimmer",
                                    prefix + " " + label, prefix + " for " + label));

                        } else {
                            descriptors.add(createDescriptor(channel, "Number", "scalaraudiogeneralsettingnumber",
                                    prefix + " " + label, prefix + " for " + label));
                        }

                        if (candidates != null) {
                            final @Nullable GeneralSettingsCandidate candidate = candidates.stream()
                                    .filter(c -> c != null).findFirst().orElseGet(() -> null);
                            // ..filter(c -> c != null && c.isAvailable()).findFirst().orElseGet(() ->
                            // null);

                            if (candidate != null) {
                                final Double min = candidate.getMin(), max = candidate.getMax(),
                                        step = candidate.getStep();
                                if (min != null || max != null || step != null) {
                                    final List<StateOption> options = new ArrayList<>();
                                    if (set.isUiPicker()) {
                                        double dmin = min == null || min.isInfinite() || min.isNaN() ? 0
                                                : min.doubleValue();
                                        double dmax = max == null || max.isInfinite() || max.isNaN() ? 100
                                                : max.doubleValue();
                                        double dstep = step == null || step.isInfinite() || step.isNaN() ? 1
                                                : step.doubleValue();
                                        for (double p = dmin; p <= dmax; p += (dstep == 0 ? 1 : dstep)) {
                                            final String opt = Double.toString(p);
                                            options.add(new StateOption(opt, opt));
                                        }
                                    }
                                    StateDescriptionFragmentBuilder bld = StateDescriptionFragmentBuilder.create();
                                    if (min != null) {
                                        bld = bld.withMinimum(new BigDecimal(min));
                                    }
                                    if (max != null) {
                                        bld = bld.withMaximum(new BigDecimal(max));
                                    }
                                    if (step != null) {
                                        bld = bld.withStep(new BigDecimal(step));
                                    }
                                    if (options.size() > 0) {
                                        bld = bld.withOptions(options);
                                    }

                                    final StateDescription sd = bld.build().toStateDescription();
                                    if (sd != null) {
                                        getContext().getStateProvider().addStateOverride(getContext().getThingUID(),
                                                getContext().getMapper().getMappedChannelId(channel.getChannelId()),
                                                sd);
                                    }
                                }
                            }
                        }

                        break;
                    case GeneralSetting.INTEGERTARGET:
                        if (set.isUiSlider()) {
                            descriptors.add(createDescriptor(channel, "Dimmer", "scalaraudiogeneralsettingdimmer",
                                    prefix + " " + label, prefix + " for " + label));

                        } else {
                            descriptors.add(createDescriptor(channel, "Number", "scalaraudiogeneralsettingnumber",
                                    prefix + " " + label, prefix + " for " + label));
                        }

                        if (candidates != null) {
                            final @Nullable GeneralSettingsCandidate candidate = candidates.stream()
                                    .filter(c -> c != null).findFirst().orElseGet(() -> null);
                            // ..filter(c -> c != null && c.isAvailable()).findFirst().orElseGet(() ->
                            // null);

                            if (candidate != null) {
                                final Double min = candidate.getMin(), max = candidate.getMax(),
                                        step = candidate.getStep();
                                if (min != null || max != null || step != null) {
                                    final List<StateOption> options = new ArrayList<>();
                                    if (set.isUiPicker()) {
                                        int imin = min == null ? 0 : min.intValue();
                                        int imax = max == null ? 100 : max.intValue();
                                        int istep = step == null ? 1 : step.intValue();
                                        for (int p = imin; p <= imax; p += (istep == 0 ? 1 : istep)) {
                                            final String opt = Double.toString(p);
                                            options.add(new StateOption(opt, opt));
                                        }
                                    }

                                    StateDescriptionFragmentBuilder bld = StateDescriptionFragmentBuilder.create();
                                    if (min != null) {
                                        bld = bld.withMinimum(new BigDecimal(min));
                                    }
                                    if (max != null) {
                                        bld = bld.withMaximum(new BigDecimal(max));
                                    }
                                    if (step != null) {
                                        bld = bld.withStep(new BigDecimal(step));
                                    }
                                    if (options.size() > 0) {
                                        bld = bld.withOptions(options);
                                    }

                                    final StateDescription sd = bld.build().toStateDescription();
                                    if (sd != null) {
                                        getContext().getStateProvider().addStateOverride(getContext().getThingUID(),
                                                channel.getChannelId(), sd);
                                    }
                                }
                            }
                        }

                        break;

                    case GeneralSetting.ENUMTARGET:
                        descriptors.add(createDescriptor(channel, "String", "scalaraudiogeneralsettingstring",
                                prefix + " " + label, prefix + " for " + label));

                        if (candidates != null) {
                            final List<StateOption> stateInfo = candidates.stream()
                                    // .filter(c -> c != null && c.isAvailable())
                                    .map(c -> {
                                        if (c == null) {
                                            return null;
                                        }
                                        final String stateVal = c.getValue();
                                        if (stateVal == null || StringUtils.isEmpty(stateVal)) {
                                            return null;
                                        }
                                        final String stateTitle = textLookup(c.getTitle());
                                        if (stateTitle == null || StringUtils.isEmpty(stateTitle)) {
                                            return null;
                                        }
                                        return new StateOption(stateVal, stateTitle);
                                    }).filter(c -> c != null).collect(Collectors.toList());
                            if (stateInfo.size() > 0) {
                                final StateDescription sd = StateDescriptionFragmentBuilder.create()
                                        .withOptions(stateInfo).build().toStateDescription();
                                if (sd != null) {
                                    getContext().getStateProvider().addStateOverride(getContext().getThingUID(),
                                            channel.getChannelId(), sd);
                                }
                            }
                        }

                        break;
                    default:
                        descriptors.add(createDescriptor(channel, "String", "scalaraudiogeneralsettingstring",
                                prefix + " " + label, prefix + " for " + label));
                        break;
                }
            }
        } catch (IOException e) {
            // ignore - probably not handled
        }
    }

    /**
     * Refreshs the general settings for a list of channels and their openHAB category
     * 
     * @param settings a non-null, possibly empty list of {@link GeneralSetting}
     * @param channels a non-null, possibly empty list of {@link ScalarWebChannel}
     * @param ctgy     a non-null, non-empty category
     */
    protected void refreshGeneralSettings(List<GeneralSetting> settings, List<ScalarWebChannel> channels, String ctgy) {
        Objects.requireNonNull(settings, "settings cannot be null");
        Objects.requireNonNull(channels, "channels cannot be null");
        Validate.notEmpty(ctgy, "ctgy cannot be empty");

        final Map<String, GeneralSetting> settingValues = new HashMap<>();
        for (GeneralSetting set : settings) {
            final String target = set.getTarget();
            if (target == null || StringUtils.isEmpty(target)) {
                continue;
            }
            settingValues.put(target, set);
        }

        for (ScalarWebChannel chl : channels) {
            final String target = chl.getPathPart(0);
            if (target == null) {
                continue;
            }

            final String settingType = chl.getProperty(PROP_SETTINGTYPE, GeneralSetting.STRINGTARGET);

            final GeneralSetting setting = settingValues.get(target);
            final String currentValue = setting.getCurrentValue();

            switch (settingType) {
                case GeneralSetting.BOOLEANTARGET:
                    stateChanged(ctgy, chl.getId(),
                            StringUtils.equalsIgnoreCase(currentValue, GeneralSetting.ON) ? OnOffType.ON
                                    : OnOffType.OFF);
                    break;

                case GeneralSetting.DOUBLETARGET:
                case GeneralSetting.INTEGERTARGET:
                    if (setting.isUiSlider()) {
                        final StateDescription sd = getContext().getStateProvider()
                                .getStateDescription(getContext().getThingUID(), chl.getChannelId());
                        final BigDecimal min = sd == null ? BigDecimal.ZERO : sd.getMinimum();
                        final BigDecimal max = sd == null ? SonyUtil.BIGDECIMAL_HUNDRED : sd.getMaximum();
                        try {
                            final BigDecimal currVal = new BigDecimal(currentValue == null ? "0" : currentValue);
                            final BigDecimal val = SonyUtil.scale(currVal, min, max);
                            chl.addProperty(PROP_CURRVALUE, currVal.toString());

                            if (settingType.equals(GeneralSetting.INTEGERTARGET)) {
                                stateChanged(ctgy, chl.getId(),
                                        SonyUtil.newPercentType(val.setScale(0, RoundingMode.FLOOR)));
                            } else {
                                stateChanged(ctgy, chl.getId(), SonyUtil.newPercentType(val));

                            }
                        } catch (NumberFormatException e) {
                            logger.debug("Current value {} was not a valid integer", currentValue);
                        }
                    } else {
                        stateChanged(ctgy, chl.getId(), SonyUtil.newDecimalType(currentValue));
                    }
                    break;

                default:
                    stateChanged(ctgy, chl.getId(), SonyUtil.newStringType(currentValue));
                    break;
            }
        }

    }

    /**
     * Sets a general setting. This method will take a target (method, target, channel) and execute a command against
     * it.
     * 
     * @param method a non-null, non-empty method
     * @param target a non-null, non-empty sony target id
     * @param chl    a non-null channel describing the setting
     * @param cmd    a non-null command to execute
     */
    protected void setGeneralSetting(String method, String target, ScalarWebChannel chl, Command cmd) {
        Validate.notEmpty(method, "method cannot be empty");
        Validate.notEmpty(target, "target cannot be empty");
        Objects.requireNonNull(chl, "chl cannot be null");
        Objects.requireNonNull(cmd, "cmd cannot be null");

        final String settingType = chl.getProperty(PROP_SETTINGTYPE, GeneralSetting.STRINGTARGET);
        final String deviceUi = chl.getProperty(PROP_DEVICEUI);

        switch (settingType) {
            case GeneralSetting.BOOLEANTARGET:
                if (cmd instanceof OnOffType) {
                    handleExecute(method, new GeneralSettingsRequest(target,
                            cmd == OnOffType.ON ? GeneralSetting.ON : GeneralSetting.OFF));
                } else {
                    logger.debug("{} command not an OnOffType: {}", method, cmd);
                }
                break;

            case GeneralSetting.DOUBLETARGET:
            case GeneralSetting.INTEGERTARGET:
                if (StringUtils.contains(deviceUi, GeneralSetting.SLIDER)) {
                    final StateDescription sd = getContext().getStateProvider()
                            .getStateDescription(getContext().getThingUID(), chl.getChannelId());

                    final BigDecimal sdMin = sd == null ? null : sd.getMinimum();
                    final BigDecimal sdMax = sd == null ? null : sd.getMaximum();
                    final BigDecimal min = sdMin == null ? BigDecimal.ZERO : sdMin;
                    final BigDecimal max = sdMax == null ? SonyUtil.BIGDECIMAL_HUNDRED : sdMax;

                    final String propVal = chl.getProperty(PROP_CURRVALUE, "0");
                    final String offPropVal = chl.removeProperty(PROP_OFFVALUE);

                    try {
                        final BigDecimal currVal = SonyUtil.guard(new BigDecimal(propVal), min, max);

                        BigDecimal newVal;
                        if (cmd instanceof OnOffType) {
                            if (cmd == OnOffType.OFF) {
                                chl.addProperty(PROP_OFFVALUE, propVal);
                                newVal = min;
                            } else {
                                // if no prior off value, go with min instead
                                newVal = offPropVal == null ? min
                                        : SonyUtil.guard(new BigDecimal(offPropVal), min, max);
                            }

                        } else if (cmd instanceof PercentType) {
                            newVal = SonyUtil.unscale(((PercentType) cmd).toBigDecimal(), min, max);
                        } else if (cmd instanceof IncreaseDecreaseType) {
                            newVal = SonyUtil.guard(cmd == IncreaseDecreaseType.INCREASE ? currVal.add(BigDecimal.ONE)
                                    : currVal.subtract(BigDecimal.ONE), min, max);
                        } else {
                            logger.debug("{} command not an dimmer type: {}", method, cmd);
                            return;
                        }
                        if (settingType.equals(GeneralSetting.INTEGERTARGET)) {
                            handleExecute(method,
                                    new GeneralSettingsRequest(target, Integer.toString(newVal.intValue())));
                        } else {
                            handleExecute(method,
                                    new GeneralSettingsRequest(target, Double.toString(newVal.doubleValue())));
                        }
                    } catch (NumberFormatException e) {
                        logger.debug("{} command current/off value not a valid number - either {} or {}: {}", method,
                                propVal, offPropVal, e.getMessage());
                    }
                } else {
                    if (cmd instanceof DecimalType) {
                        if (settingType.equals(GeneralSetting.INTEGERTARGET)) {
                            handleExecute(method, new GeneralSettingsRequest(target,
                                    Integer.toString(((DecimalType) cmd).intValue())));
                        } else {
                            handleExecute(method, new GeneralSettingsRequest(target,
                                    Double.toString(((DecimalType) cmd).doubleValue())));
                        }
                    } else {
                        logger.debug("{} command not an DecimalType: {}", method, cmd);
                    }
                }
                break;

            default:
                if (cmd instanceof StringType) {
                    handleExecute(method, new GeneralSettingsRequest(target, ((StringType) cmd).toString()));
                } else {
                    logger.debug("{} command not an StringType: {}", method, cmd);
                }
                break;
        }

    }

    /**
     * Helper function to lookup common sony text and translate to a better name
     * 
     * @param text a possibly null, possibly empty text string
     * @return the translated text or text if not recognized
     */
    private static @Nullable String textLookup(@Nullable String text) {
        if (StringUtils.equalsIgnoreCase("IDMR_TEXT_FOOTBALL_STRING", text)) {
            return "Football";
        }

        if (StringUtils.equalsIgnoreCase("IDMR_TEXT_NARRATION_OFF_STRING", text)) {
            return "Narration Off";
        }

        if (StringUtils.equalsIgnoreCase("IDMR_TEXT_NARRATION_ON_STRING", text)) {
            return "Narration On";
        }

        return text;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public void close() {
        service.getTransport().close();
    }

    /**
     * This class represents the listener to sony events and will forward those
     * events on to the protocol implementation if they have a method of the same
     * name as the event
     *
     * @author Tim Roberts - Initial contribution
     */
    private class Listener implements SonyTransportListener {
        @Override
        public void onEvent(ScalarWebEvent event) {
            Objects.requireNonNull(event, "event cannot be null");
            context.getScheduler().execute(() -> {
                try {
                    eventReceived(event);
                } catch (IOException e) {
                    logger.debug("IOException during event notification: {}", e.getMessage(), e);
                }
            });
        }

        @Override
        public void onError(Throwable t) {
        }
    }

    protected interface GetParms {
        @Nullable
        Object getParms(String version);
    }
}
