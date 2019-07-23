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
package org.openhab.binding.sony.internal.providers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.DynamicStateDescriptionProvider;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeRegistry;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateOption;
import org.openhab.binding.sony.internal.SonyBindingConstants;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of {@link SonyDefinitionProvider} will manage the various {@link SonySource} and provide data
 * from them
 *
 * @author Tim Roberts - Initial contribution
 */
@Component(immediate = true, service = { DynamicStateDescriptionProvider.class, SonyDynamicStateProvider.class,
        SonyDefinitionProvider.class, ThingTypeProvider.class, ChannelGroupTypeProvider.class })
@NonNullByDefault
public class SonyDefinitionProviderImpl
        implements SonyDefinitionProvider, ThingTypeProvider, SonyDynamicStateProvider, ChannelGroupTypeProvider {
    /** The logger */
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /** The list of sources (created in activate, cleared in deactivate) */
    private final List<SonySource> sources = new ArrayList<>();

    /** The list of dynamic state overrides by channel uid */
    private final Map<ChannelUID, StateDescription> stateOverride = new HashMap<>();

    /** The thing registry used to lookup things */
    private @NonNullByDefault({}) ThingRegistry thingRegistry;

    /** The thing registry used to lookup things */
    private @NonNullByDefault({}) ThingTypeRegistry thingTypeRegistry;

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable Locale locale) {
        Objects.requireNonNull(channelGroupTypeUID, "thingTypeUID cannot be null");
        if (StringUtils.equalsIgnoreCase(channelGroupTypeUID.getBindingId(), SonyBindingConstants.BINDING_ID)) {
            for (SonySource src : sources) {
                final ChannelGroupType groupType = src.getChannelGroupType(channelGroupTypeUID);
                if (groupType != null) {
                    return groupType;
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        final Map<ChannelGroupTypeUID, ChannelGroupType> groupTypes = new HashMap<>();
        for (SonySource src : sources) {
            final Collection<ChannelGroupType> localGroupTypes = src.getChannelGroupTypes();
            if (localGroupTypes != null) {
                for (ChannelGroupType gt : localGroupTypes) {
                    if (!groupTypes.containsKey(gt.getUID())) {
                        groupTypes.put(gt.getUID(), gt);
                    }
                }
            }
        }
        return groupTypes.values();
    }

    @Override
    public Collection<ThingType> getThingTypes(@Nullable Locale locale) {
        final Map<ThingTypeUID, ThingType> thingTypes = new HashMap<>();
        for (SonySource src : sources) {
            for (ThingType tt : src.getThingTypes()) {
                if (!thingTypes.containsKey(tt.getUID())) {
                    thingTypes.put(tt.getUID(), tt);
                }
            }
        }
        return thingTypes.values();
    }

    @Override
    public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        Objects.requireNonNull(thingTypeUID, "thingTypeUID cannot be null");
        if (StringUtils.equalsIgnoreCase(thingTypeUID.getBindingId(), SonyBindingConstants.BINDING_ID)) {
            for (SonySource src : sources) {
                final ThingType thingType = src.getThingType(thingTypeUID);
                if (thingType != null) {
                    return thingType;
                }
            }
        }
        return null;
    }

    @Override
    public void addStateOverride(ThingUID thingUID, String channelId, StateDescription stateDescription) {
        Objects.requireNonNull(thingUID, "thingUID cannot be null");
        Validate.notEmpty(channelId, "channelId cannot be empty");
        Objects.requireNonNull(stateDescription, "stateDescription cannot be null");

        final ChannelUID id = new ChannelUID(thingUID, channelId);
        stateOverride.put(id, stateDescription);
    }

    @Override
    public @Nullable StateDescription getStateDescription(Channel channel,
            @Nullable StateDescription originalStateDescription, @Nullable Locale locale) {
        Objects.requireNonNull(channel, "channel cannot be null");

        if (StringUtils.equalsIgnoreCase(channel.getUID().getBindingId(), SonyBindingConstants.BINDING_ID)) {
            return getStateDescription(channel.getUID().getThingUID(), channel.getUID().getId(),
                    originalStateDescription);
        }
        return originalStateDescription;
    }

    @Override
    public @Nullable StateDescription getStateDescription(ThingUID thingUID, String channelId) {
        return getStateDescription(thingUID, channelId, null);
    }

    /**
     * This is a helper method to get a state description for a specific thingUID and channel ID. This will intelligenly
     * merge the original state description (from a thing definition) with any overrides that have been added
     *
     * @param thingUID                 a non-null thing uid
     * @param channelId                a non-null, non-empty channel id
     * @param originalStateDescription a potentially null (if none) original state description
     * @return
     */
    private @Nullable StateDescription getStateDescription(ThingUID thingUID, String channelId,
            @Nullable StateDescription originalStateDescription) {
        Objects.requireNonNull(thingUID, "thingUID cannot be null");
        Validate.notEmpty(channelId, "channelID cannot be empty");

        final ThingRegistry localThingRegistry = thingRegistry;
        if (localThingRegistry != null) {
            final Thing thing = localThingRegistry.get(thingUID);
            final ChannelUID id = new ChannelUID(thingUID, channelId);

            if (thing != null) {
                BigDecimal min = null, max = null, step = null;
                String pattern = null;
                Boolean readonly = null;
                List<StateOption> options = null;

                // First use any specified override (if found)
                // Note since compiler thinks overrideDesc cannot be null
                // it flags the 'readonly' below as can't be null (which is incorrect)
                final StateDescription overrideDesc = stateOverride.get(id);
                if (overrideDesc != null) {
                    min = overrideDesc.getMinimum();
                    max = overrideDesc.getMaximum();
                    step = overrideDesc.getStep();
                    pattern = overrideDesc.getPattern();
                    readonly = overrideDesc.isReadOnly();
                    options = overrideDesc.getOptions();
                }

                // Finally use the original values
                if (originalStateDescription != null) {
                    if (min == null) {
                        min = originalStateDescription.getMinimum();
                    }

                    if (max == null) {
                        max = originalStateDescription.getMaximum();
                    }

                    if (step == null) {
                        step = originalStateDescription.getStep();
                    }

                    if (pattern == null) {
                        pattern = originalStateDescription.getPattern();
                    }

                    if (readonly == null) {
                        readonly = originalStateDescription.isReadOnly();
                    }

                    if (options == null) {
                        options = originalStateDescription.getOptions();
                    }
                }

                // If anything is specified, create a new state description and go with it
                if (min != null || max != null || step != null || pattern != null || readonly != null
                        || (options != null && options.size() > 0)) {
                    StateDescriptionFragmentBuilder bld = StateDescriptionFragmentBuilder.create();
                    if (min != null) {
                        bld = bld.withMinimum(min);
                    }
                    if (max != null) {
                        bld = bld.withMaximum(max);
                    }
                    if (step != null) {
                        bld = bld.withStep(step);
                    }
                    if (pattern != null) {
                        bld = bld.withPattern(pattern);
                    }
                    if (readonly != null) {
                        bld = bld.withReadOnly(readonly);
                    }
                    if (options.size() > 0) {
                        bld = bld.withOptions(options);
                    }
                    return bld.build().toStateDescription();
                }

            }
        }
        return originalStateDescription;
    }

    @Override
    public void writeDeviceCapabilities(SonyDeviceCapability deviceCapability) {
        Objects.requireNonNull(deviceCapability, "deviceCapability cannot be null");
        for (SonySource src : sources) {
            src.writeDeviceCapabilities(deviceCapability);
        }
    }

    @Override
    public void writeThing(String service, String configUri, String modelName, Thing thing,
            Predicate<Channel> channelFilter) {
        Validate.notEmpty(service, "service cannot be empty");
        Validate.notEmpty(configUri, "configUri cannot be empty");
        Validate.notEmpty(modelName, "modelName cannot be empty");
        Objects.requireNonNull(thing, "thing cannot be null");
        Objects.requireNonNull(channelFilter, "channelFilter cannot be null");

        final ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (!StringUtils.equalsIgnoreCase(service, thingTypeUID.getId())) {
            logger.debug("Could not write thing type - already a specific thing type (not generic)");
            return;
        }

        final ThingTypeRegistry localThingTypeRegistry = thingTypeRegistry;
        if (localThingTypeRegistry == null) {
            logger.debug("Could not write thing type - thing type registry was null");
            return;
        }

        final ThingType thingType = localThingTypeRegistry.getThingType(thingTypeUID);
        if (thingType == null) {
            logger.debug("Could not write thing type - thing type was not found in the sony sources");
            return;
        }

        // Get the state channel that have a type (with no mapping)
        // ignore null warning as the filter makes sure it's not null
        final List<SonyThingChannelDefinition> chls = thing.getChannels().stream().filter(channelFilter)
                .map(chl -> {
                    final ChannelTypeUID ctuid = chl.getChannelTypeUID();
                    return ctuid == null ? null : 
                        new SonyThingChannelDefinition(chl.getUID().getId(), null, ctuid.getId(),
                                    new SonyThingStateDefinition(getStateDescription(chl, null, null)),
                                    chl.getProperties());
                })
                .filter(chl -> chl != null)
                .collect(Collectors.toList());

        final String label = thing.getLabel() == null || StringUtils.isEmpty(thing.getLabel()) ? thingType.getLabel()
                : thing.getLabel();
        if (label == null || StringUtils.isEmpty(label)) {
            logger.debug("Could not write thing type - no label was found");
            return;
        }

        final String desc = thingType.getDescription();

        // hardcoded service groups for now
        final SonyThingDefinition ttd = new SonyThingDefinition(service, configUri, modelName, "Sony " + label,
                desc == null || StringUtils.isEmpty(desc) ? label : desc, ScalarWebService.getServiceLabels(), chls);

        for (SonySource src : sources) {
            src.writeThingDefinition(ttd);
        }
    }

    @Activate
    public void activate() {
        sources.add(new SonyFolderSource());
    }

    @Deactivate
    public void deactivate() {
        for (SonySource src : sources) {
            src.close();
        }
        sources.clear();
    }

    @Reference
    public void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    public void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference
    public void setThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = thingTypeRegistry;
    }

    public void unsetThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = null;
    }

}
