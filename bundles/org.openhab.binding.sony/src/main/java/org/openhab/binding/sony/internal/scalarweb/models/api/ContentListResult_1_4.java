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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class represents a content list result and is used for deserialization only
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ContentListResult_1_4 {

    private @Nullable String albumName;

    private @Nullable String artist;

    /** The audio codecs */
    private @Nullable AudioInfo @Nullable [] audioInfo;

    private @Nullable Integer broadcastFreq;

    private @Nullable String broadcastFreqBand;

    /** The channel name */
    private @Nullable String channelName;

    /** The channel surfing visibility */
    private @Nullable String channelSurfingVisibility;

    /** The total chapter count */
    private @Nullable Integer chapterCount;

    private @Nullable ContentInfo content;

    private @Nullable String contentKind;

    /** The content type */
    private @Nullable String contentType;

    /** The created time */
    private @Nullable String createdTime;

    /** The direct remote number */
    private @Nullable Integer directRemoteNum;

    /** The content display number */
    private @Nullable String dispNum;

    /** The duration (in seconds) */
    private @Nullable Double durationMSec;

    /** The epg visibility */
    private @Nullable String epgVisibility;

    private @Nullable String fileNo;

    /** The file size (bytes) */
    private @Nullable Integer fileSizeByte;

    private @Nullable String folderNo;

    private @Nullable String genre;

    /** The index. */
    private @Nullable Integer index;

    private @Nullable String is3D;

    /** Whether the content has already been played */
    private @Nullable String isAlreadyPlayed;

    private @Nullable String isBrowsable;

    private @Nullable String isPlayable;

    /** Whether the content is protected */
    private @Nullable String isProtected;

    /** The original display number. */
    private @Nullable String originalDispNum;

    /** The audio channels */
    private @Nullable ParentalInfo @Nullable [] parentalInfo;

    private @Nullable Integer parentIndex;

    private @Nullable String parentUri;

    private @Nullable String path;

    private @Nullable String playlistName;

    private @Nullable String podcastName;

    /** The product identifier */
    private @Nullable String productID;

    /** The program media type */
    private @Nullable String programMediaType;

    /** The program number */
    private @Nullable Integer programNum;

    private @Nullable String remotePlayType;

    /** The size (in MB) */
    private @Nullable Integer sizeMB;

    /** The start date time */
    private @Nullable String startDateTime;

    /** The storage uri (for usb, etc) */
    private @Nullable String storageUri;

    /** The audio frequencies */
    private @Nullable SubtitleInfo @Nullable [] subtitleInfo;

    /** The content title */
    private @Nullable String title;

    /** The triplet channel number */
    private @Nullable String tripletStr;

    /** The content uri */
    private @Nullable String uri;

    /** The user content flag */
    private @Nullable Boolean userContentFlag;

    private @Nullable VideoInfo videoInfo;

    /** The visibility of the content */
    private @Nullable String visibility;

    public @Nullable String getAlbumName() {
        return albumName;
    }

    public @Nullable String getArtist() {
        return artist;
    }

    public @Nullable AudioInfo @Nullable [] getAudioInfo() {
        return audioInfo;
    }

    public @Nullable Integer getBroadcastFreq() {
        return broadcastFreq;
    }

    public @Nullable String getBroadcastFreqBand() {
        return broadcastFreqBand;
    }

    public @Nullable String getChannelName() {
        return channelName;
    }

    public @Nullable String getChannelSurfingVisibility() {
        return channelSurfingVisibility;
    }

    public @Nullable Integer getChapterCount() {
        return chapterCount;
    }

    public @Nullable ContentInfo getContent() {
        return content;
    }

    public @Nullable String getContentKind() {
        return contentKind;
    }

    public @Nullable String getContentType() {
        return contentType;
    }

    public @Nullable String getCreatedTime() {
        return createdTime;
    }

    public @Nullable Integer getDirectRemoteNum() {
        return directRemoteNum;
    }

    public @Nullable String getDispNum() {
        return dispNum;
    }

    public @Nullable Double getDurationMSec() {
        return durationMSec;
    }

    public @Nullable String getEpgVisibility() {
        return epgVisibility;
    }

    public @Nullable String getFileNo() {
        return fileNo;
    }

    public @Nullable Integer getFileSizeByte() {
        return fileSizeByte;
    }

    public @Nullable String getFolderNo() {
        return folderNo;
    }

    public @Nullable String getGenre() {
        return genre;
    }

    public @Nullable Integer getIndex() {
        return index;
    }

    public @Nullable String is3D() {
        return is3D;
    }

    public @Nullable String isAlreadyPlayed() {
        return isAlreadyPlayed;
    }

    public @Nullable String isBrowsable() {
        return isBrowsable;
    }

    public @Nullable String isPlayable() {
        return isPlayable;
    }

    public @Nullable String isProtected() {
        return isProtected;
    }

    public @Nullable String getOriginalDispNum() {
        return originalDispNum;
    }

    public @Nullable ParentalInfo @Nullable [] getParentalInfo() {
        return parentalInfo;
    }

    public @Nullable Integer getParentIndex() {
        return parentIndex;
    }

    public @Nullable String getParentUri() {
        return parentUri;
    }

    public @Nullable String getPath() {
        return path;
    }

    public @Nullable String getPlaylistName() {
        return playlistName;
    }

    public @Nullable String getPodcastName() {
        return podcastName;
    }

    public @Nullable String getProductID() {
        return productID;
    }

    public @Nullable String getProgramMediaType() {
        return programMediaType;
    }

    public @Nullable Integer getProgramNum() {
        return programNum;
    }

    public @Nullable String getRemotePlayType() {
        return remotePlayType;
    }

    public @Nullable Integer getSizeMB() {
        return sizeMB;
    }

    public @Nullable String getStartDateTime() {
        return startDateTime;
    }

    public @Nullable String getStorageUri() {
        return storageUri;
    }

    public @Nullable SubtitleInfo @Nullable [] getSubtitleInfo() {
        return subtitleInfo;
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

    public @Nullable Boolean getUserContentFlag() {
        return userContentFlag;
    }

    public @Nullable VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public @Nullable String getVisibility() {
        return visibility;
    }

    @Override
    public String toString() {
        return "ContentListResult_1_4 [albumName=" + albumName + ", artist=" + artist + ", audioInfo="
                + Arrays.toString(audioInfo) + ", broadcastFreq=" + broadcastFreq + ", broadcastFreqBand="
                + broadcastFreqBand + ", channelName=" + channelName + ", channelSurfingVisibility="
                + channelSurfingVisibility + ", chapterCount=" + chapterCount + ", content=" + content
                + ", contentKind=" + contentKind + ", contentType=" + contentType + ", createdTime=" + createdTime
                + ", directRemoteNum=" + directRemoteNum + ", dispNum=" + dispNum + ", durationMSec=" + durationMSec
                + ", epgVisibility=" + epgVisibility + ", fileNo=" + fileNo + ", fileSizeByte=" + fileSizeByte
                + ", folderNo=" + folderNo + ", genre=" + genre + ", index=" + index + ", is3D=" + is3D
                + ", isAlreadyPlayed=" + isAlreadyPlayed + ", isBrowsable=" + isBrowsable + ", isPlayable=" + isPlayable
                + ", isProtected=" + isProtected + ", originalDispNum=" + originalDispNum + ", parentalInfo="
                + Arrays.toString(parentalInfo) + ", parentIndex=" + parentIndex + ", parentUri=" + parentUri
                + ", path=" + path + ", playlistName=" + playlistName + ", podcastName=" + podcastName + ", productID="
                + productID + ", programMediaType=" + programMediaType + ", programNum=" + programNum
                + ", remotePlayType=" + remotePlayType + ", sizeMB=" + sizeMB + ", startDateTime=" + startDateTime
                + ", storageUri=" + storageUri + ", subtitleInfo=" + Arrays.toString(subtitleInfo) + ", title=" + title
                + ", tripletStr=" + tripletStr + ", uri=" + uri + ", userContentFlag=" + userContentFlag
                + ", videoInfo=" + videoInfo + ", visibility=" + visibility + "]";
    }
}
