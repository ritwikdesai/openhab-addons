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
package org.openhab.binding.sony.internal.scalarweb.models.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class represents a preset broadcast station and is used for serialization only
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class PresetBroadcastStation {
    /** The URI to  */
    private final String uri;
    private final @Nullable String frequency;

    public PresetBroadcastStation(String uri) {
        this.uri = uri;
        this.frequency = null;
    }

    public String getUri() {
        return uri;
    }

    public @Nullable String getFrequency() {
        return frequency;
    }
}
