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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The class provides the source of mappings between a mapped channel id (the channel id in openHAB) and the base
 * channel id (the channel id in the protocols).
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarWebChannelMapper {
    /** Map of base channel ids to mapped channel ids */
    private final Map<String, String> baseToMap = new HashMap<>();

    /** Map of mapped channel ids to base channel ids */
    private final Map<String, String> mapToBase = new HashMap<>();

    /**
     * Creates the mapper from the specified thing
     *
     * @param thing a non-null thing to create the channel mapper from
     */
    public ScalarWebChannelMapper(Thing thing) {
        Objects.requireNonNull(thing, "thing cannot be null");

        final ThingTypeUID typeUID = thing.getThingTypeUID();
        if (!ScalarWebConstants.THING_TYPE_SCALAR.equals(typeUID)) {
            for (Channel chl : thing.getChannels()) {
                final ScalarWebChannel channel = new ScalarWebChannel(chl);
                final String baseId = channel.getProperty(ScalarWebChannel.CNL_BASECHANNELID);
                if (baseId != null && StringUtils.isNotEmpty(baseId)) {
                    final String mappedId = channel.getChannelId();
                    if (!StringUtils.equals(baseId, mappedId)) {
                        baseToMap.put(baseId, mappedId);
                        mapToBase.put(mappedId, baseId);
                    }
                }
            }
        }
    }

    /**
     * Returns the mapped channel id for the base channel id. If no mapping exists, the base channel id will be returned
     * (since there is no mapping)
     *
     * @param baseChannelId a non-null, non-empty channel id
     * @return a non-null, non-empty channel id
     */
    public String getMappedChannelId(String baseChannelId) {
        Validate.notEmpty(baseChannelId, "baseChannelId cannot be empty");
        return baseToMap.getOrDefault(baseChannelId, baseChannelId);
    }

    /**
     * Returns the mapped channel uid for the base channel uid. If no mapping exists, the base channel uid will be
     * returned (since there is no mapping)
     *
     * @param baseChannelUID a non-null channel uid
     * @return a non-null channel uid
     */
    public ChannelUID getMappedChannelId(ChannelUID baseChannelUID) {
        Objects.requireNonNull(baseChannelUID, "baseChannelUID cannot be null");
        return new ChannelUID(baseChannelUID.getThingUID(), getMappedChannelId(baseChannelUID.getId()));
    }

    /**
     * Returns the base channel id for the mapped channel id. If no mapping exists, the mapped channel id will be
     * returned (since there is no mapping)
     *
     * @param mappedChannelId a non-null, non-empty channel id
     * @return a non-null, non-empty channel id
     */
    public String getBaseChannelId(String mappedChannelId) {
        Validate.notEmpty(mappedChannelId, "mappedChannelId cannot be empty");
        return mapToBase.getOrDefault(mappedChannelId, mappedChannelId);
    }

    /**
     * Returns the base channel uid for the mapped channel uid. If no mapping exists, the mapped channel uid will be
     * returned (since there is no mapping)
     *
     * @param mappedChannelId a non-null channel uid
     * @return a non-null channel uid
     */
    public ChannelUID getBaseChannelId(ChannelUID mappedChannelId) {
        Objects.requireNonNull(mappedChannelId, "mappedChannelId cannot be null");
        return new ChannelUID(mappedChannelId.getThingUID(), getBaseChannelId(mappedChannelId.getId()));
    }
}
