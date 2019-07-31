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
/*
 *
 */
package org.openhab.binding.sony.internal.scalarweb.protocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateOption;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannel;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelDescriptor;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelTracker;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebContext;
import org.openhab.binding.sony.internal.scalarweb.VersionUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebError;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebEvent;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.openhab.binding.sony.internal.scalarweb.models.api.ActiveTerminal;
import org.openhab.binding.sony.internal.scalarweb.models.api.AudioInfo;
import org.openhab.binding.sony.internal.scalarweb.models.api.BivlInfo;
import org.openhab.binding.sony.internal.scalarweb.models.api.BroadcastFreq;
import org.openhab.binding.sony.internal.scalarweb.models.api.ContentCount_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.ContentCount_1_3;
import org.openhab.binding.sony.internal.scalarweb.models.api.ContentListRequest_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.ContentListRequest_1_4;
import org.openhab.binding.sony.internal.scalarweb.models.api.ContentListResult_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.ContentListResult_1_2;
import org.openhab.binding.sony.internal.scalarweb.models.api.ContentListResult_1_4;
import org.openhab.binding.sony.internal.scalarweb.models.api.ContentListResult_1_5;
import org.openhab.binding.sony.internal.scalarweb.models.api.Count;
import org.openhab.binding.sony.internal.scalarweb.models.api.CurrentExternalInputsStatus_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.CurrentExternalInputsStatus_1_1;
import org.openhab.binding.sony.internal.scalarweb.models.api.CurrentExternalTerminalsStatus_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.DabInfo;
import org.openhab.binding.sony.internal.scalarweb.models.api.DeleteContent;
import org.openhab.binding.sony.internal.scalarweb.models.api.DeleteProtection;
import org.openhab.binding.sony.internal.scalarweb.models.api.Description;
import org.openhab.binding.sony.internal.scalarweb.models.api.Duration;
import org.openhab.binding.sony.internal.scalarweb.models.api.Output;
import org.openhab.binding.sony.internal.scalarweb.models.api.ParentalInfo;
import org.openhab.binding.sony.internal.scalarweb.models.api.ParentalRatingSetting_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.PlayContent_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.PlayContent_1_2;
import org.openhab.binding.sony.internal.scalarweb.models.api.PlayingContentInfoRequest_1_2;
import org.openhab.binding.sony.internal.scalarweb.models.api.PlayingContentInfoResult_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.PlayingContentInfoResult_1_2;
import org.openhab.binding.sony.internal.scalarweb.models.api.PresetBroadcastStation;
import org.openhab.binding.sony.internal.scalarweb.models.api.ScanPlayingContent_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.Scheme;
import org.openhab.binding.sony.internal.scalarweb.models.api.SeekBroadcastStation_1_0;
import org.openhab.binding.sony.internal.scalarweb.models.api.Source;
import org.openhab.binding.sony.internal.scalarweb.models.api.StateInfo;
import org.openhab.binding.sony.internal.scalarweb.models.api.SubtitleInfo;
import org.openhab.binding.sony.internal.scalarweb.models.api.TvContentVisibility;
import org.openhab.binding.sony.internal.scalarweb.models.api.VideoInfo;
import org.openhab.binding.sony.internal.scalarweb.models.api.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of the protocol handles the Av Content service
 *
 * @author Tim Roberts - Initial contribution
 * @param <T> the generic type for the callback
 */
@NonNullByDefault
class ScalarWebAvContentProtocol<T extends ThingCallback<String>> extends AbstractScalarWebProtocol<T> {

    private static final String MAINOUTPUT = "main"; // cannot be empty as it's used as a channel id
    private static final String MAINTITLE = "Main";

    // Constants used by this protocol
    private static final String BLUETOOTHSETTINGS = "bluetoothsetting";
    private static final String PLAYBACKSETTINGS = "playbackmode";
    private static final String SCHEMES = "schemes";
    private static final String SOURCES = "sources";

    private static final String PARENTRATING = "pr_";
    private static final String PR_RATINGTYPEAGE = PARENTRATING + "ratingtypeage";
    private static final String PR_RATINGTYPESONY = PARENTRATING + "ratingtypesony";
    private static final String PR_RATINGCOUNTRY = PARENTRATING + "ratingcountry";
    private static final String PR_RATINGCUSTOMTYPETV = PARENTRATING + "ratingcustomtypetv";
    private static final String PR_RATINGCUSTOMTYPEMPAA = PARENTRATING + "ratingcustomtypempaa";
    private static final String PR_RATINGCUSTOMTYPECAENGLISH = PARENTRATING + "ratingcustomtypecaenglish";
    private static final String PR_RATINGCUSTOMTYPECAFRENCH = PARENTRATING + "ratingcustomtypecafrench";
    private static final String PR_UNRATEDLOCK = PARENTRATING + "unratedlock";

    private static final String PLAYING = "pl_";
    private static final String PL_CMD = PLAYING + "cmd";
    private static final String PL_URI = PLAYING + "uri";
    private static final String PL_SOURCE = PLAYING + "source";
    private static final String PL_TITLE = PLAYING + "title";
    private static final String PL_DISPNUM = PLAYING + "dispnum";
    private static final String PL_ORIGINALDISPNUM = PLAYING + "originaldispnum";
    private static final String PL_TRIPLETSTR = PLAYING + "tripletstr";
    private static final String PL_PROGRAMNUM = PLAYING + "programnum";
    private static final String PL_PROGRAMTITLE = PLAYING + "programtitle";
    private static final String PL_STARTDATETIME = PLAYING + "startdatetime";
    private static final String PL_DURATIONSEC = PLAYING + "durationsec";
    private static final String PL_MEDIATYPE = PLAYING + "mediatype";
    private static final String PL_PLAYSPEED = PLAYING + "playspeed";
    private static final String PL_BIVLSERVICEID = PLAYING + "bivlserviceid";
    private static final String PL_BIVLASSETID = PLAYING + "bivlassetid";
    private static final String PL_BIVLPROVIDER = PLAYING + "bivlprovider";
    private static final String PL_SOURCELABEL = PLAYING + "sourcelabel";
    private static final String PL_OUTPUT = PLAYING + "output";
    private static final String PL_STATE = PLAYING + "state";
    private static final String PL_STATESUPPLEMENT = PLAYING + "statesupplement";
    private static final String PL_POSITIONSEC = PLAYING + "positionsec";
    private static final String PL_POSITIONMSEC = PLAYING + "positionmsec";
    private static final String PL_DURATIONMSEC = PLAYING + "durationmsec";
    private static final String PL_PLAYSTEPSPEED = PLAYING + "playstepspeed";
    private static final String PL_REPEATTYPE = PLAYING + "repeattype";
    private static final String PL_CHAPTERINDEX = PLAYING + "chapterindex";
    private static final String PL_CHAPTERCOUNT = PLAYING + "chaptercount";
    private static final String PL_SUBTITLEINDEX = PLAYING + "subtitleindex";
    private static final String PL_ARTIST = PLAYING + "artist";
    private static final String PL_GENRE = PLAYING + "genre";
    private static final String PL_ALBUMNAME = PLAYING + "albumname";
    private static final String PL_CONTENTKIND = PLAYING + "contentkind";
    private static final String PL_FILENO = PLAYING + "fileno";
    private static final String PL_CHANNELNAME = PLAYING + "channelname";
    private static final String PL_PLAYLISTNAME = PLAYING + "playlistname";
    private static final String PL_PODCASTNAME = PLAYING + "podcastname";
    private static final String PL_TOTALCOUNT = PLAYING + "totalcount";
    private static final String PL_BROADCASTFREQ = PLAYING + "broadcastfreq";
    private static final String PL_BROADCASTFREQBAND = PLAYING + "broadcastfreqband";
    private static final String PL_DABCOMPONENTLABEL = PLAYING + "dabcomponentlabel";
    private static final String PL_DABDYNAMICLABEL = PLAYING + "dabdynamiclabel";
    private static final String PL_DABENSEMBLELABEL = PLAYING + "dabensemblelabel";
    private static final String PL_DABSERVICELABEL = PLAYING + "dabservicelabel";
    private static final String PL_AUDIOCHANNEL = PLAYING + "audiochannel";
    private static final String PL_AUDIOCODEC = PLAYING + "audiocodec";
    private static final String PL_AUDIOFREQUENCY = PLAYING + "audiofrequency";
    private static final String PL_PARENTURI = PLAYING + "parenturi";
    private static final String PL_VIDEOCODEC = PLAYING + "videocodec";
    private static final String PL_SERVICE = PLAYING + "service";
    private static final String PL_INDEX = PLAYING + "index";
    private static final String PL_PARENTINDEX = PLAYING + "parentindex";
    private static final String PL_IS3D = PLAYING + "is3d";
    private static final String PL_PATH = PLAYING + "path";
    private static final String PL_APPLICATIONNAME = PLAYING + "applicationname";
    private static final String PL_PRESET = PLAYING + "presetid";

    private static final String INPUT = "in_";
    private static final String IN_URI = INPUT + "uri";
    private static final String IN_TITLE = INPUT + "title";
    private static final String IN_CONNECTION = INPUT + "connection";
    private static final String IN_LABEL = INPUT + "label";
    private static final String IN_ICON = INPUT + "icon";
    private static final String IN_STATUS = INPUT + "status";

    private static final String TERM = "tm_";
    private static final String TERM_SOURCE = TERM + "source";
    private static final String TERM_URI = TERM + "uri";
    private static final String TERM_TITLE = TERM + "title";
    private static final String TERM_CONNECTION = TERM + "connection";
    private static final String TERM_LABEL = TERM + "label";
    private static final String TERM_ICON = TERM + "icon";
    private static final String TERM_ACTIVE = TERM + "active";

    private static final String CONTENT = "cn_";
    private static final String CN_ALBUMNAME = CONTENT + "albumname";
    private static final String CN_APPLICATIONNAME = CONTENT + "applicationname";
    private static final String CN_ARTIST = CONTENT + "artist";
    private static final String CN_AUDIOCHANNEL = CONTENT + "audiochannel";
    private static final String CN_AUDIOCODEC = CONTENT + "audiocodec";
    private static final String CN_AUDIOFREQUENCY = CONTENT + "audiofrequency";
    private static final String CN_BROADCASTFREQ = CONTENT + "broadcastfreq";
    private static final String CN_BIVLSERVICEID = CONTENT + "bivlserviceid";
    private static final String CN_BIVLASSETID = CONTENT + "bivleassetid";
    private static final String CN_BIVLPROVIDER = CONTENT + "bivlprovider";
    private static final String CN_BROADCASTFREQBAND = CONTENT + "broadcastfreqband";
    private static final String CN_CHANNELNAME = CONTENT + "channelname";
    private static final String CN_CHANNELSURFINGVISIBILITY = CONTENT + "channelsurfingvisibility";
    private static final String CN_CHAPTERCOUNT = CONTENT + "chaptercount";
    private static final String CN_CHAPTERINDEX = CONTENT + "chapterindex";
    private static final String CN_CHILDCOUNT = CONTENT + "childcount";
    private static final String CN_CLIPCOUNT = CONTENT + "clipcount";
    private static final String CN_CONTENTKIND = CONTENT + "contentkind";
    private static final String CN_CONTENTTYPE = CONTENT + "contenttype";
    private static final String CN_CREATEDTIME = CONTENT + "createdtime";
    private static final String CN_DABCOMPONENTLABEL = CONTENT + "dabcomponentlabel";
    private static final String CN_DABDYNAMICLABEL = CONTENT + "dabdynamiclabel";
    private static final String CN_DABENSEMBLELABEL = CONTENT + "dabensemblelabel";
    private static final String CN_DABSERVICELABEL = CONTENT + "dabservicelabel";
    private static final String CN_DESCRIPTION = CONTENT + "description";
    private static final String CN_DIRECTREMOTENUM = CONTENT + "directremotenum";
    private static final String CN_DISPNUM = CONTENT + "dispnum";
    private static final String CN_DURATIONMSEC = CONTENT + "durationmsec";
    private static final String CN_DURATIONSEC = CONTENT + "durationsec";
    private static final String CN_EPGVISIBILITY = CONTENT + "epgvisibility";
    private static final String CN_EVENTID = CONTENT + "eventid";
    private static final String CN_FILENO = CONTENT + "fileno";
    private static final String CN_FILESIZEBYTE = CONTENT + "filesizebyte";
    private static final String CN_FOLDERNO = CONTENT + "folderno";
    private static final String CN_GENRE = CONTENT + "genre";
    private static final String CN_GLOBALPLAYBACKCOUNT = CONTENT + "globalplaybackcount";
    private static final String CN_HASRESUME = CONTENT + "hasresume";
    private static final String CN_INDEX = CONTENT + "idx";
    private static final String CN_IS3D = CONTENT + "IS3D";
    private static final String CN_IS4K = CONTENT + "is4k";
    private static final String CN_ISALREADYPLAYED = CONTENT + "isalreadyplayed";
    private static final String CN_ISAUTODELETE = CONTENT + "isautodelete";
    private static final String CN_ISBROWSABLE = CONTENT + "isbrowsable";
    private static final String CN_ISNEW = CONTENT + "isnew";
    private static final String CN_ISPLAYABLE = CONTENT + "isplayable";
    private static final String CN_ISPLAYLIST = CONTENT + "isplaylist";
    private static final String CN_ISPROTECTED = CONTENT + "isprotected";
    private static final String CN_ISSOUNDPHOTO = CONTENT + "issoundphoto";
    private static final String CN_MEDIATYPE = CONTENT + "mediatype";
    private static final String CN_ORIGINALDISPNUM = CONTENT + "originaldispnum";
    private static final String CN_OUTPUT = CONTENT + "output";
    private static final String CN_PARENTALCOUNTRY = CONTENT + "parentalcountry";
    private static final String CN_PARENTALRATING = CONTENT + "parentalrating";
    private static final String CN_PARENTALSYSTEM = CONTENT + "parentalsystem";
    private static final String CN_PARENTINDEX = CONTENT + "parentindex";
    private static final String CN_PARENTURI = CONTENT + "parenturi";
    private static final String CN_PATH = CONTENT + "PATH";
    private static final String CN_PLAYLISTNAME = CONTENT + "playlistname";
    private static final String CN_PODCASTNAME = CONTENT + "podcastname";
    private static final String CN_PRODUCTID = CONTENT + "productid";
    private static final String CN_PROGRAMMEDIATYPE = CONTENT + "programmediatype";
    private static final String CN_PROGRAMNUM = CONTENT + "programnum";
    private static final String CN_PROGRAMSERVICETYPE = CONTENT + "programservicetype";
    private static final String CN_PROGRAMTITLE = CONTENT + "programtitle";
    private static final String CN_REMOTEPLAYTYPE = CONTENT + "remoteplaytype";
    private static final String CN_REPEATTYPE = CONTENT + "repeattype";
    private static final String CN_SELECTED = CONTENT + "selected";
    private static final String CN_SERVICE = CONTENT + "service";
    private static final String CN_SIZEMB = CONTENT + "sizemb";
    private static final String CN_SOURCE = CONTENT + "source";
    private static final String CN_SOURCELABEL = CONTENT + "sourcelabel";
    private static final String CN_STARTDATETIME = CONTENT + "startdatetime";
    private static final String CN_STATE = CONTENT + "state";
    private static final String CN_STATESUPPLEMENT = CONTENT + "statesupplement";
    private static final String CN_STORAGEURI = CONTENT + "storageuri";
    private static final String CN_SUBTITLELANGUAGE = CONTENT + "subtitlelanguage";
    private static final String CN_SUBTITLETITLE = CONTENT + "subtitletitle";
    private static final String CN_SYNCCONTENTPRIORITY = CONTENT + "synccontentpriority";
    private static final String CN_TITLE = CONTENT + "title";
    private static final String CN_TOTALCOUNT = CONTENT + "totalcount";
    private static final String CN_TRIPLETSTR = CONTENT + "tripletstr";
    private static final String CN_URI = CONTENT + "uri";
    private static final String CN_USERCONTENTFLAG = CONTENT + "usercontentflag";
    private static final String CN_VIDEOCODEC = CONTENT + "videocodec";
    private static final String CN_VISIBILITY = CONTENT + "visibility";

    /** The logger */
    private Logger logger = LoggerFactory.getLogger(ScalarWebAvContentProtocol.class);

    private final AtomicReference<Set<Scheme>> stateSchemes = new AtomicReference<>(new HashSet<>());
    private final ConcurrentMap<String, Set<Source>> stateSources = new ConcurrentHashMap<>();
    private final AtomicReference<List<CurrentExternalTerminalsStatus_1_0>> stateTerminals = new AtomicReference<>(
            new ArrayList<>());
    private final AtomicReference<@Nullable ScalarWebResult> stateInputs = new AtomicReference<>(null);
    private final AtomicReference<ContentState> stateContent = new AtomicReference<>(new ContentState());

    private final ConcurrentMap<String, PlayingState> statePlaying = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, String> stateTermToUid = new ConcurrentHashMap<>();

    /**
     * Instantiates a new scalar web av content protocol.
     *
     * @param factory  the non-null factory
     * @param context  the non-null context
     * @param service  the non-null service
     * @param callback the non-null callback
     */
    ScalarWebAvContentProtocol(ScalarWebProtocolFactory<T> factory, ScalarWebContext context, ScalarWebService service,
            T callback) {
        super(factory, context, service, callback);
        enableNotifications(ScalarWebEvent.NOTIFYPLAYINGCONTENTINFO, ScalarWebEvent.NOTIFYEXTERNALTERMINALSTATUS);
    }

