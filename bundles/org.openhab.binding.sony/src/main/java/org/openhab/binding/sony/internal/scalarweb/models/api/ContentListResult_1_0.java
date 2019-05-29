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
 * This class represents a content list result and is used for deserialization only
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ContentListResult_1_0 {

    /** The channel name */
    private @Nullable String channelName;

    /** The direct remote number */
    private @Nullable Integer directRemoteNum;

    /** The content display number */
    private @Nullable String dispNum;

    /** The duration (in seconds) */
    private @Nullable Integer durationSec;

    /** The file size (bytes) */
    private @Nullable Integer fileSizeByte;

    /** The index. */
    private @Nullable Integer index;

    /** Whether the content has already been played */
    private @Nullable Boolean isAlreadyPlayed;

    /** Whether the content is protected */
    private @Nullable Boolean isProtected;

    /** The original display number. */
    private @Nullable String originalDispNum;

    /** The program media type */
    private @Nullable String programMediaType;

    /** The program number */
    private @Nullable Integer programNum;

    /** The start date time */
    private @Nullable String startDateTime;

    /** The content title */
    private @Nullable String title;

    /** The triplet channel number */
    private @Nullable String tripletStr;

    /** The content uri */
    private @Nullable String uri;

    public @Nullable String getChannelName() {
        return channelName;
    }

    public @Nullable Integer getDirectRemoteNum() {
        return directRemoteNum;
    }

    public @Nullable String getDispNum() {
        return dispNum;
    }

    public @Nullable Integer getDurationSec() {
        return durationSec;
    }

    public @Nullable Integer getFileSizeByte() {
        return fileSizeByte;
    }

    public @Nullable Integer getIndex() {
        return index;
    }

    public @Nullable Boolean isAlreadyPlayed() {
        return isAlreadyPlayed;
    }

    public @Nullable Boolean isProtected() {
        return isProtected;
    }

    public @Nullable String getOriginalDispNum() {
        return originalDispNum;
    }

    public @Nullable String getProgramMediaType() {
        return programMediaType;
    }

    public @Nullable Integer getProgramNum() {
        return programNum;
    }

    public @Nullable String getStartDateTime() {
        return startDateTime;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public @Nullable String getTripletStr() {
        return tripletStr;
    }

    public @Nullable String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return "ContentListResult_1_0 [channelName=" + channelName + ", directRemoteNum=" + directRemoteNum
                + ", dispNum=" + dispNum + ", durationSec=" + durationSec + ", fileSizeByte=" + fileSizeByte
                + ", index=" + index + ", isAlreadyPlayed=" + isAlreadyPlayed + ", isProtected=" + isProtected
                + ", originalDispNum=" + originalDispNum + ", programMediaType=" + programMediaType + ", programNum="
                + programNum + ", startDateTime=" + startDateTime + ", title=" + title + ", tripletStr=" + tripletStr
                + ", uri=" + uri + "]";
    }

}
