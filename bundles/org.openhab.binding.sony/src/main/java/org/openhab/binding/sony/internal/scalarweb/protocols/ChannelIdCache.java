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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.SonyUtil;

/**
 * A simple helper class to ensure channel IDs are unique
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ChannelIdCache {
    /** The cache of IDs used (should all be lower case to prevent duplicates based on case) */
    private final Set<String> cache = new HashSet<>();

    /**
     * Creates a unique valid channel id from the given id
     *
     * @param channelId a possibly null, possibly empty channel id
     * @return a valid unique channel id
     */
    public String getUniqueChannelId(@Nullable String channelId) {
        String validId = SonyUtil.createValidChannelUId(channelId == null ? "" : channelId);

        // If nothing left (absolutely happens) - just use "na"
        if (StringUtils.isEmpty(validId)) {
            validId = "na";
        }

        // Make sure there is no duplicates
        String id = validId.toLowerCase();

        int i = 0;
        while (cache.contains(id)) {
            id = validId + "-" + (++i);
        }

        cache.add(id);

        return id;
    }
}