    @Override
    public Collection<ScalarWebChannelDescriptor> getChannelDescriptors() {
        final ChannelIdCache cache = new ChannelIdCache();
        final List<ScalarWebChannelDescriptor> descriptors = new ArrayList<ScalarWebChannelDescriptor>();

        if (service.hasMethod(ScalarWebMethod.GETCONTENTLIST)) {
            addContentListDescriptors(descriptors);
        }

        if (service.hasMethod(ScalarWebMethod.GETSCHEMELIST)) {
            descriptors.add(createDescriptor(createChannel(SCHEMES), "String", "scalarwebavcontrolschemes"));
        }

        if (service.hasMethod(ScalarWebMethod.GETSOURCELIST)) {
            descriptors.add(createDescriptor(createChannel(SOURCES), "String", "scalarwebavcontrolsources"));
        }

        // don't check has here since we create a dummy terminal for single output devices
        addTerminalStatusDescriptors(descriptors, cache);

        if (service.hasMethod(ScalarWebMethod.GETCURRENTEXTERNALINPUTSSTATUS)) {
            addInputStatusDescriptors(descriptors, cache);
        }

        if (service.hasMethod(ScalarWebMethod.GETPARENTALRATINGSETTINGS)) {
            addParentalRatingDescriptors(descriptors);
        }

        // Note: must come AFTER terminal descriptors since we use the IDs generated from it
        if (service.hasMethod(ScalarWebMethod.GETPLAYINGCONTENTINFO)) {
            addPlayingContentDescriptors(descriptors, cache);
        }

        addGeneralSettingsDescriptor(descriptors, cache, ScalarWebMethod.GETBLUETOOTHSETTINGS, BLUETOOTHSETTINGS,
                "Bluetooth Setting");

        addGeneralSettingsDescriptor(descriptors, cache, ScalarWebMethod.GETPLAYBACKMODESETTINGS, PLAYBACKSETTINGS,
                "Playback Setting");

        // update the terminal sources
        updateTermSource();

        return descriptors;
    }

