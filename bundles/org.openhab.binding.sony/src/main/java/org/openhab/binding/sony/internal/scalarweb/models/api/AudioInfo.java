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
 * The video information class used for deserialization only
 * 
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class AudioInfo {
    private @Nullable String channel;
    private @Nullable String codec;
    private @Nullable String frequency;

    public @Nullable String getChannel() {
        return channel;
    }

    public @Nullable String getCodec() {
        return codec;
    }

    public @Nullable String getFrequency() {
        return frequency;
    }

    @Override
    public String toString() {
        return "AudioInfo [channel=" + channel + ", codec=" + codec + ", frequency=" + frequency + "]";
    }
}