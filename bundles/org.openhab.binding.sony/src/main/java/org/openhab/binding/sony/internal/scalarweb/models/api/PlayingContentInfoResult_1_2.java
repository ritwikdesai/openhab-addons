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

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class represents the request to play content information and is used for deserialization only
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class PlayingContentInfoResult_1_2 extends PlayingContentInfoResult_1_0 {

    /** The album name */
    private @Nullable String albumName;

    /** The application name to the content */
    private @Nullable String applicationName;

    /** The artist */
    private @Nullable String artist;

    /** The audio information */
    private @Nullable AudioInfo @Nullable [] audioInfo;

    /** The broadcast frequency */
    private @Nullable Integer broadcastFreq;

    /** The broadcast frequency band */
    private @Nullable String broadcastFreqBand;

    /** The channel name */
    private @Nullable String channelName;

    /** The chapter count */
    private @Nullable Integer chapterCount;

    /** The chapter index */
    private @Nullable Integer chapterIndex;

    /** The content kind */
    private @Nullable String contentKind;

    /** The dab info */
    private @Nullable DabInfo dabInfo;

    /** The duration milliseconds of the content */
    private @Nullable Integer durationMsec;

    /** The file number? */
    private @Nullable String fileNo;

    /** The genre */
    private @Nullable String genre;

    /** The index of the content */
    private @Nullable Integer index;

    /** The index of the content */
    private @Nullable String is3D;

    /** The output of the content */
    private @Nullable String output;

    /** The parent index of the content */
    private @Nullable Integer parentIndex;

    /** The URI of the parent */
    private @Nullable String parentUri;

    /** The path to the content */
    private @Nullable String path;

    /** The playlist name */
    private @Nullable String playlistName;

    /** The play step speed of the content */
    private @Nullable Integer playStepSpeed;

    /** The podcast name */
    private @Nullable String podcastName;

    /** The position milliseconds of the content */
    private @Nullable Integer positionMsec;

    /** The position seconds of the content */
    private @Nullable Double positionSec;

    /** The repeat type of the content */
    private @Nullable String repeatType;

    /** The service of the content */
    private @Nullable String service;

    /** The source label of the content */
    private @Nullable String sourceLabel;

    /** The state info of the content */
    private @Nullable StateInfo stateInfo;

    /** The subtitle index */
    private @Nullable Integer subtitleIndex;

    /** The total count */
    private @Nullable Integer totalCount;

    /** The video information */
    private @Nullable VideoInfo videoInfo;

    public @Nullable String getAlbumName() {
        return albumName;
    }

    public @Nullable String getApplicationName() {
        return applicationName;
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

    public @Nullable Integer getChapterCount() {
        return chapterCount;
    }

    public @Nullable Integer getChapterIndex() {
        return chapterIndex;
    }

    public @Nullable String getContentKind() {
        return contentKind;
    }

    public @Nullable DabInfo getDabInfo() {
        return dabInfo;
    }

    public @Nullable Integer getDurationMsec() {
        return durationMsec;
    }

    public @Nullable String getFileNo() {
        return fileNo;
    }

    public @Nullable String getGenre() {
        return genre;
    }

    public @Nullable Integer getIndex() {
        return index;
    }

    public @Nullable String getIs3D() {
        return is3D;
    }

    public @Nullable String getOutput() {
        return output;
    }

    public String getOutput(String defValue) {
        final String localOutput = output;
        return localOutput == null || StringUtils.isEmpty(localOutput) ? defValue : localOutput;
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

    public @Nullable Integer getPlayStepSpeed() {
        return playStepSpeed;
    }

    public @Nullable String getPodcastName() {
        return podcastName;
    }

    public @Nullable Integer getPositionMsec() {
        return positionMsec;
    }

    public @Nullable Double getPositionSec() {
        return positionSec;
    }

    public @Nullable String getRepeatType() {
        return repeatType;
    }

    public @Nullable String getService() {
        return service;
    }

    public @Nullable String getSourceLabel() {
        return sourceLabel;
    }

    public @Nullable StateInfo getStateInfo() {
        return stateInfo;
    }

    public @Nullable Integer getSubtitleIndex() {
        return subtitleIndex;
    }

    public @Nullable Integer getTotalCount() {
        return totalCount;
    }

    public @Nullable VideoInfo getVideoInfo() {
        return videoInfo;
    }

    @Override
    public String toString() {
        return "PlayingContentInfoResult_1_2 [albumName=" + albumName + ", applicationName=" + applicationName
                + ", artist=" + artist + ", audioInfo=" + Arrays.toString(audioInfo) + ", broadcastFreq="
                + broadcastFreq + ", broadcastFreqBand=" + broadcastFreqBand + ", channelName=" + channelName
                + ", chapterCount=" + chapterCount + ", chapterIndex=" + chapterIndex + ", contentKind=" + contentKind
                + ", dabInfo=" + dabInfo + ", durationMsec=" + durationMsec + ", fileNo=" + fileNo + ", genre=" + genre
                + ", index=" + index + ", is3D=" + is3D + ", output=" + output + ", parentIndex=" + parentIndex
                + ", parentUri=" + parentUri + ", path=" + path + ", playlistName=" + playlistName + ", playStepSpeed="
                + playStepSpeed + ", podcastName=" + podcastName + ", positionMsec=" + positionMsec + ", positionSec="
                + positionSec + ", repeatType=" + repeatType + ", service=" + service + ", sourceLabel=" + sourceLabel
                + ", stateInfo=" + stateInfo + ", subtitleIndex=" + subtitleIndex + ", totalCount=" + totalCount
                + ", videoInfo=" + videoInfo + ", getBivlAssetId()=" + getBivlAssetId() + ", getBivlProvider()="
                + getBivlProvider() + ", getBivlServiceId()=" + getBivlServiceId() + ", getDispNum()=" + getDispNum()
                + ", getDurationSec()=" + getDurationSec() + ", getMediaType()=" + getMediaType()
                + ", getOriginalDispNum()=" + getOriginalDispNum() + ", getPlaySpeed()=" + getPlaySpeed()
                + ", getProgramNum()=" + getProgramNum() + ", getProgramTitle()=" + getProgramTitle() + ", getSource()="
                + getSource() + ", getStartDateTime()=" + getStartDateTime() + ", getTitle()=" + getTitle()
                + ", getTripletStr()=" + getTripletStr() + ", getUri()=" + getUri() + "]";
    }
}