    /**
     * Adds the content list descriptors
     * @param descriptors the non-null, possibly empty list of descriptors
     */
    private void addContentListDescriptors(List<ScalarWebChannelDescriptor> descriptors) {
        Objects.requireNonNull(descriptors, "descriptors cannot be null");

        // The control (parent uri/uri/index) and virtual channels (childcount/selected)
        descriptors.add(createDescriptor(createChannel(CN_PARENTURI), "String", "scalarwebavcontrolcontentparenturi"));
        descriptors.add(createDescriptor(createChannel(CN_URI), "String", "scalarwebavcontrolcontenturi"));
        descriptors.add(createDescriptor(createChannel(CN_INDEX), "Number", "scalarwebavcontrolcontentidx"));
        descriptors
                .add(createDescriptor(createChannel(CN_CHILDCOUNT), "Number", "scalarwebavcontrolcontentchildcount"));

        // final Map<String, String> outputs = getTerminalOutputs(getTerminalStatuses());
        //
        // for (Entry<String, String> entry : outputs.entrySet()) {
        // descriptors.add(createDescriptor(createChannel(CN_SELECTED, entry.getKey()), "Switch",
        // "scalarwebavcontroltermstatusselected", "Content Play on Output " + entry.getValue(), null));
        // }
        descriptors.add(createDescriptor(createChannel(CN_SELECTED), "String", "scalarwebavcontrolcontentselected"));

        final String version = getVersion(ScalarWebMethod.GETCONTENTLIST);
        if (VersionUtilities.equals(version, ScalarWebMethod.V1_0, ScalarWebMethod.V1_1)) {
            descriptors.add(
                    createDescriptor(createChannel(CN_CHANNELNAME), "String", "scalarwebavcontrolcontentchannelname"));
            descriptors.add(createDescriptor(createChannel(CN_DIRECTREMOTENUM), "Number",
                    "scalarwebavcontrolcontentdirectremotenum"));
            descriptors.add(createDescriptor(createChannel(CN_DISPNUM), "String", "scalarwebavcontrolcontentdispnum"));
            descriptors.add(
                    createDescriptor(createChannel(CN_DURATIONSEC), "Number", "scalarwebavcontrolcontentdurationsec"));
            descriptors.add(createDescriptor(createChannel(CN_FILESIZEBYTE), "Number",
                    "scalarwebavcontrolcontentfilesizebyte"));
            descriptors.add(createDescriptor(createChannel(CN_ISALREADYPLAYED), "String",
                    "scalarwebavcontrolcontentisalreadyplayed"));
            descriptors.add(
                    createDescriptor(createChannel(CN_ISPROTECTED), "String", "scalarwebavcontrolcontentisprotected"));
            descriptors.add(createDescriptor(createChannel(CN_ORIGINALDISPNUM), "String",
                    "scalarwebavcontrolcontentoriginaldispnum"));
            descriptors.add(createDescriptor(createChannel(CN_PROGRAMMEDIATYPE), "String",
                    "scalarwebavcontrolcontentprogrammediatype"));
            descriptors.add(
                    createDescriptor(createChannel(CN_PROGRAMNUM), "Number", "scalarwebavcontrolcontentprogramnum"));
            descriptors.add(createDescriptor(createChannel(CN_STARTDATETIME), "String",
                    "scalarwebavcontrolcontentstartdatetime"));
            descriptors.add(createDescriptor(createChannel(CN_TITLE), "String", "scalarwebavcontrolcontenttitle"));
            descriptors.add(
                    createDescriptor(createChannel(CN_TRIPLETSTR), "String", "scalarwebavcontrolcontenttripletstr"));
        } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_2, ScalarWebMethod.V1_3)) {
            descriptors.add(createDescriptor(createChannel(CN_AUDIOCHANNEL), "String",
                    "scalarwebavcontrolcontentaudiochannel"));
            descriptors.add(
                    createDescriptor(createChannel(CN_AUDIOCODEC), "String", "scalarwebavcontrolcontentaudiocodec"));
            descriptors.add(createDescriptor(createChannel(CN_AUDIOFREQUENCY), "String",
                    "scalarwebavcontrolcontentaudiofrequency"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CHANNELNAME), "String", "scalarwebavcontrolcontentchannelname"));
            descriptors.add(createDescriptor(createChannel(CN_CHANNELSURFINGVISIBILITY), "String",
                    "scalarwebavcontrolcontentchannelsurfingvisibility"));
            descriptors.add(createDescriptor(createChannel(CN_CHAPTERCOUNT), "Number",
                    "scalarwebavcontrolcontentchaptercount"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CONTENTTYPE), "String", "scalarwebavcontrolcontentcontenttype"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CREATEDTIME), "String", "scalarwebavcontrolcontentcreatedtime"));
            descriptors.add(createDescriptor(createChannel(CN_DIRECTREMOTENUM), "Number",
                    "scalarwebavcontrolcontentdirectremotenum"));
            descriptors.add(createDescriptor(createChannel(CN_DISPNUM), "String", "scalarwebavcontrolcontentdispnum"));
            descriptors.add(
                    createDescriptor(createChannel(CN_DURATIONSEC), "Number", "scalarwebavcontrolcontentdurationsec"));
            descriptors.add(createDescriptor(createChannel(CN_EPGVISIBILITY), "String",
                    "scalarwebavcontrolcontentepgvisibility"));
            descriptors.add(createDescriptor(createChannel(CN_FILESIZEBYTE), "Number",
                    "scalarwebavcontrolcontentfilesizebyte"));
            descriptors.add(createDescriptor(createChannel(CN_ISALREADYPLAYED), "String",
                    "scalarwebavcontrolcontentisalreadyplayed"));
            descriptors.add(
                    createDescriptor(createChannel(CN_ISPROTECTED), "String", "scalarwebavcontrolcontentisprotected"));
            descriptors.add(createDescriptor(createChannel(CN_ORIGINALDISPNUM), "String",
                    "scalarwebavcontrolcontentoriginaldispnum"));
            descriptors.add(createDescriptor(createChannel(CN_PARENTALCOUNTRY), "String",
                    "scalarwebavcontrolcontentparentalcountry"));
            descriptors.add(createDescriptor(createChannel(CN_PARENTALRATING), "String",
                    "scalarwebavcontrolcontentparentalrating"));
            descriptors.add(createDescriptor(createChannel(CN_PARENTALSYSTEM), "String",
                    "scalarwebavcontrolcontentparentalsystem"));
            descriptors
                    .add(createDescriptor(createChannel(CN_PRODUCTID), "String", "scalarwebavcontrolcontentproductid"));
            descriptors.add(createDescriptor(createChannel(CN_PROGRAMMEDIATYPE), "String",
                    "scalarwebavcontrolcontentprogrammediatype"));
            descriptors.add(
                    createDescriptor(createChannel(CN_PROGRAMNUM), "Number", "scalarwebavcontrolcontentprogramnum"));
            descriptors.add(createDescriptor(createChannel(CN_SIZEMB), "Number", "scalarwebavcontrolcontentsizemb"));
            descriptors.add(createDescriptor(createChannel(CN_STARTDATETIME), "String",
                    "scalarwebavcontrolcontentstartdatetime"));
            descriptors.add(
                    createDescriptor(createChannel(CN_STORAGEURI), "String", "scalarwebavcontrolcontentstorageuri"));
            descriptors.add(createDescriptor(createChannel(CN_SUBTITLELANGUAGE), "String",
                    "scalarwebavcontrolcontentsubtitlelanguage"));
            descriptors.add(createDescriptor(createChannel(CN_SUBTITLETITLE), "String",
                    "scalarwebavcontrolcontentsubtitletitle"));
            descriptors.add(createDescriptor(createChannel(CN_TITLE), "String", "scalarwebavcontrolcontenttitle"));
            descriptors.add(
                    createDescriptor(createChannel(CN_TRIPLETSTR), "String", "scalarwebavcontrolcontenttripletstr"));
            descriptors.add(createDescriptor(createChannel(CN_USERCONTENTFLAG), "String",
                    "scalarwebavcontrolcontentusercontentflag"));
            descriptors.add(
                    createDescriptor(createChannel(CN_VIDEOCODEC), "String", "scalarwebavcontrolcontentvideocodec"));
            descriptors.add(
                    createDescriptor(createChannel(CN_VISIBILITY), "String", "scalarwebavcontrolcontentvisibility"));

        } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_4)) {
            descriptors
                    .add(createDescriptor(createChannel(CN_ALBUMNAME), "String", "scalarwebavcontrolcontentalbumname"));
            descriptors.add(createDescriptor(createChannel(CN_ARTIST), "String", "scalarwebavcontrolcontentartist"));
            descriptors.add(createDescriptor(createChannel(CN_AUDIOCHANNEL), "String",
                    "scalarwebavcontrolcontentaudiochannel"));
            descriptors.add(
                    createDescriptor(createChannel(CN_AUDIOCODEC), "String", "scalarwebavcontrolcontentaudiocodec"));
            descriptors.add(createDescriptor(createChannel(CN_AUDIOFREQUENCY), "String",
                    "scalarwebavcontrolcontentaudiofrequency"));
            descriptors.add(createDescriptor(createChannel(CN_BROADCASTFREQ), "Number",
                    "scalarwebavcontrolcontentbroadcastfreq"));
            descriptors.add(createDescriptor(createChannel(CN_BROADCASTFREQBAND), "String",
                    "scalarwebavcontrolcontentbroadcastband"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CHANNELNAME), "String", "scalarwebavcontrolcontentchannelname"));
            descriptors.add(createDescriptor(createChannel(CN_CHANNELSURFINGVISIBILITY), "String",
                    "scalarwebavcontrolcontentchannelsurfingvisibility"));
            descriptors.add(createDescriptor(createChannel(CN_CHAPTERCOUNT), "Number",
                    "scalarwebavcontrolcontentchaptercount"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CONTENTKIND), "String", "scalarwebavcontrolcontentcontentkind"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CONTENTTYPE), "String", "scalarwebavcontrolcontentcontenttype"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CREATEDTIME), "String", "scalarwebavcontrolcontentcreatedtime"));
            descriptors.add(createDescriptor(createChannel(CN_DIRECTREMOTENUM), "Number",
                    "scalarwebavcontrolcontentdirectremotenum"));
            descriptors.add(createDescriptor(createChannel(CN_DISPNUM), "String", "scalarwebavcontrolcontentdispnum"));
            descriptors.add(createDescriptor(createChannel(CN_DURATIONMSEC), "Number",
                    "scalarwebavcontrolcontentdurationmsec"));
            descriptors.add(createDescriptor(createChannel(CN_EPGVISIBILITY), "String",
                    "scalarwebavcontrolcontentepgvisibility"));
            descriptors.add(createDescriptor(createChannel(CN_FILENO), "String", "scalarwebavcontrolcontentfileno"));
            descriptors.add(createDescriptor(createChannel(CN_FILESIZEBYTE), "Number",
                    "scalarwebavcontrolcontentfilesizebyte"));
            descriptors
                    .add(createDescriptor(createChannel(CN_FOLDERNO), "String", "scalarwebavcontrolcontentfolderno"));
            descriptors.add(createDescriptor(createChannel(CN_GENRE), "String", "scalarwebavcontrolcontentgenre"));
            descriptors.add(createDescriptor(createChannel(CN_IS3D), "String", "scalarwebavcontrolcontentis3d"));
            descriptors.add(createDescriptor(createChannel(CN_ISALREADYPLAYED), "String",
                    "scalarwebavcontrolcontentisalreadyplayed"));
            descriptors.add(
                    createDescriptor(createChannel(CN_ISBROWSABLE), "String", "scalarwebavcontrolcontentisbrowsable"));
            descriptors.add(
                    createDescriptor(createChannel(CN_ISPLAYABLE), "String", "scalarwebavcontrolcontentisplayable"));
            descriptors.add(
                    createDescriptor(createChannel(CN_ISPROTECTED), "String", "scalarwebavcontrolcontentisprotected"));
            descriptors.add(createDescriptor(createChannel(CN_ORIGINALDISPNUM), "String",
                    "scalarwebavcontrolcontentoriginaldispnum"));
            descriptors.add(createDescriptor(createChannel(CN_PARENTALCOUNTRY), "String",
                    "scalarwebavcontrolcontentparentalcountry"));
            descriptors.add(createDescriptor(createChannel(CN_PARENTALRATING), "String",
                    "scalarwebavcontrolcontentparentalrating"));
            descriptors.add(createDescriptor(createChannel(CN_PARENTALSYSTEM), "String",
                    "scalarwebavcontrolcontentparentalsystem"));
            descriptors.add(
                    createDescriptor(createChannel(CN_PARENTINDEX), "Number", "scalarwebavcontrolcontentparentindex"));
            descriptors.add(createDescriptor(createChannel(CN_PATH), "String", "scalarwebavcontrolcontentpath"));
            descriptors.add(createDescriptor(createChannel(CN_PLAYLISTNAME), "String",
                    "scalarwebavcontrolcontentplaylistname"));
            descriptors.add(
                    createDescriptor(createChannel(CN_PODCASTNAME), "String", "scalarwebavcontrolcontentpodcastname"));
            descriptors
                    .add(createDescriptor(createChannel(CN_PRODUCTID), "String", "scalarwebavcontrolcontentproductid"));
            descriptors.add(createDescriptor(createChannel(CN_PROGRAMMEDIATYPE), "String",
                    "scalarwebavcontrolcontentprogrammediatype"));
            descriptors.add(
                    createDescriptor(createChannel(CN_PROGRAMNUM), "Number", "scalarwebavcontrolcontentprogramnum"));
            descriptors.add(createDescriptor(createChannel(CN_REMOTEPLAYTYPE), "String",
                    "scalarwebavcontrolcontentremoteplaytype"));
            descriptors.add(createDescriptor(createChannel(CN_SIZEMB), "Number", "scalarwebavcontrolcontentsizemb"));
            descriptors.add(createDescriptor(createChannel(CN_STARTDATETIME), "String",
                    "scalarwebavcontrolcontentstartdatetime"));
            descriptors.add(
                    createDescriptor(createChannel(CN_STORAGEURI), "String", "scalarwebavcontrolcontentstorageuri"));
            descriptors.add(createDescriptor(createChannel(CN_SUBTITLELANGUAGE), "String",
                    "scalarwebavcontrolcontentsubtitlelanguage"));
            descriptors.add(createDescriptor(createChannel(CN_SUBTITLETITLE), "String",
                    "scalarwebavcontrolcontentsubtitletitle"));
            descriptors.add(createDescriptor(createChannel(CN_TITLE), "String", "scalarwebavcontrolcontenttitle"));
            descriptors.add(
                    createDescriptor(createChannel(CN_TRIPLETSTR), "String", "scalarwebavcontrolcontenttripletstr"));
            descriptors.add(createDescriptor(createChannel(CN_USERCONTENTFLAG), "String",
                    "scalarwebavcontrolcontentusercontentflag"));
            descriptors.add(
                    createDescriptor(createChannel(CN_VIDEOCODEC), "String", "scalarwebavcontrolcontentvideocodec"));
            descriptors.add(
                    createDescriptor(createChannel(CN_VISIBILITY), "String", "scalarwebavcontrolcontentvisibility"));
        } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_5)) {
            descriptors
                    .add(createDescriptor(createChannel(CN_ALBUMNAME), "String", "scalarwebavcontrolcontentalbumname"));
            descriptors.add(createDescriptor(createChannel(CN_APPLICATIONNAME), "String",
                    "scalarwebavcontrolplapplicationname"));
            descriptors.add(createDescriptor(createChannel(CN_ARTIST), "String", "scalarwebavcontrolcontentartist"));
            descriptors.add(createDescriptor(createChannel(CN_AUDIOCHANNEL), "String",
                    "scalarwebavcontrolcontentaudiochannel"));
            descriptors.add(
                    createDescriptor(createChannel(CN_AUDIOCODEC), "String", "scalarwebavcontrolcontentaudiocodec"));
            descriptors.add(createDescriptor(createChannel(CN_AUDIOFREQUENCY), "String",
                    "scalarwebavcontrolcontentaudiofrequency"));
            descriptors.add(createDescriptor(createChannel(CN_BIVLSERVICEID), "String",
                    "scalarwebavcontrolcontentbivlserviceid"));
            descriptors.add(
                    createDescriptor(createChannel(CN_BIVLASSETID), "String", "scalarwebavcontrolcontentbivlassetid"));
            descriptors.add(createDescriptor(createChannel(CN_BIVLPROVIDER), "String",
                    "scalarwebavcontrolcontentbivlprovider"));
            descriptors.add(createDescriptor(createChannel(CN_BROADCASTFREQ), "Number",
                    "scalarwebavcontrolcontentbroadcastfreq"));
            descriptors.add(createDescriptor(createChannel(CN_BROADCASTFREQBAND), "String",
                    "scalarwebavcontrolcontentbroadcastband"));
            // descriptors.add(createDescriptor(createChannel(CN_BROADCASTGENREINFO), "String",
            // "scalarwebavcontrolcontextbroadcastGenreInfo"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CHANNELNAME), "String", "scalarwebavcontrolcontentchannelname"));
            descriptors.add(createDescriptor(createChannel(CN_CHANNELSURFINGVISIBILITY), "String",
                    "scalarwebavcontrolcontentchannelsurfingvisibility"));
            descriptors.add(createDescriptor(createChannel(CN_CHAPTERCOUNT), "Number",
                    "scalarwebavcontrolcontentchaptercount"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CHAPTERINDEX), "Number", "scalarwebavcontrolplchapterindex"));
            descriptors
                    .add(createDescriptor(createChannel(CN_CLIPCOUNT), "Number", "scalarwebavcontrolcontentclipcount"));
            // descriptors.add(createDescriptor(createChannel(CN_CONTENT), "String",
            // "scalarwebavcontrolcontextcontent"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CONTENTKIND), "String", "scalarwebavcontrolcontentcontentkind"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CONTENTTYPE), "String", "scalarwebavcontrolcontentcontenttype"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CREATEDTIME), "String", "scalarwebavcontrolcontentcreatedtime"));
            descriptors.add(
                    createDescriptor(createChannel(CN_CREATEDTIME), "String", "scalarwebavcontrolcontentcreatedtime"));
            descriptors.add(createDescriptor(createChannel(CN_DABCOMPONENTLABEL), "String",
                    "scalarwebavcontrolcontentdabcomponentlabel"));
            descriptors.add(createDescriptor(createChannel(CN_DABDYNAMICLABEL), "String",
                    "scalarwebavcontrolcontentdabdynamiclabel"));
            descriptors.add(createDescriptor(createChannel(CN_DABENSEMBLELABEL), "String",
                    "scalarwebavcontrolcontentdabensemblelabel"));
            descriptors.add(createDescriptor(createChannel(CN_DABSERVICELABEL), "String",
                    "scalarwebavcontrolcontentdabservicelabel"));
            // descriptors.add(createDescriptor(createChannel(CN_DATAINFO), "String",
            // "scalarwebavcontrolcontextdataInfo"));
            // // todo
            descriptors.add(
                    createDescriptor(createChannel(CN_DESCRIPTION), "String", "scalarwebavcontrolcontentdescription"));
            descriptors.add(createDescriptor(createChannel(CN_DIRECTREMOTENUM), "Number",
                    "scalarwebavcontrolcontentdirectremotenum"));
            descriptors.add(createDescriptor(createChannel(CN_DISPNUM), "String", "scalarwebavcontrolcontentdispnum"));
            // descriptors.add(createDescriptor(createChannel(CN_DUBBINGINFO), "String",
            // "scalarwebavcontrolcontextdubbingInfo")); // todo
            descriptors.add(createDescriptor(createChannel(CN_DURATIONMSEC), "Number",
                    "scalarwebavcontrolcontentdurationmsec"));
            descriptors.add(
                    createDescriptor(createChannel(CN_DURATIONSEC), "Number", "scalarwebavcontrolcontentdurationsec"));
            descriptors.add(createDescriptor(createChannel(CN_EPGVISIBILITY), "String",
                    "scalarwebavcontrolcontentepgvisibility"));
            descriptors.add(createDescriptor(createChannel(CN_EVENTID), "String", "scalarwebavcontrolcontenteventid"));
            descriptors.add(createDescriptor(createChannel(CN_FILENO), "String", "scalarwebavcontrolcontentfileno"));
            descriptors.add(createDescriptor(createChannel(CN_FILESIZEBYTE), "Number",
                    "scalarwebavcontrolcontentfilesizebyte"));
            descriptors
                    .add(createDescriptor(createChannel(CN_FOLDERNO), "String", "scalarwebavcontrolcontentfolderno"));
            descriptors.add(createDescriptor(createChannel(CN_GENRE), "String", "scalarwebavcontrolcontentgenre"));
            descriptors.add(createDescriptor(createChannel(CN_GLOBALPLAYBACKCOUNT), "Number",
                    "scalarwebavcontrolcontentglobalplaybackcount"));
            // descriptors.add(createDescriptor(createChannel(CN_GROUPINFO), "String",
            // "scalarwebavcontrolcontextgroupInfo")); // TODO
            descriptors
                    .add(createDescriptor(createChannel(CN_HASRESUME), "String", "scalarwebavcontrolcontenthasresume"));
            descriptors.add(createDescriptor(createChannel(CN_IS3D), "String", "scalarwebavcontrolcontentis3d"));
            descriptors.add(createDescriptor(createChannel(CN_IS4K), "String", "scalarwebavcontrolcontentis4k"));
            descriptors.add(createDescriptor(createChannel(CN_ISALREADYPLAYED), "String",
                    "scalarwebavcontrolcontentisalreadyplayed"));
            descriptors.add(createDescriptor(createChannel(CN_ISAUTODELETE), "String",
                    "scalarwebavcontrolcontentisautodelete"));
            descriptors.add(
                    createDescriptor(createChannel(CN_ISBROWSABLE), "String", "scalarwebavcontrolcontentisbrowsable"));
            descriptors.add(createDescriptor(createChannel(CN_ISNEW), "String", "scalarwebavcontrolcontentisnew"));
            descriptors.add(
                    createDescriptor(createChannel(CN_ISPLAYABLE), "String", "scalarwebavcontrolcontentisplayable"));
            descriptors.add(
                    createDescriptor(createChannel(CN_ISPLAYLIST), "String", "scalarwebavcontrolcontentisplaylist"));
            descriptors.add(
                    createDescriptor(createChannel(CN_ISPROTECTED), "String", "scalarwebavcontrolcontentisprotected"));
            descriptors.add(createDescriptor(createChannel(CN_ISSOUNDPHOTO), "String",
                    "scalarwebavcontrolcontentissoundphoto"));
            descriptors
                    .add(createDescriptor(createChannel(CN_MEDIATYPE), "String", "scalarwebavcontrolcontentmediatype"));
            descriptors.add(createDescriptor(createChannel(CN_ORIGINALDISPNUM), "String",
                    "scalarwebavcontrolcontentoriginaldispnum"));
            descriptors.add(createDescriptor(createChannel(CN_OUTPUT), "String", "scalarwebavcontrolcontentoutput"));
            descriptors.add(createDescriptor(createChannel(CN_PARENTALCOUNTRY), "String",
                    "scalarwebavcontrolcontentparentalcountry"));
            descriptors.add(createDescriptor(createChannel(CN_PARENTALRATING), "String",
                    "scalarwebavcontrolcontentparentalrating"));
            descriptors.add(createDescriptor(createChannel(CN_PARENTALSYSTEM), "String",
                    "scalarwebavcontrolcontentparentalsystem"));
            descriptors.add(
                    createDescriptor(createChannel(CN_PARENTINDEX), "Number", "scalarwebavcontrolcontentparentindex"));
            // descriptors.add(createDescriptor(createChannel(CN_PLAYLISTINFO), "String",
            // "scalarwebavcontrolcontextplaylistInfo")); //todo
            descriptors.add(createDescriptor(createChannel(CN_PLAYLISTNAME), "String",
                    "scalarwebavcontrolcontentplaylistname"));
            // descriptors.add(createDescriptor(createChannel(CN_PLAYSPEED), "String",
            // "scalarwebavcontrolcontextplaySpeed")); // todo
            descriptors.add(
                    createDescriptor(createChannel(CN_PODCASTNAME), "String", "scalarwebavcontrolcontentpodcastname"));
            // descriptors.add(createDescriptor(createChannel(CN_POSITION), "String",
            // "scalarwebavcontrolcontextposition"));
            // //todo
            descriptors
                    .add(createDescriptor(createChannel(CN_PRODUCTID), "String", "scalarwebavcontrolcontentproductid"));
            descriptors.add(createDescriptor(createChannel(CN_PROGRAMMEDIATYPE), "String",
                    "scalarwebavcontrolcontentprogrammediatype"));
            descriptors.add(
                    createDescriptor(createChannel(CN_PROGRAMNUM), "Number", "scalarwebavcontrolcontentprogramnum"));
            descriptors.add(createDescriptor(createChannel(CN_PROGRAMSERVICETYPE), "String",
                    "scalarwebavcontrolcontentprogramservicetype"));
            descriptors.add(createDescriptor(createChannel(CN_PROGRAMTITLE), "String",
                    "scalarwebavcontrolcontentprogramtitle"));
            // descriptors.add(createDescriptor(createChannel(CN_RECORDINGINFO), "String",
            // "scalarwebavcontrolcontextrecordingInfo")); // todo
            descriptors.add(createDescriptor(createChannel(CN_REMOTEPLAYTYPE), "String",
                    "scalarwebavcontrolcontentremoteplaytype"));
            descriptors.add(
                    createDescriptor(createChannel(CN_REPEATTYPE), "String", "scalarwebavcontrolcontentrepeattype"));
            descriptors.add(createDescriptor(createChannel(CN_SERVICE), "String", "scalarwebavcontrolcontentservice"));
            descriptors.add(createDescriptor(createChannel(CN_SIZEMB), "Number", "scalarwebavcontrolcontentsizemb"));
            descriptors.add(createDescriptor(createChannel(CN_SOURCE), "String", "scalarwebavcontrolcontentsource"));
            descriptors.add(
                    createDescriptor(createChannel(CN_SOURCELABEL), "String", "scalarwebavcontrolcontentsourcelabel"));
            descriptors.add(createDescriptor(createChannel(CN_STARTDATETIME), "String",
                    "scalarwebavcontrolcontentstartdatetime"));
            descriptors.add(createDescriptor(createChannel(CN_STATE), "String", "scalarwebavcontrolcontentstate"));
            descriptors.add(createDescriptor(createChannel(CN_STATESUPPLEMENT), "String",
                    "scalarwebavcontrolcontentstatesupplement"));
            descriptors.add(
                    createDescriptor(createChannel(CN_STORAGEURI), "String", "scalarwebavcontrolcontentstorageuri"));
            descriptors.add(createDescriptor(createChannel(CN_SUBTITLELANGUAGE), "String",
                    "scalarwebavcontrolcontentsubtitlelanguage"));
            descriptors.add(createDescriptor(createChannel(CN_SUBTITLETITLE), "String",
                    "scalarwebavcontrolcontentsubtitletitle"));
            descriptors.add(createDescriptor(createChannel(CN_SYNCCONTENTPRIORITY), "String",
                    "scalarwebavcontrolcontentsynccontentpriority"));
            descriptors.add(createDescriptor(createChannel(CN_TITLE), "String", "scalarwebavcontrolcontenttitle"));
            descriptors.add(
                    createDescriptor(createChannel(CN_TOTALCOUNT), "Number", "scalarwebavcontrolcontenttotalcount"));
            descriptors.add(
                    createDescriptor(createChannel(CN_TRIPLETSTR), "String", "scalarwebavcontrolcontenttripletstr"));
            descriptors.add(createDescriptor(createChannel(CN_USERCONTENTFLAG), "String",
                    "scalarwebavcontrolcontentusercontentflag"));
            descriptors.add(
                    createDescriptor(createChannel(CN_VIDEOCODEC), "String", "scalarwebavcontrolcontentvideocodec"));
            descriptors.add(
                    createDescriptor(createChannel(CN_VISIBILITY), "String", "scalarwebavcontrolcontentvisibility"));
        }

    }

    /**
     * Adds input status descriptors for a specific input
     * @param descriptors a non-null, possibly empty list of descriptors
     * @param id a non-null, non-empty channel id
     * @param uri a non-null, non-empty input uri
     * @param title a non-null, non-empty input title
     * @param apiVersion a non-null, non-empty API version
     */
    private void addInputStatusDescriptor(List<ScalarWebChannelDescriptor> descriptors,
            String id, String uri, String title, String apiVersion) {
        Objects.requireNonNull(descriptors, "descriptors cannot be null");
        Validate.notEmpty(id, "id cannot be empty");
        Validate.notEmpty(uri, "uri cannot be empty");
        Validate.notEmpty(title, "title cannot be empty");
        Validate.notEmpty(apiVersion, "apiVersion cannot be empty");

        descriptors.add(createDescriptor(createChannel(IN_URI, id, uri), "String", "scalarwebavcontrolinpstatusuri",
                "Input " + title + " URI", uri));

        descriptors.add(createDescriptor(createChannel(IN_TITLE, id, uri), "String", "scalarwebavcontrolinpstatustitle",
                "Input " + title + " Title", uri));

        descriptors.add(createDescriptor(createChannel(IN_CONNECTION, id, uri), "String",
                "scalarwebavcontrolinpstatusconnection", "Input " + title + " Connected", uri));

        descriptors.add(createDescriptor(createChannel(IN_LABEL, id, uri), "String", "scalarwebavcontrolinpstatuslabel",
                "Input " + title + " Label", uri));

        descriptors.add(createDescriptor(createChannel(IN_ICON, id, uri), "String", "scalarwebavcontrolinpstatusicon",
                "Input " + title + " Icon", uri));

        if (StringUtils.equalsIgnoreCase(apiVersion, ScalarWebMethod.V1_1)) {
            descriptors.add(createDescriptor(createChannel(IN_STATUS, id, uri), "String",
                    "scalarwebavcontrolinpstatusstatus", "Input " + title + " Status", uri));
        }
    }

    /**
     * Adds all input status descriptors
     * @param descriptors a non-null, possibly empty list of descriptors
     * @param cache a non-null channel cache
     */
    private void addInputStatusDescriptors(List<ScalarWebChannelDescriptor> descriptors, ChannelIdCache cache) {
        Objects.requireNonNull(descriptors, "descriptors cannot be null");
        Objects.requireNonNull(cache, "cache cannot be null");

        try {
            final ScalarWebResult result = getInputStatus();
            final String version = getService().getVersion(ScalarWebMethod.GETCURRENTEXTERNALINPUTSSTATUS);
            if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
                for (CurrentExternalInputsStatus_1_0 status : result.asArray(CurrentExternalInputsStatus_1_0.class)) {
                    final String uri = status.getUri();
                    if (uri == null || StringUtils.isEmpty(uri)) {
                        logger.debug("External Input status had no URI (which is required): {}", status);
                        continue;
                    }

                    final String id = cache.getUniqueChannelId(status.getTitle(uri));
                    addInputStatusDescriptor(descriptors, id, uri, status.getTitle(MAINTITLE), ScalarWebMethod.V1_0);
                }
            } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_1)) {
                for (CurrentExternalInputsStatus_1_1 status : result.asArray(CurrentExternalInputsStatus_1_1.class)) {
                    final String uri = status.getUri();
                    if (uri == null || StringUtils.isEmpty(uri)) {
                        logger.debug("External Input status had no URI (which is required): {}", status);
                        continue;
                    }

                    final String id = cache.getUniqueChannelId(status.getTitle(uri));
                    addInputStatusDescriptor(descriptors, id, uri, status.getTitle(MAINTITLE), ScalarWebMethod.V1_1);
                }
            }
        } catch (IOException e) {
            logger.debug("Error add input status description {}", e.getMessage());
        }
    }

    /**
     * Adds the parental rating descriptors
     * @param descriptors a non-null, possibly empty list of descriptors
     */
    private void addParentalRatingDescriptors(List<ScalarWebChannelDescriptor> descriptors) {
        Objects.requireNonNull(descriptors, "descriptors cannot be null");

        try {
            // execute to verify if it exists
            getParentalRating();

            descriptors.add(
                    createDescriptor(createChannel(PR_RATINGTYPEAGE), "Number", "scalarwebavcontrolprratingtypeage"));

            descriptors.add(
                    createDescriptor(createChannel(PR_RATINGTYPESONY), "String", "scalarwebavcontrolprratingtypesony"));
            descriptors.add(
                    createDescriptor(createChannel(PR_RATINGCOUNTRY), "String", "scalarwebavcontrolprratingcountry"));
            descriptors.add(createDescriptor(createChannel(PR_RATINGCUSTOMTYPETV), "String",
                    "scalarwebavcontrolprratingcustomtypetv"));
            descriptors.add(createDescriptor(createChannel(PR_RATINGCUSTOMTYPEMPAA), "String",
                    "scalarwebavcontrolprratingcustomtypempaa"));
            descriptors.add(createDescriptor(createChannel(PR_RATINGCUSTOMTYPECAENGLISH), "String",
                    "scalarwebavcontrolprratingcustomtypecaenglish"));
            descriptors.add(createDescriptor(createChannel(PR_RATINGCUSTOMTYPECAFRENCH), "String",
                    "scalarwebavcontrolprratingcustomtypecafrench"));
            descriptors
                    .add(createDescriptor(createChannel(PR_UNRATEDLOCK), "String", "scalarwebavcontrolprunratedlock"));
        } catch (IOException e) {
            logger.debug("Exception occurring getting the parental ratings: {}", e.getMessage());
        }
    }

    /**
     * Adds the playing content descriptors
     * @param descriptors a non-null, possibly empty list of descriptors
     * @param cache a non-null channel cache
     */
    private void addPlayingContentDescriptors(List<ScalarWebChannelDescriptor> descriptors, ChannelIdCache cache) {
        Objects.requireNonNull(descriptors, "descriptors cannot be null");
        Objects.requireNonNull(cache, "cache cannot be null");

        final Map<String, String> outputs = getTerminalOutputs(getTerminalStatuses());

        final String version = getService().getVersion(ScalarWebMethod.GETPLAYINGCONTENTINFO);

        for (Entry<String, String> entry : outputs.entrySet()) {

            final String prefix = "Playing" + (StringUtils.equalsIgnoreCase(entry.getKey(), MAINOUTPUT) ? " "
                    : (" (" + entry.getValue() + ") "));

            final String uri = entry.getKey();
            final String id = getIdForOutput(uri); // use the same id as the related terminal

            descriptors.add(createDescriptor(createChannel(PL_CMD, id, uri), "String", "scalarwebavcontrolplcommand",
                    prefix + "Command", null));

            descriptors.add(createDescriptor(createChannel(PL_PRESET, id, uri), "Number", "scalarwebavcontrolplpreset",
                    prefix + "Preset", null));

            if (VersionUtilities.equals(version, ScalarWebMethod.V1_0, ScalarWebMethod.V1_1, ScalarWebMethod.V1_2)) {
                descriptors.add(createDescriptor(createChannel(PL_BIVLASSETID, id, uri), "String",
                        "scalarwebavcontrolplbivlassetid", prefix + "BIVL AssetID", null));

                descriptors.add(createDescriptor(createChannel(PL_BIVLPROVIDER, id, uri), "String",
                        "scalarwebavcontrolplbivlprovider", prefix + "BIVL Provider", null));

                descriptors.add(createDescriptor(createChannel(PL_BIVLSERVICEID, id, uri), "String",
                        "scalarwebavcontrolplbivlserviceid", prefix + "BIVL ServiceID", null));

                descriptors.add(createDescriptor(createChannel(PL_DISPNUM, id, uri), "String",
                        "scalarwebavcontrolpldispnum", prefix + "Display Number", null));

                descriptors.add(createDescriptor(createChannel(PL_DURATIONSEC, id, uri), "String",
                        "scalarwebavcontrolpldurationsec", prefix + "Duraction (in seconds)", null));

                descriptors.add(createDescriptor(createChannel(PL_MEDIATYPE, id, uri), "String",
                        "scalarwebavcontrolplmediatype", prefix + "Media Type", null));

                descriptors.add(createDescriptor(createChannel(PL_ORIGINALDISPNUM, id, uri), "String",
                        "scalarwebavcontrolploriginaldispnum", prefix + "Original Display Number", null));

                descriptors.add(createDescriptor(createChannel(PL_PLAYSPEED, id, uri), "String",
                        "scalarwebavcontrolplplayspeed", prefix + "Play Speed", null));

                descriptors.add(createDescriptor(createChannel(PL_PROGRAMNUM, id, uri), "Number",
                        "scalarwebavcontrolplprogramnum", prefix + "Program Number", null));

                descriptors.add(createDescriptor(createChannel(PL_PROGRAMTITLE, id, uri), "String",
                        "scalarwebavcontrolplprogramtitle", prefix + "Program Title", null));

                descriptors.add(createDescriptor(createChannel(PL_SOURCE, id, uri), "String",
                        "scalarwebavcontrolplsource", prefix + "Source", null));

                descriptors.add(createDescriptor(createChannel(PL_STARTDATETIME, id, uri), "String",
                        "scalarwebavcontrolplstartdatetime", prefix + "Start Date/Time", null));

                descriptors.add(createDescriptor(createChannel(PL_TITLE, id, uri), "String",
                        "scalarwebavcontrolpltitle", prefix + "Title", null));

                descriptors.add(createDescriptor(createChannel(PL_TRIPLETSTR, id, uri), "String",
                        "scalarwebavcontrolpltripletstr", prefix + "Triplet", null));

                descriptors.add(createDescriptor(createChannel(PL_URI, id, uri), "String", "scalarwebavcontrolpluri",
                        prefix + "URI", null));

            }

            if (VersionUtilities.equals(version, ScalarWebMethod.V1_2)) {
                descriptors.add(createDescriptor(createChannel(PL_ALBUMNAME, id, uri), "String",
                        "scalarwebavcontrolplalbumname", prefix + "Album Nmae", null));
                descriptors.add(createDescriptor(createChannel(PL_APPLICATIONNAME, id, uri), "String",
                        "scalarwebavcontrolplapplicationname", prefix + "Application Name", null));
                descriptors.add(createDescriptor(createChannel(PL_ARTIST, id, uri), "String",
                        "scalarwebavcontrolplartist", prefix + "Artist", null));
                descriptors.add(createDescriptor(createChannel(PL_AUDIOCHANNEL, id, uri), "String",
                        "scalarwebavcontrolplaudiochannel", prefix + "Audio Channel", null));
                descriptors.add(createDescriptor(createChannel(PL_AUDIOCODEC, id, uri), "String",
                        "scalarwebavcontrolplaudiocodec", prefix + "Audio Codec", null));
                descriptors.add(createDescriptor(createChannel(PL_AUDIOFREQUENCY, id, uri), "String",
                        "scalarwebavcontrolplaudiofrequency", prefix + "Audio Frequency", null));
                descriptors.add(createDescriptor(createChannel(PL_BROADCASTFREQ, id, uri), "Number",
                        "scalarwebavcontrolplbroadcastfreq", prefix + "Broadcast Frequency", null));
                descriptors.add(createDescriptor(createChannel(PL_BROADCASTFREQBAND, id, uri), "String",
                        "scalarwebavcontrolplbroadcastfreqband", prefix + "Broadcast Frequency Band", null));
                descriptors.add(createDescriptor(createChannel(PL_CHANNELNAME, id, uri), "String",
                        "scalarwebavcontrolplchannelname", prefix + "Channel Name", null));
                descriptors.add(createDescriptor(createChannel(PL_CHAPTERCOUNT, id, uri), "Number",
                        "scalarwebavcontrolplchaptercount", prefix + "Chapter Count", null));
                descriptors.add(createDescriptor(createChannel(PL_CHAPTERINDEX, id, uri), "Number",
                        "scalarwebavcontrolplchapterindex", prefix + "Chapter Index", null));
                descriptors.add(createDescriptor(createChannel(PL_CONTENTKIND, id, uri), "String",
                        "scalarwebavcontrolplcontentkind", prefix + "Content Kind", null));
                descriptors.add(createDescriptor(createChannel(PL_DABCOMPONENTLABEL, id, uri), "String",
                        "scalarwebavcontrolpldabcomponentlabel", prefix + "DAB Component Label", null));
                descriptors.add(createDescriptor(createChannel(PL_DABDYNAMICLABEL, id, uri), "String",
                        "scalarwebavcontrolpldabdynamiclabel", prefix + "DAB Dynamic Label", null));
                descriptors.add(createDescriptor(createChannel(PL_DABENSEMBLELABEL, id, uri), "String",
                        "scalarwebavcontrolpldabensemblelabel", prefix + "DAB Ensemble Label", null));
                descriptors.add(createDescriptor(createChannel(PL_DABSERVICELABEL, id, uri), "String",
                        "scalarwebavcontrolpldabservicelabel", prefix + "DAB Service Label", null));
                descriptors.add(createDescriptor(createChannel(PL_DURATIONMSEC, id, uri), "Number",
                        "scalarwebavcontrolpldurationmsec", prefix + "Duration (in milliseconds)", null));
                descriptors.add(createDescriptor(createChannel(PL_FILENO, id, uri), "String",
                        "scalarwebavcontrolplfileno", prefix + "File Number", null));
                descriptors.add(createDescriptor(createChannel(PL_GENRE, id, uri), "String",
                        "scalarwebavcontrolplgenre", prefix + "Genre", null));
                descriptors.add(createDescriptor(createChannel(PL_INDEX, id, uri), "Number",
                        "scalarwebavcontrolplindex", prefix + "Index", null));
                descriptors.add(createDescriptor(createChannel(PL_IS3D, id, uri), "String", "scalarwebavcontrolplis3d",
                        prefix + "is 3D", null));
                descriptors.add(createDescriptor(createChannel(PL_OUTPUT, id, uri), "String",
                        "scalarwebavcontrolploutput", prefix + "Output", null));
                descriptors.add(createDescriptor(createChannel(PL_PARENTINDEX, id, uri), "Number",
                        "scalarwebavcontrolplparentindex", prefix + "Parent Index", null));
                descriptors.add(createDescriptor(createChannel(PL_PARENTURI, id, uri), "String",
                        "scalarwebavcontrolplparenturi", prefix + "Parent URI", null));
                descriptors.add(createDescriptor(createChannel(PL_PATH, id, uri), "String", "scalarwebavcontrolplpath",
                        prefix + "Path", null));
                descriptors.add(createDescriptor(createChannel(PL_PLAYLISTNAME, id, uri), "String",
                        "scalarwebavcontrolplplaylistname", prefix + "Play List Name", null));
                descriptors.add(createDescriptor(createChannel(PL_PLAYSTEPSPEED, id, uri), "Number",
                        "scalarwebavcontrolplplaystepspeed", prefix + "Play Step Speed", null));
                descriptors.add(createDescriptor(createChannel(PL_PODCASTNAME, id, uri), "String",
                        "scalarwebavcontrolplpodcastname", prefix + "Podcast Name", null));
                descriptors.add(createDescriptor(createChannel(PL_POSITIONMSEC, id, uri), "Number",
                        "scalarwebavcontrolplpositionmsec", prefix + "Position (in milliseconds)", null));
                descriptors.add(createDescriptor(createChannel(PL_POSITIONSEC, id, uri), "Number",
                        "scalarwebavcontrolplpositionsec", prefix + "Position (in seconds)", null));
                descriptors.add(createDescriptor(createChannel(PL_REPEATTYPE, id, uri), "String",
                        "scalarwebavcontrolplrepeattype", prefix + "Repeat Type", null));
                descriptors.add(createDescriptor(createChannel(PL_SERVICE, id, uri), "String",
                        "scalarwebavcontrolplservice", prefix + "Service", null));
                descriptors.add(createDescriptor(createChannel(PL_SOURCELABEL, id, uri), "String",
                        "scalarwebavcontrolplsourcelabel", prefix + "Source Label", null));
                descriptors.add(createDescriptor(createChannel(PL_STATE, id, uri), "String",
                        "scalarwebavcontrolplstate", prefix + "State", null));
                descriptors.add(createDescriptor(createChannel(PL_STATESUPPLEMENT, id, uri), "String",
                        "scalarwebavcontrolplstatesupplement", prefix + "State Supplement", null));
                descriptors.add(createDescriptor(createChannel(PL_SUBTITLEINDEX, id, uri), "Number",
                        "scalarwebavcontrolplsubtitleindex", prefix + "Subtitle Index", null));
                descriptors.add(createDescriptor(createChannel(PL_TOTALCOUNT, id, uri), "Number",
                        "scalarwebavcontrolpltotalcount", prefix + "Total Count", null));
                descriptors.add(createDescriptor(createChannel(PL_VIDEOCODEC, id, uri), "String",
                        "scalarwebavcontrolplvideocodec", prefix + "Video Codec", null));
            }
        }
    }

    /**
     * Adds the terminal status descriptors
     * @param descriptors a non-null, possibly empty list of descriptors
     * @param cache a non-null channel cache
     */
    private void addTerminalStatusDescriptors(List<ScalarWebChannelDescriptor> descriptors, ChannelIdCache cache) {
        Objects.requireNonNull(descriptors, "descriptors cannot be null");
        Objects.requireNonNull(cache, "cache cannot be null");
        for (final CurrentExternalTerminalsStatus_1_0 term : getTerminalStatuses()) {
            final String uri = term.getUri();
            if (uri == null) {
                logger.debug("External Terminal status had no URI (which is required): {}", term);
                continue;
            }

            final String title = term.getTitle(MAINTITLE);
            final String id = cache.getUniqueChannelId(term.getTitle(uri));

            stateTermToUid.put(uri, id);

            if (term.isOutput()) {
                descriptors.add(createDescriptor(createChannel(TERM_SOURCE, id, uri), "String",
                        "scalarwebavcontroltermstatussource", "Terminal " + title + " Source", null));

                // if not our dummy 'main', create an active switch
                if (!StringUtils.equalsIgnoreCase(title, MAINTITLE)) {
                    descriptors.add(createDescriptor(createChannel(TERM_ACTIVE, id, uri), "Switch",
                            "scalarwebavcontroltermstatusactive", "Terminal " + title + " Active", null));
                }
            }

            descriptors.add(createDescriptor(createChannel(TERM_URI, id, uri), "String",
                    "scalarwebavcontroltermstatusuri", "Terminal " + title + " URI", null));

            descriptors.add(createDescriptor(createChannel(TERM_TITLE, id, uri), "String",
                    "scalarwebavcontroltermstatustitle", "Terminal " + title + " Title", null));

            descriptors.add(createDescriptor(createChannel(TERM_CONNECTION, id, uri), "String",
                    "scalarwebavcontroltermstatusconnection", "Terminal " + title + " Connection", null));

            descriptors.add(createDescriptor(createChannel(TERM_LABEL, id, uri), "String",
                    "scalarwebavcontroltermstatuslabel", "Terminal " + title + " Label", null));

            descriptors.add(createDescriptor(createChannel(TERM_ICON, id, uri), "String",
                    "scalarwebavcontroltermstatusicon", "Terminal " + title + " Icon", null));

        }
    }

    @Override
    protected void eventReceived(ScalarWebEvent event) throws IOException {
        switch (event.getMethod()) {
            case ScalarWebEvent.NOTIFYPLAYINGCONTENTINFO:
                final String version = getVersion(ScalarWebMethod.GETPLAYINGCONTENTINFO);
                if (VersionUtilities.equals(version, ScalarWebMethod.V1_0, ScalarWebMethod.V1_1)) {
                    notifyPlayingContentInfo(event.as(PlayingContentInfoResult_1_0.class), getIdForOutput(MAINOUTPUT));
                } else {
                    final PlayingContentInfoResult_1_2 res = event.as(PlayingContentInfoResult_1_2.class);
                    final String output = res.getOutput(MAINOUTPUT);
                    notifyPlayingContentInfo(res, getIdForOutput(output));
                }

                break;

            case ScalarWebEvent.NOTIFYEXTERNALTERMINALSTATUS:
                notifyCurrentTerminalStatus(event.as(CurrentExternalTerminalsStatus_1_0.class));
                break;

            default:
                logger.debug("Unhandled event received: {}", event);
                break;
        }
    }

    /**
     * Get's the channel id for the given output
     * @param output a non-null, non-empty output identifier
     * @return a channel identifier representing the output
     */
    private String getIdForOutput(String output) {
        Validate.notEmpty(output, "output cannot be empty");

        for (Channel chl : getContext().getThing().getChannels()) {
            final ScalarWebChannel swc = new ScalarWebChannel(chl);
            if (StringUtils.equalsIgnoreCase(swc.getCategory(), TERM_URI)) {
                final String uri = swc.getPathPart(0);
                if (StringUtils.equals(uri, output)) {
                    return swc.getId();
                }
            }
        }

        return SonyUtil.createValidChannelUId(output);
    }

    /**
     * Get the source identifier for the given URL
     * @param uid a non-null, non-empty source
     * @return the source identifier (or uid if none found)
     */
    private String getSourceFromUri(String uid) {
        Validate.notEmpty(uid, "uid cannot be empty");
        // Following finds the source from the uri (radio:fm&content=x to radio:fm)
        final String src = getSources().stream().filter(s -> StringUtils.startsWith(uid, s.getSource())).findFirst()
                .map(s -> s.getSource()).orElse(null);
        return src == null || StringUtils.isEmpty(src) ? uid : src;

    }

    /**
     * Returns the current input statuses.  This method simply calls getInputStatus(false) to get the cached result if it exists
     * @return the ScalarWebResult containing the status information for all the inputs
     * @throws IOException if an IOException occurrs getting the input status
     */
    private ScalarWebResult getInputStatus() throws IOException {
        return getInputStatus(false);
    }

    /**
     * Returns the current input status.  If refresh is false, the cached version is used (if it exists) - otherwise we query the device for the statuses
     * @param refresh true to refresh from the device, false to potentially use a cached version (if it exists)
     * @return the ScalarWebResult containing the status information for all the inputs
     * @throws IOException if an IOException occurrs getting the input status
     */
    private ScalarWebResult getInputStatus(boolean refresh) throws IOException {
        if (!refresh) {
            final ScalarWebResult rs = stateInputs.get();
            if (rs != null) {
                return rs;
            }
        }
        final ScalarWebResult res = execute(ScalarWebMethod.GETCURRENTEXTERNALINPUTSSTATUS);
        stateInputs.set(res);
        return res;
    }

    /**
     * Get's the parental rating from the device
     * @return the ScalarWebResult containing the status information for all the inputs
     * @throws IOException if an IOException occurrs getting the input status
     */
    private ScalarWebResult getParentalRating() throws IOException {
        return execute(ScalarWebMethod.GETPARENTALRATINGSETTINGS);
    }

    /**
     * Gets the playing content info
     *
     * @return the ScalarWebResult containing the status information for all the inputs
     * @throws IOException if an IOException occurrs getting the input status
     */
    private ScalarWebResult getPlayingContentInfo() throws IOException {
        return execute(ScalarWebMethod.GETPLAYINGCONTENTINFO, version -> {
            if (VersionUtilities.equals(version, ScalarWebMethod.V1_0, ScalarWebMethod.V1_1)) {
                return null;
            }
            return new PlayingContentInfoRequest_1_2("");
        });
    }

    /**
     * Returns the current set of schemes.  This method simply calls getSchemes(false) to get the cached result if it exists
     * @return a non-null, possibly empty set of schemes
     */
    private Set<Scheme> getSchemes() {
        return getSchemes(false);
    }

    /**
     * Returns the current set of schemes.  If refresh is false, the cached version is used (if it exists) - otherwise we query the device for the schemes
     * @param refresh true to refresh from the device, false to potentially use a cached version (if it exists)
     * @return a non-null, possibly empty set of schemes
     */
    private Set<Scheme> getSchemes(boolean refresh) {
        final Set<Scheme> cacheSchemes = stateSchemes.get();
        if (!cacheSchemes.isEmpty() && !refresh) {
            return cacheSchemes;
        }

        final Set<Scheme> schemes = new HashSet<>();
        try {
            for (Scheme scheme : execute(ScalarWebMethod.GETSCHEMELIST).asArray(Scheme.class)) {
                final String schemeName = scheme.getScheme();
                if (schemeName != null && StringUtils.isNotEmpty(schemeName)) {
                    schemes.add(scheme);
                }
            }
        } catch (IOException e) {
            logger.debug("Exception occurred retrieving the scheme list: {}", e.getMessage());
        }

        stateSchemes.set(schemes);
        return schemes;
    }

    /**
     * Returns the current set of sources.  This method simply calls getSources(false) to get the cached result if it exists
     * @return a non-null, possibly empty set of sources
     */
    private Set<Source> getSources() {
        return getSources(false);
    }

    /**
     * Gets list of sources for a scheme. Some schemes are not valid for specific versions of the source (like DLNA is
     * only valid 1.2+) - so query all sources by scheme and return a consolidated list
     *
     * @param refresh true to refresh from the device, false to potentially use a cached version (if it exists)
     * @return the non-null, possibly empty list of sources
     */
    private Set<Source> getSources(boolean refresh) {
        final Set<Source> sources = new HashSet<>();

        for (Scheme scheme : getSchemes()) {
            final String schemeName = scheme.getScheme();
            if (schemeName != null && StringUtils.isNotEmpty(schemeName)) {
                final Set<Source> schemeSources = stateSources.compute(schemeName, (k, v) -> {
                    if (v != null && !v.isEmpty() && !refresh) {
                        return v;
                    }

                    final Set<Source> srcs = new HashSet<>();
                    try {
                        for (final String version : getService().getVersions(ScalarWebMethod.GETSOURCELIST)) {
                            final ScalarWebResult result = getService().executeSpecific(ScalarWebMethod.GETSOURCELIST,
                                    version, scheme);

                            // This can happen if the specific version source doesn't support the scheme for
                            if (result.getDeviceErrorCode() == ScalarWebError.NOTIMPLEMENTED
                                    || result.getDeviceErrorCode() == ScalarWebError.UNSUPPORTEDOPERATION
                                    || result.getDeviceErrorCode() == ScalarWebError.ILLEGALARGUMENT) {
                                logger.trace("Source version {} for scheme {} is not implemented", version, scheme);
                            } else {
                                for (Source src : result.asArray(Source.class)) {
                                    final String sourceName = src.getSource();
                                    if (sourceName != null && StringUtils.isNotEmpty(sourceName)) {
                                        srcs.add(src);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.debug("Exception occurred retrieving the source list for scheme {}: {}", scheme,
                                e.getMessage());
                    }
                    return srcs;
                });
                sources.addAll(schemeSources);
            }
        }

        if (refresh) {
            updateTermSource();
        }

        return sources;
    }

    private void updateTermSource() {
        final List<InputSource> sources = new ArrayList<>();
        if (getService().hasMethod(ScalarWebMethod.GETCURRENTEXTERNALINPUTSSTATUS)
                && !getService().hasMethod(ScalarWebMethod.GETCURRENTEXTERNALTERMINALSSTATUS)) {
            // no need to do versioning since everything we want is in v1_0 (and v1_1 inherites from it)
            try {
                for (CurrentExternalInputsStatus_1_0 inp : getInputStatus()
                        .asArray(CurrentExternalInputsStatus_1_0.class)) {
                    final String uri = inp.getUri();
                    if (uri == null || StringUtils.isEmpty(uri)) {
                        continue;
                    }

                    final String title = inp.getTitle(uri);
                    sources.add(new InputSource(uri, title, null));
                }
            } catch (IOException e) {
                logger.debug("Error updating terminal source {}", e.getMessage());
            }
        }

        // if (sources.size() == 0) {
        stateSources.values().stream().flatMap(e -> e.stream()).forEach(src -> {
            final String uri = src.getSource();
            if (uri != null && StringUtils.isNotEmpty(uri)) {
                final String[] outputs = src.getOutputs();

                // Add the source if not duplicating an input/terminal
                // (need to use starts with because hdmi source has a port number in it and
                // the source is just hdmi [no ports])
                if (!sources.stream().anyMatch(s -> StringUtils.startsWithIgnoreCase(s.uri, uri))) {
                    sources.add(new InputSource(uri, src.getTitle(),
                            outputs == null ? new ArrayList<>() : Arrays.asList(outputs)));
                }
            }
        });
        // }

        for (CurrentExternalTerminalsStatus_1_0 term : getTerminalStatuses()) {
            if (term.isOutput()) {
                final String uri = term.getUri();
                if (uri != null && StringUtils.isNotEmpty(uri)) {
                    final List<StateOption> options = sources.stream()
                            .filter(s -> s.outputs.size() == 0 || s.outputs.contains(uri))
                            .map(s -> new StateOption(s.uri, s.title == null ? s.uri : s.title))
                            .sorted((a, b) -> ObjectUtils.compare(a.getLabel(), b.getLabel())).collect(Collectors.toList());

                    final String id = getIdForOutput(uri);
                    final ScalarWebChannel cnl = createChannel(TERM_SOURCE, id, uri);
                    final StateDescription sd = StateDescriptionFragmentBuilder.create().withOptions(options).build().toStateDescription();
                    if (sd != null) {
                        getContext().getStateProvider().addStateOverride(getContext().getThingUID(),
                            getContext().getMapper().getMappedChannelId(cnl.getChannelId()), sd);
                    }
                }
            }
        }
    }

    private ScalarWebResult getTerminalStatus() throws IOException {
        return execute(ScalarWebMethod.GETCURRENTEXTERNALTERMINALSSTATUS);
    }

    private List<CurrentExternalTerminalsStatus_1_0> getTerminalStatuses() {
        return getTerminalStatuses(false);
    }

    private List<CurrentExternalTerminalsStatus_1_0> getTerminalStatuses(boolean refresh) {
        final List<CurrentExternalTerminalsStatus_1_0> cachedTerms = stateTerminals.get();
        if (!cachedTerms.isEmpty() && !refresh) {
            return cachedTerms;
        }

        final List<CurrentExternalTerminalsStatus_1_0> terms = new ArrayList<>();
        try {
            terms.addAll(getTerminalStatus().asArray(CurrentExternalTerminalsStatus_1_0.class));
        } catch (IOException e) {
            logger.debug("Error getting terminal statuses {}", e.getMessage());
        }

        // If no outputs, create our dummy 'main' output
        if (!terms.stream().anyMatch(t -> t.isOutput())) {
            terms.add(new CurrentExternalTerminalsStatus_1_0(MAINOUTPUT, MAINTITLE));
        }

        stateTerminals.set(terms);
        return terms;
    }

    private void notifyContentListResult() {
        // Set everything to undefined except for uri, index, childcount and selected
        stateChanged(CN_ALBUMNAME, UnDefType.UNDEF);
        stateChanged(CN_APPLICATIONNAME, UnDefType.UNDEF);
        stateChanged(CN_ARTIST, UnDefType.UNDEF);
        stateChanged(CN_AUDIOCHANNEL, UnDefType.UNDEF);
        stateChanged(CN_AUDIOCODEC, UnDefType.UNDEF);
        stateChanged(CN_AUDIOFREQUENCY, UnDefType.UNDEF);
        stateChanged(CN_BIVLSERVICEID, UnDefType.UNDEF);
        stateChanged(CN_BIVLASSETID, UnDefType.UNDEF);
        stateChanged(CN_BIVLPROVIDER, UnDefType.UNDEF);
        stateChanged(CN_BROADCASTFREQ, UnDefType.UNDEF);
        stateChanged(CN_BROADCASTFREQBAND, UnDefType.UNDEF);
        stateChanged(CN_CHANNELNAME, UnDefType.UNDEF);
        stateChanged(CN_CHANNELSURFINGVISIBILITY, UnDefType.UNDEF);
        stateChanged(CN_CHAPTERCOUNT, UnDefType.UNDEF);
        stateChanged(CN_CHAPTERINDEX, UnDefType.UNDEF);
        stateChanged(CN_CLIPCOUNT, UnDefType.UNDEF);
        stateChanged(CN_CONTENTKIND, UnDefType.UNDEF);
        stateChanged(CN_CONTENTTYPE, UnDefType.UNDEF);
        stateChanged(CN_CREATEDTIME, UnDefType.UNDEF);
        stateChanged(CN_DABCOMPONENTLABEL, UnDefType.UNDEF);
        stateChanged(CN_DABDYNAMICLABEL, UnDefType.UNDEF);
        stateChanged(CN_DABENSEMBLELABEL, UnDefType.UNDEF);
        stateChanged(CN_DABSERVICELABEL, UnDefType.UNDEF);
        stateChanged(CN_DESCRIPTION, UnDefType.UNDEF);
        stateChanged(CN_DIRECTREMOTENUM, UnDefType.UNDEF);
        stateChanged(CN_DISPNUM, UnDefType.UNDEF);
        stateChanged(CN_DURATIONMSEC, UnDefType.UNDEF);
        stateChanged(CN_DURATIONSEC, UnDefType.UNDEF);
        stateChanged(CN_EPGVISIBILITY, UnDefType.UNDEF);
        stateChanged(CN_EVENTID, UnDefType.UNDEF);
        stateChanged(CN_FILENO, UnDefType.UNDEF);
        stateChanged(CN_FILESIZEBYTE, UnDefType.UNDEF);
        stateChanged(CN_FOLDERNO, UnDefType.UNDEF);
        stateChanged(CN_GENRE, UnDefType.UNDEF);
        stateChanged(CN_GLOBALPLAYBACKCOUNT, UnDefType.UNDEF);
        stateChanged(CN_HASRESUME, UnDefType.UNDEF);
        stateChanged(CN_IS3D, UnDefType.UNDEF);
        stateChanged(CN_IS4K, UnDefType.UNDEF);
        stateChanged(CN_ISALREADYPLAYED, UnDefType.UNDEF);
        stateChanged(CN_ISAUTODELETE, UnDefType.UNDEF);
        stateChanged(CN_ISBROWSABLE, UnDefType.UNDEF);
        stateChanged(CN_ISNEW, UnDefType.UNDEF);
        stateChanged(CN_ISPLAYABLE, UnDefType.UNDEF);
        stateChanged(CN_ISPLAYLIST, UnDefType.UNDEF);
        stateChanged(CN_ISPROTECTED, UnDefType.UNDEF);
        stateChanged(CN_ISSOUNDPHOTO, UnDefType.UNDEF);
        stateChanged(CN_MEDIATYPE, UnDefType.UNDEF);
        stateChanged(CN_ORIGINALDISPNUM, UnDefType.UNDEF);
        stateChanged(CN_OUTPUT, UnDefType.UNDEF);
        stateChanged(CN_PARENTALCOUNTRY, UnDefType.UNDEF);
        stateChanged(CN_PARENTALRATING, UnDefType.UNDEF);
        stateChanged(CN_PARENTALSYSTEM, UnDefType.UNDEF);
        stateChanged(CN_PARENTINDEX, UnDefType.UNDEF);
        stateChanged(CN_PATH, UnDefType.UNDEF);
        stateChanged(CN_PLAYLISTNAME, UnDefType.UNDEF);
        stateChanged(CN_PODCASTNAME, UnDefType.UNDEF);
        stateChanged(CN_PRODUCTID, UnDefType.UNDEF);
        stateChanged(CN_PROGRAMMEDIATYPE, UnDefType.UNDEF);
        stateChanged(CN_PROGRAMNUM, UnDefType.UNDEF);
        stateChanged(CN_PROGRAMSERVICETYPE, UnDefType.UNDEF);
        stateChanged(CN_PROGRAMTITLE, UnDefType.UNDEF);
        stateChanged(CN_REMOTEPLAYTYPE, UnDefType.UNDEF);
        stateChanged(CN_REPEATTYPE, UnDefType.UNDEF);
        stateChanged(CN_SERVICE, UnDefType.UNDEF);
        stateChanged(CN_SIZEMB, UnDefType.UNDEF);
        stateChanged(CN_SOURCE, UnDefType.UNDEF);
        stateChanged(CN_SOURCELABEL, UnDefType.UNDEF);
        stateChanged(CN_STATE, UnDefType.UNDEF);
        stateChanged(CN_STATESUPPLEMENT, UnDefType.UNDEF);
        stateChanged(CN_STARTDATETIME, UnDefType.UNDEF);
        stateChanged(CN_STORAGEURI, UnDefType.UNDEF);
        stateChanged(CN_SUBTITLELANGUAGE, UnDefType.UNDEF);
        stateChanged(CN_SUBTITLETITLE, UnDefType.UNDEF);
        stateChanged(CN_SYNCCONTENTPRIORITY, UnDefType.UNDEF);
        stateChanged(CN_TITLE, UnDefType.UNDEF);
        stateChanged(CN_TOTALCOUNT, UnDefType.UNDEF);
        stateChanged(CN_TRIPLETSTR, UnDefType.UNDEF);
        stateChanged(CN_USERCONTENTFLAG, UnDefType.UNDEF);
        stateChanged(CN_VIDEOCODEC, UnDefType.UNDEF);
        stateChanged(CN_VISIBILITY, UnDefType.UNDEF);
    }

    private void notifyContentListResult(ContentListResult_1_0 clr) {
        // Set everything to undefined except for uri, index, childcount and selected

        stateChanged(CN_CHANNELNAME, SonyUtil.newStringType(clr.getChannelName()));

        stateChanged(CN_DIRECTREMOTENUM, SonyUtil.newDecimalType(clr.getDirectRemoteNum()));
        stateChanged(CN_DISPNUM, SonyUtil.newStringType(clr.getDispNum()));
        stateChanged(CN_DURATIONSEC, SonyUtil.newDecimalType(clr.getDurationSec()));
        stateChanged(CN_FILESIZEBYTE, SonyUtil.newDecimalType(clr.getFileSizeByte()));
        stateChanged(CN_ISALREADYPLAYED,
                SonyUtil.newStringType(Boolean.toString(BooleanUtils.toBoolean(clr.isAlreadyPlayed()))));
        stateChanged(CN_ISPROTECTED,
                SonyUtil.newStringType(Boolean.toString(BooleanUtils.toBoolean(clr.isProtected()))));
        stateChanged(CN_ORIGINALDISPNUM, SonyUtil.newStringType(clr.getOriginalDispNum()));

        stateChanged(CN_PROGRAMMEDIATYPE, SonyUtil.newStringType(clr.getProgramMediaType()));
        stateChanged(CN_PROGRAMNUM, SonyUtil.newDecimalType(clr.getProgramNum()));

        stateChanged(CN_STARTDATETIME, SonyUtil.newStringType(clr.getStartDateTime()));

        stateChanged(CN_TITLE, SonyUtil.newStringType(clr.getTitle()));
        stateChanged(CN_TRIPLETSTR, SonyUtil.newStringType(clr.getTripletStr()));

    }

    private void notifyContentListResult(ContentListResult_1_2 clr) {
        // Set everything to undefined except for uri, index, childcount and selected

        stateChanged(CN_AUDIOCHANNEL, SonyUtil.newStringType(StringUtils.join(clr.getAudioChannel(), ',')));
        stateChanged(CN_AUDIOCODEC, SonyUtil.newStringType(StringUtils.join(clr.getAudioCodec(), ',')));
        stateChanged(CN_AUDIOFREQUENCY, SonyUtil.newStringType(StringUtils.join(clr.getAudioFrequency(), ',')));

        stateChanged(CN_CHANNELNAME, SonyUtil.newStringType(clr.getChannelName()));

        stateChanged(CN_CHANNELSURFINGVISIBILITY, SonyUtil.newStringType(clr.getChannelSurfingVisibility()));
        stateChanged(CN_EPGVISIBILITY, SonyUtil.newStringType(clr.getEpgVisibility()));
        stateChanged(CN_VISIBILITY, SonyUtil.newStringType(clr.getVisibility()));

        stateChanged(CN_CHAPTERCOUNT, SonyUtil.newDecimalType(clr.getChapterCount()));
        stateChanged(CN_CONTENTTYPE, SonyUtil.newStringType(clr.getContentType()));
        stateChanged(CN_CREATEDTIME, SonyUtil.newStringType(clr.getCreatedTime()));

        stateChanged(CN_DIRECTREMOTENUM, SonyUtil.newDecimalType(clr.getDirectRemoteNum()));
        stateChanged(CN_DISPNUM, SonyUtil.newStringType(clr.getDispNum()));
        stateChanged(CN_DURATIONSEC, SonyUtil.newDecimalType(clr.getDurationSec()));
        stateChanged(CN_FILESIZEBYTE, SonyUtil.newDecimalType(clr.getFileSizeByte()));
        stateChanged(CN_ISALREADYPLAYED,
                SonyUtil.newStringType(Boolean.toString(BooleanUtils.toBoolean(clr.isAlreadyPlayed()))));
        stateChanged(CN_ISPROTECTED,
                SonyUtil.newStringType(Boolean.toString(BooleanUtils.toBoolean(clr.isProtected()))));
        stateChanged(CN_ORIGINALDISPNUM, SonyUtil.newStringType(clr.getOriginalDispNum()));

        stateChanged(CN_PARENTALCOUNTRY, SonyUtil.newStringType(StringUtils.join(clr.getParentalCountry(), ',')));
        stateChanged(CN_PARENTALRATING, SonyUtil.newStringType(StringUtils.join(clr.getParentalRating(), ',')));
        stateChanged(CN_PARENTALSYSTEM, SonyUtil.newStringType(StringUtils.join(clr.getParentalSystem(), ',')));

        stateChanged(CN_PRODUCTID, SonyUtil.newStringType(clr.getProductID()));
        stateChanged(CN_PROGRAMMEDIATYPE, SonyUtil.newStringType(clr.getProgramMediaType()));
        stateChanged(CN_PROGRAMNUM, SonyUtil.newDecimalType(clr.getProgramNum()));
        stateChanged(CN_SIZEMB, SonyUtil.newDecimalType(clr.getSizeMB()));

        stateChanged(CN_STARTDATETIME, SonyUtil.newStringType(clr.getStartDateTime()));
        stateChanged(CN_STORAGEURI, SonyUtil.newStringType(clr.getStorageUri()));

        stateChanged(CN_SUBTITLELANGUAGE, SonyUtil.newStringType(StringUtils.join(clr.getSubtitleLanguage(), ',')));
        stateChanged(CN_SUBTITLETITLE, SonyUtil.newStringType(StringUtils.join(clr.getSubtitleTitle(), ',')));

        stateChanged(CN_TITLE, SonyUtil.newStringType(clr.getTitle()));
        stateChanged(CN_TRIPLETSTR, SonyUtil.newStringType(clr.getTripletStr()));
        stateChanged(CN_USERCONTENTFLAG, SonyUtil.newBooleanType(clr.isUserContentFlag()));

        stateChanged(CN_VIDEOCODEC, SonyUtil.newStringType(clr.getVideoCodec()));
    }

    private void notifyContentListResult(ContentListResult_1_4 clr) {
        // Set everything to undefined except for uri, index, childcount and selected
        stateChanged(CN_ALBUMNAME, SonyUtil.newStringType(clr.getAlbumName()));
        stateChanged(CN_ARTIST, SonyUtil.newStringType(clr.getArtist()));

        final AudioInfo[] audioInfo = clr.getAudioInfo();
        if (audioInfo != null) {
            stateChanged(CN_AUDIOCHANNEL, SonyUtil.newStringType(
                    Arrays.stream(audioInfo).map(ai -> ai.getChannel()).collect(Collectors.joining(","))));
            stateChanged(CN_AUDIOCODEC, SonyUtil
                    .newStringType(Arrays.stream(audioInfo).map(ai -> ai.getCodec()).collect(Collectors.joining(","))));
            stateChanged(CN_AUDIOFREQUENCY, SonyUtil.newStringType(
                    Arrays.stream(audioInfo).map(ai -> ai.getFrequency()).collect(Collectors.joining(","))));
        }

        stateChanged(CN_BROADCASTFREQ, SonyUtil.newDecimalType(clr.getBroadcastFreq()));
        stateChanged(CN_BROADCASTFREQBAND, SonyUtil.newStringType(clr.getBroadcastFreqBand()));
        stateChanged(CN_CHANNELNAME, SonyUtil.newStringType(clr.getChannelName()));

        stateChanged(CN_CHANNELSURFINGVISIBILITY, SonyUtil.newStringType(clr.getChannelSurfingVisibility()));
        stateChanged(CN_EPGVISIBILITY, SonyUtil.newStringType(clr.getEpgVisibility()));
        stateChanged(CN_VISIBILITY, SonyUtil.newStringType(clr.getVisibility()));

        stateChanged(CN_CHAPTERCOUNT, SonyUtil.newDecimalType(clr.getChapterCount()));
        stateChanged(CN_CONTENTKIND, SonyUtil.newStringType(clr.getContentKind()));
        stateChanged(CN_CONTENTTYPE, SonyUtil.newStringType(clr.getContentType()));
        stateChanged(CN_CREATEDTIME, SonyUtil.newStringType(clr.getCreatedTime()));

        stateChanged(CN_DIRECTREMOTENUM, SonyUtil.newDecimalType(clr.getDirectRemoteNum()));
        stateChanged(CN_DISPNUM, SonyUtil.newStringType(clr.getDispNum()));
        stateChanged(CN_DURATIONMSEC, SonyUtil.newDecimalType(clr.getDurationMSec()));
        stateChanged(CN_FILENO, SonyUtil.newStringType(clr.getFileNo()));
        stateChanged(CN_FILESIZEBYTE, SonyUtil.newDecimalType(clr.getFileSizeByte()));
        stateChanged(CN_FOLDERNO, SonyUtil.newStringType(clr.getFolderNo()));
        stateChanged(CN_GENRE, SonyUtil.newStringType(clr.getGenre()));
        stateChanged(CN_IS3D, SonyUtil.newStringType(clr.is3D()));
        stateChanged(CN_ISALREADYPLAYED, SonyUtil.newStringType(clr.isAlreadyPlayed()));
        stateChanged(CN_ISBROWSABLE, SonyUtil.newStringType(clr.isBrowsable()));
        stateChanged(CN_ISPLAYABLE, SonyUtil.newStringType(clr.isPlayable()));
        stateChanged(CN_ISPROTECTED, SonyUtil.newStringType(clr.isProtected()));
        stateChanged(CN_ORIGINALDISPNUM, SonyUtil.newStringType(clr.getOriginalDispNum()));

        final ParentalInfo[] pis = clr.getParentalInfo();
        if (pis != null) {
            stateChanged(CN_PARENTALCOUNTRY, SonyUtil
                    .newStringType(Arrays.stream(pis).map(pi -> pi.getCountry()).collect(Collectors.joining(","))));
            stateChanged(CN_PARENTALRATING, SonyUtil
                    .newStringType(Arrays.stream(pis).map(pi -> pi.getRating()).collect(Collectors.joining(","))));
            stateChanged(CN_PARENTALSYSTEM, SonyUtil
                    .newStringType(Arrays.stream(pis).map(pi -> pi.getSystem()).collect(Collectors.joining(","))));
        }
        stateChanged(CN_PARENTINDEX, SonyUtil.newDecimalType(clr.getParentIndex()));
        stateChanged(CN_PATH, SonyUtil.newStringType(clr.getPath()));
        stateChanged(CN_PLAYLISTNAME, SonyUtil.newStringType(clr.getPlaylistName()));
        stateChanged(CN_PODCASTNAME, SonyUtil.newStringType(clr.getPodcastName()));
        stateChanged(CN_PRODUCTID, SonyUtil.newStringType(clr.getProductID()));
        stateChanged(CN_PROGRAMMEDIATYPE, SonyUtil.newStringType(clr.getProgramMediaType()));
        stateChanged(CN_PROGRAMNUM, SonyUtil.newDecimalType(clr.getProgramNum()));
        stateChanged(CN_REMOTEPLAYTYPE, SonyUtil.newStringType(clr.getRemotePlayType()));
        stateChanged(CN_SIZEMB, SonyUtil.newDecimalType(clr.getSizeMB()));

        stateChanged(CN_STARTDATETIME, SonyUtil.newStringType(clr.getStartDateTime()));
        stateChanged(CN_STORAGEURI, SonyUtil.newStringType(clr.getStorageUri()));

        final SubtitleInfo[] subInfos = clr.getSubtitleInfo();
        if (subInfos != null) {
            stateChanged(CN_SUBTITLELANGUAGE, SonyUtil.newStringType(
                    Arrays.stream(subInfos).map(subi -> subi.getLangauge()).collect(Collectors.joining(","))));
            stateChanged(CN_SUBTITLETITLE, SonyUtil.newStringType(
                    Arrays.stream(subInfos).map(subi -> subi.getTitle()).collect(Collectors.joining(","))));
        }
        stateChanged(CN_TITLE, SonyUtil.newStringType(clr.getTitle()));
        stateChanged(CN_TRIPLETSTR, SonyUtil.newStringType(clr.getTripletStr()));
        stateChanged(CN_USERCONTENTFLAG, SonyUtil.newBooleanType(clr.getUserContentFlag()));

        final VideoInfo vis = clr.getVideoInfo();
        if (vis != null) {
            stateChanged(CN_VIDEOCODEC, SonyUtil.newStringType(vis.getCodec()));
        }
    }

    private void notifyContentListResult(ContentListResult_1_5 clr) {
        // Set everything to undefined except for uri, index, childcount and selected
        stateChanged(CN_ALBUMNAME, SonyUtil.newStringType(clr.getAlbumName()));
        stateChanged(CN_APPLICATIONNAME, SonyUtil.newStringType(clr.getApplicationName()));
        stateChanged(CN_ARTIST, SonyUtil.newStringType(clr.getArtist()));

        final AudioInfo[] audioInfo = clr.getAudioInfo();
        if (audioInfo != null) {
            stateChanged(CN_AUDIOCHANNEL, SonyUtil.newStringType(
                    Arrays.stream(audioInfo).map(ai -> ai.getChannel()).collect(Collectors.joining(","))));
            stateChanged(CN_AUDIOCODEC, SonyUtil
                    .newStringType(Arrays.stream(audioInfo).map(ai -> ai.getCodec()).collect(Collectors.joining(","))));
            stateChanged(CN_AUDIOFREQUENCY, SonyUtil.newStringType(
                    Arrays.stream(audioInfo).map(ai -> ai.getFrequency()).collect(Collectors.joining(","))));
        }

        final BivlInfo bivlInfo = clr.getBivlInfo();
        if (bivlInfo != null) {
            stateChanged(CN_BIVLSERVICEID, SonyUtil.newStringType(bivlInfo.getServiceId()));
            stateChanged(CN_BIVLASSETID, SonyUtil.newStringType(bivlInfo.getAssetId()));
            stateChanged(CN_BIVLPROVIDER, SonyUtil.newStringType(bivlInfo.getProvider()));
        }

        final BroadcastFreq bf = clr.getBroadcastFreq();
        if (bf != null) {
            stateChanged(CN_BROADCASTFREQ, SonyUtil.newDecimalType(bf.getFrequency()));
            stateChanged(CN_BROADCASTFREQBAND, SonyUtil.newStringType(bf.getBand()));
        }
        stateChanged(CN_CHANNELNAME, SonyUtil.newStringType(clr.getChannelName()));

        final Visibility visibility = clr.getVisibility();
        if (visibility != null) {
            stateChanged(CN_CHANNELSURFINGVISIBILITY, SonyUtil.newStringType(visibility.getChannelSurfingVisibility()));
            stateChanged(CN_EPGVISIBILITY, SonyUtil.newStringType(visibility.getEpgVisibility()));
            stateChanged(CN_VISIBILITY, SonyUtil.newStringType(visibility.getVisibility()));
        }

        stateChanged(CN_CHAPTERCOUNT, SonyUtil.newDecimalType(clr.getChapterCount()));
        stateChanged(CN_CHAPTERINDEX, SonyUtil.newDecimalType(clr.getChapterIndex()));
        stateChanged(CN_CLIPCOUNT, SonyUtil.newDecimalType(clr.getClipCount()));
        stateChanged(CN_CONTENTKIND, SonyUtil.newStringType(clr.getContentKind()));
        stateChanged(CN_CONTENTTYPE, SonyUtil.newStringType(clr.getContentType()));
        stateChanged(CN_CREATEDTIME, SonyUtil.newStringType(clr.getCreatedTime()));

        final DabInfo dab = clr.getDabInfo();
        if (dab != null) {
            stateChanged(CN_DABCOMPONENTLABEL, SonyUtil.newStringType(dab.getComponentLabel()));
            stateChanged(CN_DABDYNAMICLABEL, SonyUtil.newStringType(dab.getDynamicLabel()));
            stateChanged(CN_DABENSEMBLELABEL, SonyUtil.newStringType(dab.getEnsembleLabel()));
            stateChanged(CN_DABSERVICELABEL, SonyUtil.newStringType(dab.getServiceLabel()));
        }

        final Description desc = clr.getDescription();
        if (desc != null) {
            // stateChanged(CN_DESCRIPTION, SonyUtil.newStringType(desc.));
        }
        stateChanged(CN_DIRECTREMOTENUM, SonyUtil.newDecimalType(clr.getDirectRemoteNum()));
        stateChanged(CN_DISPNUM, SonyUtil.newStringType(clr.getDispNum()));

        final Duration duration = clr.getDuration();
        if (duration != null) {
            stateChanged(CN_DURATIONMSEC, SonyUtil.newDecimalType(duration.getSeconds()));
            stateChanged(CN_DURATIONSEC, SonyUtil.newDecimalType(duration.getMillseconds()));
        }

        stateChanged(CN_EVENTID, SonyUtil.newStringType(clr.getEventId()));
        stateChanged(CN_FILENO, SonyUtil.newStringType(clr.getFileNo()));
        stateChanged(CN_FILESIZEBYTE, SonyUtil.newDecimalType(clr.getFileSizeByte()));
        stateChanged(CN_FOLDERNO, SonyUtil.newStringType(clr.getFolderNo()));
        stateChanged(CN_GENRE, SonyUtil.newStringType(clr.getGenre()));
        stateChanged(CN_GLOBALPLAYBACKCOUNT, SonyUtil.newDecimalType(clr.getGlobalPlaybackCount()));
        stateChanged(CN_HASRESUME, SonyUtil.newStringType(clr.getHasResume()));
        stateChanged(CN_IS3D, SonyUtil.newStringType(clr.is3D()));
        stateChanged(CN_IS4K, SonyUtil.newStringType(clr.is4K()));
        stateChanged(CN_ISALREADYPLAYED, SonyUtil.newStringType(clr.isAlreadyPlayed()));
        stateChanged(CN_ISAUTODELETE, SonyUtil.newStringType(clr.isAutoDelete()));
        stateChanged(CN_ISBROWSABLE, SonyUtil.newStringType(clr.isBrowsable()));
        stateChanged(CN_ISNEW, SonyUtil.newStringType(clr.isNew()));
        stateChanged(CN_ISPLAYABLE, SonyUtil.newStringType(clr.isPlayable()));
        stateChanged(CN_ISPLAYLIST, SonyUtil.newStringType(clr.isPlaylist()));
        stateChanged(CN_ISPROTECTED, SonyUtil.newStringType(clr.isProtected()));
        stateChanged(CN_ISSOUNDPHOTO, SonyUtil.newStringType(clr.isSoundPhoto()));
        stateChanged(CN_MEDIATYPE, SonyUtil.newStringType(clr.getMediaType()));
        stateChanged(CN_ORIGINALDISPNUM, SonyUtil.newStringType(clr.getOriginalDispNum()));
        stateChanged(CN_OUTPUT, SonyUtil.newStringType(clr.getOutput()));

        final ParentalInfo[] pis = clr.getParentalInfo();
        if (pis != null) {
            stateChanged(CN_PARENTALCOUNTRY, SonyUtil
                    .newStringType(Arrays.stream(pis).map(pi -> pi.getCountry()).collect(Collectors.joining(","))));
            stateChanged(CN_PARENTALRATING, SonyUtil
                    .newStringType(Arrays.stream(pis).map(pi -> pi.getRating()).collect(Collectors.joining(","))));
            stateChanged(CN_PARENTALSYSTEM, SonyUtil
                    .newStringType(Arrays.stream(pis).map(pi -> pi.getSystem()).collect(Collectors.joining(","))));
        }
        stateChanged(CN_PARENTINDEX, SonyUtil.newDecimalType(clr.getParentIndex()));
        stateChanged(CN_PLAYLISTNAME, SonyUtil.newStringType(clr.getPlaylistName()));
        stateChanged(CN_PODCASTNAME, SonyUtil.newStringType(clr.getPodcastName()));
        stateChanged(CN_PRODUCTID, SonyUtil.newStringType(clr.getProductID()));
        stateChanged(CN_PROGRAMMEDIATYPE, SonyUtil.newStringType(clr.getProgramMediaType()));
        stateChanged(CN_PROGRAMNUM, SonyUtil.newDecimalType(clr.getProgramNum()));
        stateChanged(CN_PROGRAMSERVICETYPE, SonyUtil.newStringType(clr.getProgramServiceType()));
        stateChanged(CN_PROGRAMTITLE, SonyUtil.newStringType(clr.getProgramTitle()));
        stateChanged(CN_REMOTEPLAYTYPE, SonyUtil.newStringType(clr.getRemotePlayType()));
        stateChanged(CN_REPEATTYPE, SonyUtil.newStringType(clr.getRepeatType()));
        stateChanged(CN_SERVICE, SonyUtil.newStringType(clr.getService()));
        stateChanged(CN_SIZEMB, SonyUtil.newDecimalType(clr.getSizeMB()));
        stateChanged(CN_SOURCE, SonyUtil.newStringType(clr.getSource()));
        stateChanged(CN_SOURCELABEL, SonyUtil.newStringType(clr.getSourceLabel()));

        final StateInfo si = clr.getStateInfo();
        if (si != null) {
            stateChanged(CN_STATE, SonyUtil.newStringType(si.getState()));
            stateChanged(CN_STATESUPPLEMENT, SonyUtil.newStringType(si.getSupplement()));
        }
        stateChanged(CN_STARTDATETIME, SonyUtil.newStringType(clr.getStartDateTime()));
        stateChanged(CN_STORAGEURI, SonyUtil.newStringType(clr.getStorageUri()));

        final SubtitleInfo[] subInfos = clr.getSubtitleInfo();
        if (subInfos != null) {
            stateChanged(CN_SUBTITLELANGUAGE, SonyUtil.newStringType(
                    Arrays.stream(subInfos).map(subi -> subi.getLangauge()).collect(Collectors.joining(","))));
            stateChanged(CN_SUBTITLETITLE, SonyUtil.newStringType(
                    Arrays.stream(subInfos).map(subi -> subi.getTitle()).collect(Collectors.joining(","))));
        }
        stateChanged(CN_SYNCCONTENTPRIORITY, SonyUtil.newStringType(clr.getSyncContentPriority()));
        stateChanged(CN_TITLE, SonyUtil.newStringType(clr.getTitle()));
        stateChanged(CN_TOTALCOUNT, SonyUtil.newDecimalType(clr.getTotalCount()));
        stateChanged(CN_TRIPLETSTR, SonyUtil.newStringType(clr.getTripletStr()));
        stateChanged(CN_USERCONTENTFLAG, SonyUtil.newBooleanType(clr.getUserContentFlag()));

        final VideoInfo[] vis = clr.getVideoInfo();
        if (vis != null) {
            stateChanged(CN_VIDEOCODEC, SonyUtil
                    .newStringType(Arrays.stream(vis).map(vi -> vi.getCodec()).collect(Collectors.joining(","))));
        }
    }

    private void notifyCurrentTerminalStatus(CurrentExternalTerminalsStatus_1_0 term) {
        final String termUri = term.getUri();
        for (ScalarWebChannel chnl : getChannelTracker()
                .getLinkedChannelsForCategory(TERM_URI, TERM_TITLE, TERM_CONNECTION, TERM_LABEL, TERM_ICON, TERM_ACTIVE)
                .stream().toArray(ScalarWebChannel[]::new)) {
            if (StringUtils.equalsIgnoreCase(termUri, chnl.getPathPart(0))) {
                notifyCurrentTerminalStatus(chnl, term);
            }
        }
    }

    private void notifyCurrentTerminalStatus(ScalarWebChannel channel) {
        try {
            for (CurrentExternalTerminalsStatus_1_0 term : execute(ScalarWebMethod.GETCURRENTEXTERNALTERMINALSSTATUS)
                    .asArray(CurrentExternalTerminalsStatus_1_0.class)) {
                final String termUri = term.getUri();
                if (StringUtils.equalsIgnoreCase(termUri, channel.getPathPart(0))) {
                    notifyCurrentTerminalStatus(channel, term);
                }
            }
        } catch (IOException e) {
            logger.debug("Error notify current terminal status {}", e.getMessage());
        }
    }

    private void notifyCurrentTerminalStatus(ScalarWebChannel channel, CurrentExternalTerminalsStatus_1_0 cets) {
        final String id = channel.getId();
        stateChanged(TERM_TITLE, id, SonyUtil.newStringType(cets.getTitle()));
        stateChanged(TERM_CONNECTION, id, SonyUtil.newStringType(cets.getConnection()));
        stateChanged(TERM_LABEL, id, SonyUtil.newStringType(cets.getLabel()));
        stateChanged(TERM_ICON, id, SonyUtil.newStringType(cets.getIconUrl()));
        stateChanged(TERM_ACTIVE, id, SonyUtil.newStringType(cets.getActive()));
    }

    private void notifyInputStatus(ScalarWebChannel channel, CurrentExternalInputsStatus_1_0 status) {
        final String id = channel.getId();
        stateChanged(IN_TITLE, id, SonyUtil.newStringType(status.getTitle()));
        stateChanged(IN_CONNECTION, id, status.isConnection() ? OnOffType.ON : OnOffType.OFF);
        stateChanged(IN_LABEL, id, SonyUtil.newStringType(status.getLabel()));
        stateChanged(IN_ICON, id, SonyUtil.newStringType(status.getIcon()));
    }

    private void notifyInputStatus(ScalarWebChannel channel, CurrentExternalInputsStatus_1_1 status) {
        notifyInputStatus(channel, (CurrentExternalInputsStatus_1_0) status);

        final String id = channel.getId();
        stateChanged(IN_STATUS, id, SonyUtil.newStringType(status.getStatus()));
    }

    private void notifyParentalRating(ParentalRatingSetting_1_0 prs) {
        stateChanged(PR_RATINGTYPEAGE, SonyUtil.newDecimalType(prs.getRatingTypeAge()));
        stateChanged(PR_RATINGTYPESONY, SonyUtil.newStringType(prs.getRatingTypeSony()));
        stateChanged(PR_RATINGCOUNTRY, SonyUtil.newStringType(prs.getRatingCountry()));
        stateChanged(PR_RATINGCUSTOMTYPETV, SonyUtil.newStringType(StringUtils.join(prs.getRatingCustomTypeTV())));
        stateChanged(PR_RATINGCUSTOMTYPEMPAA, SonyUtil.newStringType(prs.getRatingCustomTypeMpaa()));
        stateChanged(PR_RATINGCUSTOMTYPECAENGLISH, SonyUtil.newStringType(prs.getRatingCustomTypeCaEnglish()));
        stateChanged(PR_RATINGCUSTOMTYPECAFRENCH, SonyUtil.newStringType(prs.getRatingCustomTypeCaFrench()));
        stateChanged(PR_UNRATEDLOCK, prs.isUnratedLock() ? OnOffType.ON : OnOffType.OFF);
    }

    private void notifyPlayingContentInfo(PlayingContentInfoResult_1_0 pci, String id) {
        stateChanged(PL_BIVLASSETID, id, SonyUtil.newStringType(pci.getBivlAssetId()));
        stateChanged(PL_BIVLPROVIDER, id, SonyUtil.newStringType(pci.getBivlProvider()));
        stateChanged(PL_BIVLSERVICEID, id, SonyUtil.newStringType(pci.getBivlServiceId()));
        stateChanged(PL_DISPNUM, id, SonyUtil.newStringType(pci.getDispNum()));
        stateChanged(PL_DURATIONSEC, id, SonyUtil.newDecimalType(pci.getDurationSec()));
        stateChanged(PL_MEDIATYPE, id, SonyUtil.newStringType(pci.getMediaType()));
        stateChanged(PL_ORIGINALDISPNUM, id, SonyUtil.newStringType(pci.getOriginalDispNum()));
        stateChanged(PL_PLAYSPEED, id, SonyUtil.newStringType(pci.getPlaySpeed()));
        stateChanged(PL_PROGRAMNUM, id, SonyUtil.newDecimalType(pci.getProgramNum()));
        stateChanged(PL_PROGRAMTITLE, id, SonyUtil.newStringType(pci.getProgramTitle()));
        stateChanged(PL_SOURCE, id, SonyUtil.newStringType(pci.getSource()));
        stateChanged(PL_STARTDATETIME, id, SonyUtil.newStringType(pci.getStartDateTime()));
        stateChanged(PL_TITLE, id, SonyUtil.newStringType(pci.getTitle()));
        stateChanged(PL_TRIPLETSTR, id, SonyUtil.newStringType(pci.getTripletStr()));

        final String sourceUri = pci.getUri();
        if (sourceUri != null && StringUtils.isNotEmpty(sourceUri)) {
            // statePlaying.put(output, new PlayingState(sourceUri, preset));
            statePlaying.compute(id, (k, v) -> {
                if (v == null) {
                    int preset = 1;
                    final Matcher m = Source.RADIOPATTERN.matcher(sourceUri);
                    if (m.matches()) {
                        try {
                            preset = Integer.parseInt(m.group(2));
                        } catch (NumberFormatException e) {
                            logger.debug("Radio preset number is not a valid number: {}", sourceUri);
                        }
                    }
                    return new PlayingState(sourceUri, preset);
                } else {
                    return new PlayingState(sourceUri, v.preset);
                }
            });
            stateChanged(PL_URI, id, SonyUtil.newStringType(sourceUri));
            // stateChanged(PL_PRESET, id, SonyUtil.newDecimalType(preset));

            stateChanged(TERM_SOURCE, id, SonyUtil.newStringType(getSourceFromUri(sourceUri)));
        }
    }

    private void notifyPlayingContentInfo(PlayingContentInfoResult_1_2 pci, String id) {
        notifyPlayingContentInfo((PlayingContentInfoResult_1_0) pci, id);

        stateChanged(PL_ALBUMNAME, id, SonyUtil.newStringType(pci.getAlbumName()));
        stateChanged(PL_APPLICATIONNAME, id, SonyUtil.newStringType(pci.getApplicationName()));
        stateChanged(PL_ARTIST, id, SonyUtil.newStringType(pci.getArtist()));

        final AudioInfo[] ais = pci.getAudioInfo();
        stateChanged(PL_AUDIOCHANNEL, id, SonyUtil.newStringType(
                ais == null ? null : Arrays.stream(ais).map(a -> a.getChannel()).collect(Collectors.joining(","))));
        stateChanged(PL_AUDIOCODEC, id, SonyUtil.newStringType(
                ais == null ? null : Arrays.stream(ais).map(a -> a.getCodec()).collect(Collectors.joining(","))));
        stateChanged(PL_AUDIOFREQUENCY, id, SonyUtil.newStringType(
                ais == null ? null : Arrays.stream(ais).map(a -> a.getFrequency()).collect(Collectors.joining(","))));

        stateChanged(PL_BROADCASTFREQ, id, SonyUtil.newDecimalType(pci.getBroadcastFreq()));
        stateChanged(PL_BROADCASTFREQBAND, id, SonyUtil.newStringType(pci.getBroadcastFreqBand()));
        stateChanged(PL_CHANNELNAME, id, SonyUtil.newStringType(pci.getChannelName()));
        stateChanged(PL_CHAPTERCOUNT, id, SonyUtil.newDecimalType(pci.getChapterCount()));
        stateChanged(PL_CHAPTERINDEX, id, SonyUtil.newDecimalType(pci.getChapterIndex()));
        stateChanged(PL_CONTENTKIND, id, SonyUtil.newStringType(pci.getContentKind()));

        final DabInfo di = pci.getDabInfo();
        stateChanged(PL_DABCOMPONENTLABEL, id, SonyUtil.newStringType(di == null ? null : di.getComponentLabel()));
        stateChanged(PL_DABDYNAMICLABEL, id, SonyUtil.newStringType(di == null ? null : di.getDynamicLabel()));
        stateChanged(PL_DABENSEMBLELABEL, id, SonyUtil.newStringType(di == null ? null : di.getEnsembleLabel()));
        stateChanged(PL_DABSERVICELABEL, id, SonyUtil.newStringType(di == null ? null : di.getServiceLabel()));

        stateChanged(PL_DURATIONMSEC, id, SonyUtil.newDecimalType(pci.getDurationMsec()));
        stateChanged(PL_FILENO, id, SonyUtil.newStringType(pci.getFileNo()));
        stateChanged(PL_GENRE, id, SonyUtil.newStringType(pci.getGenre()));
        stateChanged(PL_INDEX, id, SonyUtil.newDecimalType(pci.getIndex()));
        stateChanged(PL_IS3D, id, SonyUtil.newStringType(pci.getIs3D()));
        stateChanged(PL_OUTPUT, id, SonyUtil.newStringType(pci.getOutput()));
        stateChanged(PL_PARENTINDEX, id, SonyUtil.newDecimalType(pci.getParentIndex()));
        stateChanged(PL_PARENTURI, id, SonyUtil.newStringType(pci.getParentUri()));
        stateChanged(PL_PATH, id, SonyUtil.newStringType(pci.getPath()));
        stateChanged(PL_PLAYLISTNAME, id, SonyUtil.newStringType(pci.getPlaylistName()));
        stateChanged(PL_PLAYSTEPSPEED, id, SonyUtil.newDecimalType(pci.getPlayStepSpeed()));
        stateChanged(PL_PODCASTNAME, id, SonyUtil.newStringType(pci.getPodcastName()));
        stateChanged(PL_POSITIONMSEC, id, SonyUtil.newDecimalType(pci.getPositionMsec()));
        stateChanged(PL_POSITIONSEC, id, SonyUtil.newDecimalType(pci.getPositionSec()));
        stateChanged(PL_REPEATTYPE, id, SonyUtil.newStringType(pci.getRepeatType()));
        stateChanged(PL_SERVICE, id, SonyUtil.newStringType(pci.getService()));
        stateChanged(PL_SOURCELABEL, id, SonyUtil.newStringType(pci.getSourceLabel()));

        final StateInfo si = pci.getStateInfo();
        stateChanged(PL_STATE, id, SonyUtil.newStringType(si == null ? null : si.getState()));
        stateChanged(PL_STATESUPPLEMENT, id, SonyUtil.newStringType(si == null ? null : si.getSupplement()));

        stateChanged(PL_SUBTITLEINDEX, id, SonyUtil.newDecimalType(pci.getSubtitleIndex()));
        stateChanged(PL_TOTALCOUNT, id, SonyUtil.newDecimalType(pci.getTotalCount()));

        final VideoInfo vi = pci.getVideoInfo();
        stateChanged(PL_VIDEOCODEC, id, SonyUtil.newStringType(vi == null ? null : vi.getCodec()));
    }

    @Override
    public void refreshChannel(ScalarWebChannel channel) {
        Objects.requireNonNull(channel, "channel cannot be null");

        final String ctgy = channel.getCategory();
        if (StringUtils.equalsIgnoreCase(ctgy, SCHEMES)) {
            refreshSchemes();
        } else if (StringUtils.equalsIgnoreCase(ctgy, SOURCES)) {
            refreshSources();
        } else if (StringUtils.startsWith(ctgy, PARENTRATING)) {
            refreshParentalRating();
            refreshParentalRating();
        } else if (StringUtils.startsWith(ctgy, PLAYING)) {
            refreshPlayingContentInfo();
        } else if (StringUtils.startsWith(ctgy, INPUT)) {
            refreshCurrentExternalInputStatus(Collections.singletonList(channel));
        } else if (StringUtils.startsWith(ctgy, TERM)) {
            notifyCurrentTerminalStatus(channel);
        } else if (StringUtils.startsWith(ctgy, CONTENT)) {
            refreshContent();
        } else if (StringUtils.equalsIgnoreCase(ctgy, BLUETOOTHSETTINGS)) {
            refreshGeneralSettings(Collections.singletonList(channel), ScalarWebMethod.GETBLUETOOTHSETTINGS,
                    BLUETOOTHSETTINGS);
        } else if (StringUtils.equalsIgnoreCase(ctgy, PLAYBACKSETTINGS)) {
            refreshGeneralSettings(Collections.singletonList(channel), ScalarWebMethod.GETPLAYBACKMODESETTINGS,
                    PLAYBACKSETTINGS);
        } else {
            logger.debug("Unknown refresh channel: {}", channel);
        }
    }

    /**
     * Refresh content
     *
     * @param id the non-null, non-empty channel id
     * @param si the non-null source index
     */
    private void refreshContent() {
        final ContentState state = stateContent.get();

        if (StringUtils.isEmpty(state.parentUri)) {
            notifyContentListResult();
        } else {
            Count ct;

            try {
                ct = execute(ScalarWebMethod.GETCONTENTCOUNT, version -> {
                    if (VersionUtilities.equals(version, ScalarWebMethod.V1_0, ScalarWebMethod.V1_1,
                            ScalarWebMethod.V1_2)) {
                        return new ContentCount_1_0(state.parentUri);
                    }
                    return new ContentCount_1_3(state.parentUri);
                }).as(Count.class);

            } catch (IOException e) {
                ct = new Count(-1);
            }

            // update child count
            stateChanged(CN_PARENTURI, SonyUtil.newStringType(state.parentUri));
            stateChanged(CN_CHILDCOUNT, SonyUtil.newDecimalType(ct.getCount()));

            try {
                final ScalarWebResult res = execute(ScalarWebMethod.GETCONTENTLIST, version -> {
                    if (VersionUtilities.equals(version, ScalarWebMethod.V1_0, ScalarWebMethod.V1_1,
                            ScalarWebMethod.V1_2, ScalarWebMethod.V1_3)) {
                        return new ContentListRequest_1_0(state.parentUri, state.idx, 1);
                    }
                    return new ContentListRequest_1_4(state.parentUri, state.idx, 1);
                });

                String childUri = null;
                Integer childIdx = null;

                // For USB - if you ask for 1, you'll always get two results
                // 1. The actual index you asked for
                // 2. A row describing the storage itself (idx = -1)
                // so we need to filter for just our result
                final String version = getVersion(ScalarWebMethod.GETCONTENTLIST);
                if (VersionUtilities.equals(version, ScalarWebMethod.V1_0, ScalarWebMethod.V1_1)) {
                    for (ContentListResult_1_0 clr : res.asArray(ContentListResult_1_0.class)) {
                        if (clr.getIndex() == state.idx) {
                            notifyContentListResult(clr);
                            childUri = clr.getUri();
                            childIdx = clr.getIndex();
                        }
                    }
                } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_2, ScalarWebMethod.V1_3)) {
                    for (ContentListResult_1_2 clr : res.asArray(ContentListResult_1_2.class)) {
                        if (clr.getIndex() == state.idx) {
                            notifyContentListResult(clr);
                            childUri = clr.getUri();
                            childIdx = clr.getIndex();
                        }
                    }
                } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_4)) {
                    for (ContentListResult_1_4 clr : res.asArray(ContentListResult_1_4.class)) {
                        if (clr.getIndex() == state.idx) {
                            notifyContentListResult(clr);
                            childUri = clr.getUri();
                            childIdx = clr.getIndex();
                        }
                    }
                } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_5)) {
                    for (ContentListResult_1_5 clr : res.asArray(ContentListResult_1_5.class)) {
                        if (clr.getIndex() == state.idx) {
                            notifyContentListResult(clr);
                            childUri = clr.getUri();
                            childIdx = clr.getIndex();
                        }
                    }
                }

                if (childIdx != null && childUri != null) {
                    final String finalUri = childUri;
                    final Integer finalIdx = childIdx;
                    stateContent.updateAndGet(cs -> new ContentState(cs.parentUri, finalUri, finalIdx));
                }
                stateChanged(CN_URI, SonyUtil.newStringType(childUri));
                stateChanged(CN_INDEX, SonyUtil.newDecimalType(state.idx));

            } catch (IOException e) {
                notifyContentListResult();
            }
        }
    }

    /**
     * Refresh current external input status
     *
     * @param id the non-null, non-empty channel id
     */
    private void refreshCurrentExternalInputStatus(List<ScalarWebChannel> channels) {
        try {
            final ScalarWebResult result = getInputStatus();
            final String version = getService().getVersion(ScalarWebMethod.GETCURRENTEXTERNALINPUTSSTATUS);

            if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
                for (CurrentExternalInputsStatus_1_0 inp : result.asArray(CurrentExternalInputsStatus_1_0.class)) {
                    final String inpUri = inp.getUri();
                    for (ScalarWebChannel chnl : channels) {
                        if (StringUtils.equalsIgnoreCase(inpUri, chnl.getPathPart(0))) {
                            notifyInputStatus(chnl, inp);
                        }
                    }
                }
            } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_1)) {
                for (CurrentExternalInputsStatus_1_1 inp : result.asArray(CurrentExternalInputsStatus_1_1.class)) {
                    final String inpUri = inp.getUri();
                    for (ScalarWebChannel chnl : channels) {
                        if (StringUtils.equalsIgnoreCase(inpUri, chnl.getPathPart(0))) {
                            notifyInputStatus(chnl, inp);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("Error refreshing current external input status {}", e.getMessage());
        }
    }

    /**
     * Refresh current external input status
     *
     * @param id the non-null, non-empty channel id
     */
    private void refreshCurrentExternalTerminalsStatus() {
        if (getService().hasMethod(ScalarWebMethod.GETCURRENTEXTERNALTERMINALSSTATUS)) {
            for (CurrentExternalTerminalsStatus_1_0 term : getTerminalStatuses(true)) {
                notifyCurrentTerminalStatus(term);
            }
        }
    }

    /**
     * Refresh the parental rating
     */
    private void refreshParentalRating() {
        try {
            notifyParentalRating(getParentalRating().as(ParentalRatingSetting_1_0.class));
        } catch (IOException e) {
            logger.debug("Exception occurred retrieving the parental rating setting: {}", e.getMessage());
        }
    }

    /**
     * Refresh the playing content info
     */
    private void refreshPlayingContentInfo() {
        try {
            final ScalarWebResult result = getPlayingContentInfo();
            final String version = getService().getVersion(ScalarWebMethod.GETPLAYINGCONTENTINFO);
            if (VersionUtilities.equals(version, ScalarWebMethod.V1_0, ScalarWebMethod.V1_1)) {
                for (PlayingContentInfoResult_1_0 res : result.asArray(PlayingContentInfoResult_1_0.class)) {
                    notifyPlayingContentInfo(res, getIdForOutput(MAINOUTPUT));
                }
            } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_2)) {
                for (PlayingContentInfoResult_1_2 res : result.asArray(PlayingContentInfoResult_1_2.class)) {
                    final String output = res.getOutput(MAINOUTPUT);
                    notifyPlayingContentInfo(res, getIdForOutput(output));
                }
            }
        } catch (IOException e) {
            logger.debug("Error refreshing playing content info {}", e.getMessage());
        }
    }

    private void refreshSchemes() {
        final String schemes = getSchemes(true).stream().map(s -> s.getScheme()).collect(Collectors.joining(","));
        stateSources.clear(); // clear sources to reretrieve them since schemes changed
        stateChanged(SCHEMES, SonyUtil.newStringType(schemes));
    }

    private void refreshSources() {
        final List<String> sources = new ArrayList<>();
        for (Source src : getSources(true)) {
            final String source = src.getSource();
            if (source != null && StringUtils.isNotEmpty(source)) {
                sources.add(source);
            }
        }

        if (!sources.isEmpty()) {
            stateChanged(SOURCES, SonyUtil.newStringType(StringUtils.join(sources, ',')));
        }
    }

    @Override
    public void refreshState() {
        final ScalarWebChannelTracker tracker = getChannelTracker();

        // always refresh these since they are used in lookups
        refreshSchemes();
        refreshSources();

        if (tracker.isCategoryLinked(ctgy -> StringUtils.startsWith(ctgy, PARENTRATING))) {
            refreshParentalRating();
        }

        if (tracker.isCategoryLinked(ctgy -> StringUtils.startsWith(ctgy, PLAYING))) {
            refreshPlayingContentInfo();
        }

        refreshCurrentExternalInputStatus(
                tracker.getLinkedChannelsForCategory(ctgy -> StringUtils.startsWith(ctgy, INPUT)));

        refreshCurrentExternalTerminalsStatus();

        if (tracker.isCategoryLinked(ctgy -> StringUtils.startsWith(ctgy, CONTENT))) {
            refreshContent();
        }

        if (tracker.isCategoryLinked(BLUETOOTHSETTINGS)) {
            refreshGeneralSettings(tracker.getLinkedChannelsForCategory(BLUETOOTHSETTINGS),
                    ScalarWebMethod.GETBLUETOOTHSETTINGS, BLUETOOTHSETTINGS);
        }
        if (tracker.isCategoryLinked(PLAYBACKSETTINGS)) {
            refreshGeneralSettings(tracker.getLinkedChannelsForCategory(PLAYBACKSETTINGS),
                    ScalarWebMethod.GETPLAYBACKMODESETTINGS, PLAYBACKSETTINGS);
        }

    }

    @Override
    public void setChannel(ScalarWebChannel channel, Command command) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(command, "command cannot be null");

        if (StringUtils.equalsIgnoreCase(channel.getCategory(), BLUETOOTHSETTINGS)) {
            final String uri = channel.getPathPart(0);
            if (uri == null || StringUtils.isEmpty(uri)) {
                logger.debug("{} command - channel has no target: {}", BLUETOOTHSETTINGS, channel);
                return;
            }
            setGeneralSetting(ScalarWebMethod.SETBLUETOOTHSETTINGS, uri, channel, command);
        } else if (StringUtils.equalsIgnoreCase(channel.getCategory(), PLAYBACKSETTINGS)) {
            final String uri = channel.getPathPart(0);
            if (uri == null || StringUtils.isEmpty(uri)) {
                logger.debug("{} command - channel has no uri: {}", PLAYBACKSETTINGS, channel);
                return;
            }
            setGeneralSetting(ScalarWebMethod.SETPLAYBACKMODESETTINGS, uri, channel, command);
        } else if (StringUtils.equalsIgnoreCase(channel.getCategory(), PL_CMD)) {
            final String uri = channel.getPathPart(0);
            if (uri == null || StringUtils.isEmpty(uri)) {
                logger.debug("{} command - channel has no uri: {}", PLAYBACKSETTINGS, channel);
                return;
            }
            if (command instanceof StringType) {
                setPlayingCommand(uri, channel.getId(), command.toString());
            } else {
                logger.debug("{} command not an StringType: {}", PL_CMD, command);
            }
        } else if (StringUtils.equalsIgnoreCase(channel.getCategory(), PL_PRESET)) {
            final String output = channel.getId();
            if (command instanceof DecimalType) {
                final int preset = ((DecimalType) command).intValue();
                statePlaying.compute(output, (k, v) -> {
                    if (v == null) {
                        return new PlayingState("", preset);
                    } else {
                        return new PlayingState(v.uri, preset);
                    }
                });
            } else {
                logger.debug("{} command not an StringType: {}", PL_PRESET, command);
            }
        } else if (StringUtils.startsWith(channel.getCategory(), TERM)) {
            final String uri = channel.getPathPart(0);
            if (uri == null || StringUtils.isEmpty(uri)) {
                logger.debug("{} command - channel has no uri: {}", TERM, channel);
                return;
            }
            switch (channel.getCategory()) {
                case TERM_SOURCE: {
                    if (command instanceof StringType) {
                        setTerminalSource(uri, command.toString());
                    } else {
                        logger.debug("{} command not an StringType: {}", TERM_SOURCE, command);
                    }
                    break;
                }
                case TERM_ACTIVE: {
                    if (command instanceof OnOffType) {
                        setTerminalStatus(uri, command == OnOffType.ON);
                    } else {
                        logger.debug("{} command not an OnOffType: {}", TERM_ACTIVE, command);
                    }
                    break;
                }
            }
        } else if (StringUtils.startsWith(channel.getCategory(), CONTENT)) {
            switch (channel.getCategory()) {
                case CN_PARENTURI: {
                    if (command instanceof StringType) {
                        stateContent.set(new ContentState(command.toString(), "", 0));
                        getContext().getScheduler().execute(() -> refreshContent());
                    } else {
                        logger.debug("{} command not an OnOffType: {}", CN_ISPROTECTED, command);
                    }
                    break;
                }
                case CN_INDEX: {
                    if (command instanceof DecimalType) {
                        stateContent.updateAndGet(
                                cs -> new ContentState(cs.parentUri, cs.uri, ((DecimalType) command).intValue()));
                        getContext().getScheduler().execute(() -> refreshContent());
                    } else {
                        logger.debug("{} command not an OnOffType: {}", CN_ISPROTECTED, command);
                    }
                    break;
                }
                case CN_SELECTED: {
                    final ContentState state = stateContent.get();
                    setPlayContent(state.uri, null, true);
                    break;
                }
                case CN_ISPROTECTED: {
                    if (command instanceof OnOffType) {
                        setContentProtection(command == OnOffType.ON);
                    } else {
                        logger.debug("{} command not an OnOffType: {}", CN_ISPROTECTED, command);
                    }
                    break;
                }
                case CN_EPGVISIBILITY: {
                    if (command instanceof StringType) {
                        setTvContentVisibility(command.toString(), null, null);
                    } else {
                        logger.debug("{} command not an StringType: {}", CN_EPGVISIBILITY, command);
                    }
                    break;
                }
                case CN_CHANNELSURFINGVISIBILITY: {
                    if (command instanceof StringType) {
                        setTvContentVisibility(null, command.toString(), null);
                    } else {
                        logger.debug("{} command not an StringType: {}", CN_CHANNELSURFINGVISIBILITY, command);
                    }
                    break;
                }
                case CN_VISIBILITY: {
                    if (command instanceof StringType) {
                        setTvContentVisibility(null, null, command.toString());
                    } else {
                        logger.debug("{} command not an StringType: {}", CN_VISIBILITY, command);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Sets the content protection on or off
     *
     * @param on true to turn on protection, false otherwise
     */
    private void setContentProtection(boolean on) {
        final ContentState cs = stateContent.get();
        handleExecute(ScalarWebMethod.SETDELETEPROTECTION, new DeleteProtection(cs.uri, on));
    }

    /**
     * Sets the content status to playing or not
     *
     * @param uri the non-null, possibly empty URI
     * @param on  true if playing, false otherwise
     */
    private void setPlayContent(String uri, @Nullable String output, boolean on) {
        Objects.requireNonNull(uri, "uri cannot be null");

        final String translatedOutput = output == null || StringUtils.equalsIgnoreCase(output, MAINOUTPUT) ? ""
                : output;
        if (on) {
            handleExecute(ScalarWebMethod.SETPLAYCONTENT, version -> {
                if (VersionUtilities.equals(version, ScalarWebMethod.V1_0, ScalarWebMethod.V1_1)) {
                    return new PlayContent_1_0(uri);
                }
                return new PlayContent_1_2(uri, translatedOutput);
            });
        } else {
            if (getService().hasMethod(ScalarWebMethod.STOPPLAYINGCONTENT)) {
                handleExecute(ScalarWebMethod.STOPPLAYINGCONTENT, new Output(uri));
            } else {
                handleExecute(ScalarWebMethod.DELETECOUNT, new DeleteContent(uri));
            }
        }
    }

    private void setPlayingCommand(String output, String id, String command) {
        final PlayingState state = statePlaying.get(id);
        final String playingUri = state == null ? "" : state.uri;

        final String translatedOutput = StringUtils.equalsIgnoreCase(output, MAINOUTPUT) ? "" : output;
        final boolean isRadio = Scheme.matches(Scheme.RADIO, playingUri);

        switch (command.toLowerCase()) {
            case "play":
                setPlayContent(playingUri, translatedOutput, true);
                break;

            case "pause":
                handleExecute(ScalarWebMethod.PAUSEPLAYINGCONTENT, new Output(translatedOutput));
                break;

            case "stop":
                setPlayContent(playingUri, translatedOutput, false);
                break;

            case "next":
                handleExecute(ScalarWebMethod.SETPLAYNEXTCONTENT, new Output(translatedOutput));
                break;

            case "prev":
                if (isRadio) {
                    handleExecute(ScalarWebMethod.SCANPLAYINGCONTENT,
                            new ScanPlayingContent_1_0(false, translatedOutput));
                } else {
                    handleExecute(ScalarWebMethod.SETPLAYPREVIOUSCONTENT, new Output(translatedOutput));
                }
                break;

            case "fwd":
                if (isRadio) {
                    handleExecute(ScalarWebMethod.SEEKBROADCASTSTATION, new SeekBroadcastStation_1_0(true, false));
                } else {
                    handleExecute(ScalarWebMethod.SCANPLAYINGCONTENT,
                            new ScanPlayingContent_1_0(true, translatedOutput));
                }
                break;

            case "bwd":
                handleExecute(ScalarWebMethod.SEEKBROADCASTSTATION, new SeekBroadcastStation_1_0(false, false));
                break;

            case "fwdseek":
                handleExecute(ScalarWebMethod.SEEKBROADCASTSTATION, new SeekBroadcastStation_1_0(true, true));
                break;

            case "bwdseek":
                handleExecute(ScalarWebMethod.SEEKBROADCASTSTATION, new SeekBroadcastStation_1_0(false, true));
                break;

            case "setpreset":
                final Matcher ms = Source.RADIOPATTERN.matcher(playingUri);
                if (ms.matches()) {
                    final int preset = state == null ? 1 : state.preset;
                    final String presetUri = ms.replaceFirst("$1" + preset);
                    handleExecute(ScalarWebMethod.PRESETBROADCASTSTATION, new PresetBroadcastStation(presetUri));
                } else {
                    logger.debug("Not playing a radio currently");
                }
                break;

            case "getpreset":
                final Matcher mg = Source.RADIOPATTERN.matcher(playingUri);
                if (mg.matches()) {
                    final int preset = state == null ? 1 : state.preset;
                    final String presetUri = mg.replaceFirst("$1" + preset);
                    setPlayContent(presetUri, translatedOutput, true);
                } else {
                    logger.debug("Not playing a radio currently");
                }
                break;

            default:
                break;
        }
    }

    private void setTerminalSource(String output, String source) {
        final Optional<Source> src = getSources().stream().filter(s -> s.isMatch(source)).findFirst();
        final String srcUri = src.isPresent() ? src.get().getSource() : null;
        setPlayContent(srcUri == null || StringUtils.isEmpty(srcUri) ? source : srcUri, output, true);

        getContext().getScheduler().execute(() -> {
            stateContent.set(new ContentState(source, "", 0));
            refreshContent();
        });
    }

    /**
     * Sets the terminal status to active or not
     *
     * @param uri the non-null, non-empty terminal status
     * @param on  true if playing, false otherwise
     */
    private void setTerminalStatus(String uri, boolean on) {
        Validate.notEmpty(uri, "uri cannot be empty");

        handleExecute(ScalarWebMethod.SETACTIVETERMINAL,
                new ActiveTerminal(uri, on ? ActiveTerminal.ACTIVE : ActiveTerminal.INACTIVE));

        // Turn off any other channel status
        for (ScalarWebChannel chnl : getChannelTracker().getLinkedChannelsForCategory(TERM_ACTIVE)) {
            if (!StringUtils.equalsIgnoreCase(chnl.getPathPart(0), uri)) {
                stateChanged(TERM_ACTIVE, chnl.getId(), OnOffType.OFF);
            }
        }
    }

    /**
     * Sets the tv content visibility.
     *
     * @param epgVisibility            the epg visibility (null if not specified)
     * @param channelSurfingVisibility the channel surfing visibility (null if not specified)
     * @param visibility               the visibility (null if not specified)
     */
    private void setTvContentVisibility(@Nullable String epgVisibility, @Nullable String channelSurfingVisibility,
            @Nullable String visibility) {
        final ContentState cs = stateContent.get();
        handleExecute(ScalarWebMethod.SETTVCONTENTVISIBILITY,
                new TvContentVisibility(cs.uri, epgVisibility, channelSurfingVisibility, visibility));
    }

    private Map<String, String> getTerminalOutputs(List<CurrentExternalTerminalsStatus_1_0> terms) {
        final Map<String, String> outputs = new HashMap<>();
        for (CurrentExternalTerminalsStatus_1_0 term : terms) {
            final String uri = term.getUri();
            if (uri != null && term.isOutput()) {
                outputs.put(uri, term.getTitle(uri));
            }
        }

        return outputs;
    }

    private class ContentState {
        private final String parentUri;
        private final String uri;
        private final int idx;

        public ContentState() {
            this("", "", 0);
        }

        public ContentState(String parentUri, String uri, int idx) {
            this.parentUri = parentUri;
            this.uri = uri;
            this.idx = idx;
        }
    }

    private class PlayingState {
        private final String uri;
        private final int preset;

        public PlayingState(String uri, int preset) {
            this.uri = uri;
            this.preset = preset;
        }
    }

    private class InputSource {
        private final String uri;
        private final @Nullable String title;
        private final List<String> outputs;

        public InputSource(String uri, @Nullable String title, @Nullable List<String> outputs) {
            this.uri = uri;
            this.title = title;
            this.outputs = outputs == null ? new ArrayList<>() : outputs;
        }
    }
}
