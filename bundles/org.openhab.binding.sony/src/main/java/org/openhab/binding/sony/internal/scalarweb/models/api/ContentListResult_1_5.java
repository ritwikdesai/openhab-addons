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
public class ContentListResult_1_5 {

    private @Nullable String albumName;

    private @Nullable String applicationName;

    private @Nullable String artist;

    private @Nullable AudioInfo @Nullable [] audioInfo;

    private @Nullable BivlInfo bivlInfo;

    private @Nullable BroadcastFreq broadcastFreq;

    private @Nullable BroadcastGenreInfo @Nullable [] broadcastGenreInfo;

    /** The channel name */
    private @Nullable String channelName;

    private @Nullable Integer chapterCount;

    private @Nullable Integer chapterIndex;

    private @Nullable Integer clipCount;

    private @Nullable ContentInfo content;

    private @Nullable String contentKind;

    /** The content type */
    private @Nullable String contentType;

    /** The created time */
    private @Nullable String createdTime;

    private @Nullable DabInfo dabInfo;

    private @Nullable DataBroadcastInfo dataInfo;

    private @Nullable Description description;

    /** The direct remote number */
    private @Nullable Integer directRemoteNum;

    /** The content display number */
    private @Nullable String dispNum;

    private @Nullable DubbingInfo dubbingInfo;

    private @Nullable Duration duration;

    private @Nullable String eventId;

    private @Nullable String fileNo;

    /** The file size (bytes) */
    private @Nullable Integer fileSizeByte;

    private @Nullable String folderNo;

    private @Nullable String genre;

    private @Nullable Integer globalPlaybackCount;

    private @Nullable GroupInfo @Nullable [] groupInfo;

    private @Nullable String hasResume;

    /** The index. */
    private @Nullable Integer index;

    private @Nullable String is3D;

    private @Nullable String is4K;

    /** Whether the content has already been played */
    private @Nullable String isAlreadyPlayed;

    private @Nullable String isAutoDelete;

    private @Nullable String isBrowsable;

    private @Nullable String isNew;

    private @Nullable String isPlayable;

    private @Nullable String isPlaylist;

    /** Whether the content is protected */
    private @Nullable String isProtected;

    private @Nullable String isSoundPhoto;

    private @Nullable String mediaType;

    /** The original display number. */
    private @Nullable String originalDispNum;

    private @Nullable String output;

    private @Nullable ParentalInfo @Nullable [] parentalInfo;

    private @Nullable Integer parentIndex;

    private @Nullable String parentUri;

    private @Nullable PlaylistInfo @Nullable [] playlistInfo;

    private @Nullable String playlistName;

    private @Nullable Speed playSpeed;

    private @Nullable String podcastName;

    /** Really doubt this is PIP position */
    private @Nullable Position position;

    /** The product identifier */
    private @Nullable String productID;

    /** The program media type */
    private @Nullable String programMediaType;

    /** The program number */
    private @Nullable Integer programNum;

    private @Nullable String programServiceType;

    private @Nullable String programTitle;

    private @Nullable RecordingInfo recordingInfo;

    private @Nullable String remotePlayType;

    private @Nullable String repeatType;

    private @Nullable String service;

    /** The size (in MB) */
    private @Nullable Integer sizeMB;

    /** The content source */
    private @Nullable String source;

    /** The content source label */
    private @Nullable String sourceLabel;

    /** The start date time */
    private @Nullable String startDateTime;

    private @Nullable StateInfo stateInfo;

    /** The storage uri (for usb, etc) */
    private @Nullable String storageUri;

    private @Nullable SubtitleInfo @Nullable [] subtitleInfo;

    private @Nullable String syncContentPriority;

    /** The content title */
    private @Nullable String title;

    private @Nullable Integer totalCount;

    /** The triplet channel number */
    private @Nullable String tripletStr;

    /** The content uri */
    private @Nullable String uri;

    /** The user content flag */
    private @Nullable Boolean userContentFlag;

    private @Nullable VideoInfo @Nullable [] videoInfo;

    /** The visibility of the content */
    private @Nullable Visibility visibility;

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

    public @Nullable BivlInfo getBivlInfo() {
        return bivlInfo;
    }

    public @Nullable BroadcastFreq getBroadcastFreq() {
        return broadcastFreq;
    }

