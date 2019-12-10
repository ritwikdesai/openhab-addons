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

import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class represents the source identifier and is used for deserialization only
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class Source {

    public static final String TV_ANALOG = "tv:analog";
    public static final String TV_ATSC = "tv:atsct";
    public static final String TV_DVBT = "tv:dvbt";
    public static final String RADIO_FM = "radio:fm";

    public static final Pattern RADIOPATTERN = Pattern.compile("(radio:fm\\?contentId=)(\\d+)");

    private @Nullable Boolean isBrowsable;

    private @Nullable Boolean isPlayable;
    private @Nullable String meta;
    private @Nullable String @Nullable [] outputs;
    private @Nullable String playAction;
    /** The source identifier */
    private @Nullable String source;
    private @Nullable String title;

    public @Nullable String getMeta() {
        return meta;
    }

    public @Nullable String @Nullable [] getOutputs() {
        return outputs;
    }

    public @Nullable String getPlayAction() {
        return playAction;
    }

    /**
     * Gets the source identifier
     *
     * @return the source identifier
     */
    public @Nullable String getSource() {
        return source;
    }

    public @Nullable String getSourcePart() {
        final String localSource = source;
        if (localSource == null) {
            return null;
        }

        final int idx = localSource.indexOf(":");
        return idx < 0 ? localSource : localSource.substring(idx + 1);
    }

    public @Nullable String getTitle() {
        return title;
    }

    public @Nullable Boolean isBrowsable() {
        return isBrowsable;
    }

    public @Nullable Boolean isPlayable() {
        return isPlayable;
    }

    public boolean isMatch(String name) {
        return StringUtils.equalsIgnoreCase(name, title) || StringUtils.equalsIgnoreCase(name, source);
    }

    @Override
    public int hashCode() {
        final String localSource = source;
        return ((localSource == null) ? 0 : localSource.hashCode());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(source, ((Source) obj).source);
    }

    @Override
    public String toString() {
        return "Source [source=" + source + ", isBrowsable=" + isBrowsable + ", isPlayable=" + isPlayable + ", meta="
                + meta + ", playAction=" + playAction + ", outputs=" + Arrays.toString(outputs) + ", title=" + title
                + "]";
    }
}