    public @Nullable BroadcastGenreInfo @Nullable [] getBroadcastGenreInfo() {
        return broadcastGenreInfo;
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

    public @Nullable Integer getClipCount() {
        return clipCount;
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

    public @Nullable DabInfo getDabInfo() {
        return dabInfo;
    }

    public @Nullable DataBroadcastInfo getDataInfo() {
        return dataInfo;
    }

    public @Nullable Description getDescription() {
        return description;
    }

    public @Nullable Integer getDirectRemoteNum() {
        return directRemoteNum;
    }

    public @Nullable String getDispNum() {
        return dispNum;
    }

    public @Nullable DubbingInfo getDubbingInfo() {
        return dubbingInfo;
    }

    public @Nullable Duration getDuration() {
        return duration;
    }

    public @Nullable String getEventId() {
        return eventId;
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

    public @Nullable Integer getGlobalPlaybackCount() {
        return globalPlaybackCount;
    }

    public @Nullable GroupInfo @Nullable [] getGroupInfo() {
        return groupInfo;
    }

    public @Nullable String getHasResume() {
        return hasResume;
    }

    public @Nullable Integer getIndex() {
        return index;
    }

    public @Nullable String is3D() {
        return is3D;
    }

    public @Nullable String is4K() {
        return is4K;
    }

    public @Nullable String isAlreadyPlayed() {
        return isAlreadyPlayed;
    }

    public @Nullable String isAutoDelete() {
        return isAutoDelete;
    }

    public @Nullable String isBrowsable() {
        return isBrowsable;
    }

    public @Nullable String isNew() {
        return isNew;
    }

    public @Nullable String isPlayable() {
        return isPlayable;
    }

    public @Nullable String isPlaylist() {
        return isPlaylist;
    }

    public @Nullable String isProtected() {
        return isProtected;
    }

    public @Nullable String isSoundPhoto() {
        return isSoundPhoto;
    }

    public @Nullable String getMediaType() {
        return mediaType;
    }

    public @Nullable String getOriginalDispNum() {
        return originalDispNum;
    }

    public @Nullable String getOutput() {
        return output;
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

    public @Nullable PlaylistInfo @Nullable [] getPlaylistInfo() {
        return playlistInfo;
    }

    public @Nullable String getPlaylistName() {
        return playlistName;
    }

    public @Nullable Speed getPlaySpeed() {
        return playSpeed;
    }

    public @Nullable String getPodcastName() {
        return podcastName;
    }

    public @Nullable Position getPosition() {
        return position;
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

    public @Nullable String getProgramServiceType() {
        return programServiceType;
    }

    public @Nullable String getProgramTitle() {
        return programTitle;
    }

    public @Nullable RecordingInfo getRecordingInfo() {
        return recordingInfo;
    }

    public @Nullable String getRemotePlayType() {
        return remotePlayType;
    }

    public @Nullable String getRepeatType() {
        return repeatType;
    }

    public @Nullable String getService() {
        return service;
    }

    public @Nullable Integer getSizeMB() {
        return sizeMB;
    }

    public @Nullable String getSource() {
        return source;
    }

    public @Nullable String getSourceLabel() {
        return sourceLabel;
    }

    public @Nullable String getStartDateTime() {
        return startDateTime;
    }

    public @Nullable StateInfo getStateInfo() {
        return stateInfo;
    }

    public @Nullable String getStorageUri() {
        return storageUri;
    }

    public @Nullable SubtitleInfo @Nullable [] getSubtitleInfo() {
        return subtitleInfo;
    }

    public @Nullable String getSyncContentPriority() {
        return syncContentPriority;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public @Nullable Integer getTotalCount() {
        return totalCount;
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

    public @Nullable VideoInfo @Nullable [] getVideoInfo() {
        return videoInfo;
    }

    public @Nullable Visibility getVisibility() {
        return visibility;
    }

    @Override
    public String toString() {
        return "ContentListResult_1_5 [albumName=" + albumName + ", applicationName=" + applicationName + ", artist="
                + artist + ", audioInfo=" + Arrays.toString(audioInfo) + ", bivlInfo=" + bivlInfo + ", broadcastFreq="
                + broadcastFreq + ", broadcastGenreInfo=" + Arrays.toString(broadcastGenreInfo) + ", channelName="
                + channelName + ", chapterCount=" + chapterCount + ", chapterIndex=" + chapterIndex + ", clipCount="
                + clipCount + ", content=" + content + ", contentKind=" + contentKind + ", contentType=" + contentType
                + ", createdTime=" + createdTime + ", dabInfo=" + dabInfo + ", dataInfo=" + dataInfo + ", description="
                + description + ", directRemoteNum=" + directRemoteNum + ", dispNum=" + dispNum + ", dubbingInfo="
                + dubbingInfo + ", duration=" + duration + ", eventId=" + eventId + ", fileNo=" + fileNo
                + ", fileSizeByte=" + fileSizeByte + ", folderNo=" + folderNo + ", genre=" + genre
                + ", globalPlaybackCount=" + globalPlaybackCount + ", groupInfo=" + Arrays.toString(groupInfo)
                + ", hasResume=" + hasResume + ", index=" + index + ", is3D=" + is3D + ", is4K=" + is4K
                + ", isAlreadyPlayed=" + isAlreadyPlayed + ", isAutoDelete=" + isAutoDelete + ", isBrowsable="
                + isBrowsable + ", isNew=" + isNew + ", isPlayable=" + isPlayable + ", isPlaylist=" + isPlaylist
                + ", isProtected=" + isProtected + ", isSoundPhoto=" + isSoundPhoto + ", mediaType=" + mediaType
                + ", originalDispNum=" + originalDispNum + ", output=" + output + ", parentalInfo="
                + Arrays.toString(parentalInfo) + ", parentIndex=" + parentIndex + ", parentUri=" + parentUri
                + ", playlistInfo=" + Arrays.toString(playlistInfo) + ", playlistName=" + playlistName + ", playSpeed="
                + playSpeed + ", podcastName=" + podcastName + ", position=" + position + ", productID=" + productID
                + ", programMediaType=" + programMediaType + ", programNum=" + programNum + ", programServiceType="
                + programServiceType + ", programTitle=" + programTitle + ", recordingInfo=" + recordingInfo
                + ", remotePlayType=" + remotePlayType + ", repeatType=" + repeatType + ", service=" + service
                + ", sizeMB=" + sizeMB + ", source=" + source + ", sourceLabel=" + sourceLabel + ", startDateTime="
                + startDateTime + ", stateInfo=" + stateInfo + ", storageUri=" + storageUri + ", subtitleInfo="
                + Arrays.toString(subtitleInfo) + ", syncContentPriority=" + syncContentPriority + ", title=" + title
                + ", totalCount=" + totalCount + ", tripletStr=" + tripletStr + ", uri=" + uri + ", userContentFlag="
                + userContentFlag + ", videoInfo=" + Arrays.toString(videoInfo) + ", visibility=" + visibility + "]";
    }
}
