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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.WordUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannel;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelDescriptor;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelTracker;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebContext;
import org.openhab.binding.sony.internal.scalarweb.VersionUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.openhab.binding.sony.internal.scalarweb.models.api.ActiveApp;
import org.openhab.binding.sony.internal.scalarweb.models.api.ApplicationList;
import org.openhab.binding.sony.internal.scalarweb.models.api.ApplicationStatusList;
import org.openhab.binding.sony.internal.scalarweb.models.api.PublicKey;
import org.openhab.binding.sony.internal.scalarweb.models.api.TextFormRequest_1_1;
import org.openhab.binding.sony.internal.scalarweb.models.api.TextFormResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of the protocol handles the AppControl service
 *
 * @author Tim Roberts - Initial contribution
 * @param <T> the generic type for the callback
 */
@NonNullByDefault
class ScalarWebAppControlProtocol<T extends ThingCallback<String>> extends AbstractScalarWebProtocol<T> {

    /** The logger. */
    private Logger logger = LoggerFactory.getLogger(ScalarWebAppControlProtocol.class);

    // Varous channel constants
    private static final String APPTITLE = "apptitle";
    private static final String APPICON = "appicon";
    private static final String APPDATA = "appdata";
    private static final String APPSTATUS = "appstatus";
    private static final String TEXTFORM = "textform";
    private static final String STATUS = "status";
    private static final String START = "start";
    private static final String STOP = "stop";

    // The intervals used for refresh
    private static final int APPLISTINTERVAL = 60000;
    private static final int ACTIVEAPPINTERVAL = 10000;

    /** The encryption key to use */
    private final @Nullable String pubKey;

    /** The lock used to modify app information */
    private final Lock appListLock = new ReentrantLock();

    /** The last time the app list was accessed */
    private long appListLastTime = 0;

    /** The applications */
    private final List<ApplicationList> apps = new CopyOnWriteArrayList<ApplicationList>();

    /** The lock used to access activeApp */
    private final Lock activeAppLock = new ReentrantLock();

    /** The last time the activeApp was accessed */
    private long activeAppLastTime = 0;

    /** The active app. */
    private @Nullable ActiveApp activeApp = null;

    /**
     * Instantiates a new scalar web app control protocol.
     *
     * @param context  the non-null context to use
     * @param context  the non-null context to use
     * @param service  the non-null service to use
     * @param callback the non-null callback to use
     */
    ScalarWebAppControlProtocol(ScalarWebProtocolFactory<T> factory, ScalarWebContext context, ScalarWebService service,
            T callback) {
        super(factory, context, service, callback);

        String localPubKey = null;
        final ScalarWebService enc = getService(ScalarWebService.ENCRYPTION);
        if (enc != null) {
            try {
                final PublicKey publicKey = enc.execute(ScalarWebMethod.GETPUBLICKEY).as(PublicKey.class);
                localPubKey = publicKey.getPublicKey();
            } catch (IOException e) {
                logger.debug("Exception getting public key: {}", e.getMessage());
            }
        }

        pubKey = localPubKey;
    }

    @Override
    public Collection<ScalarWebChannelDescriptor> getChannelDescriptors() {
        final ChannelIdCache cache = new ChannelIdCache();
        final List<ScalarWebChannelDescriptor> descriptors = new ArrayList<ScalarWebChannelDescriptor>();

        for (final ApplicationList app : getApplications()) {
            final String uri = app.getUri();
            if (uri == null || StringUtils.isEmpty(uri)) {
                logger.debug("uri cannot be empty: {}", app);
                continue;
            }

            final String title = app.getTitle();
            final String label = WordUtils.capitalize(title);
            final String id = cache.getUniqueChannelId(title).toLowerCase();

            descriptors.add(createDescriptor(createChannel(APPTITLE, id, uri), "String", "scalarappcontrolapptitle",
                    "App " + label + " Title", "Title for application " + label));
            descriptors.add(createDescriptor(createChannel(APPICON, id, uri), "String", "scalarappcontrolappicon",
                    "App " + label + " Icon", "Icon for application " + label));
            descriptors.add(createDescriptor(createChannel(APPDATA, id, uri), "String", "scalarappcontrolappdata",
                    "App " + label + " Data", "Data for application " + label));
            descriptors.add(createDescriptor(createChannel(APPSTATUS, id, uri), "String", "scalarappcontrolappstatus",
                    "App " + label + " Status", "Status for " + label));
        }

        try {
            final List<ApplicationStatusList> statuses = execute(ScalarWebMethod.GETAPPLICATIONSTATUSLIST)
                    .asArray(ApplicationStatusList.class);
            for (final ApplicationStatusList status : statuses) {
                final String name = status.getName();
                if (name == null || StringUtils.isEmpty(name)) {
                    logger.debug("name cannot be empty: {}", status);
                    continue;
                }

                final String title = WordUtils.capitalize(name);
                final String id = cache.getUniqueChannelId(title).toLowerCase();
                descriptors.add(createDescriptor(createChannel(STATUS, id, name), "Switch", "scalarappcontrolstatus",
                        "Indicator " + title, "Indicator for " + title));
            }
        } catch (IOException e) {
            logger.info("Exception getting application status list: {}", e.getMessage());
        }

        try {
            final String textFormVersion = getService().getVersion(ScalarWebMethod.GETTEXTFORM);
            if (VersionUtilities.equals(textFormVersion, ScalarWebMethod.V1_0)) {
                descriptors.add(createDescriptor(createChannel(TEXTFORM), "String", "scalarappcontroltextform"));
            } else if (VersionUtilities.equals(textFormVersion, ScalarWebMethod.V1_1)) {
                final String localPubKey = pubKey;
                if (localPubKey == null || StringUtils.isEmpty(localPubKey)) {
                    logger.info("Can't get text form - no public key");
                } else {
                    execute(ScalarWebMethod.GETTEXTFORM, new TextFormRequest_1_1(localPubKey, null));
                    descriptors.add(createDescriptor(createChannel(TEXTFORM), "String", "scalarappcontroltextform"));
                }
            }
        } catch (IOException e) {
            logger.info("Exception getting text form: {}", e.getMessage());
        }

        return descriptors;
    }

    @Override
    public void refreshState() {
        final ScalarWebChannelTracker tracker = getChannelTracker();

        final List<ScalarWebChannel> appChannels = getChannelTracker().getLinkedChannelsForCategory(APPTITLE, APPICON,
                APPDATA, APPSTATUS);
        if (appChannels.size() > 0) {
            final ActiveApp activeApp = getActiveApp();
            for (final ApplicationList app : getApplications()) {
                final String uri = app.getUri();

                for (ScalarWebChannel chnl : appChannels) {
                    if (StringUtils.equalsIgnoreCase(uri, chnl.getPathPart(0))) {
                        final String cid = chnl.getChannelId();
                        callback.stateChanged(cid, SonyUtil.newStringType(app.getTitle()));
                        switch (chnl.getCategory()) {
                            case APPTITLE:
                                callback.stateChanged(cid, SonyUtil.newStringType(app.getTitle()));
                                break;

                            case APPICON:
                                callback.stateChanged(cid, SonyUtil.newStringType(app.getIcon()));
                                break;

                            case APPDATA:
                                callback.stateChanged(cid, SonyUtil.newStringType(app.getData()));
                                break;

                            case APPSTATUS:
                                callback.stateChanged(cid,
                                        activeApp == null ? SonyUtil.newStringType(STOP)
                                                : StringUtils.equalsIgnoreCase(activeApp.getUri(), uri)
                                                        ? SonyUtil.newStringType(START)
                                                        : SonyUtil.newStringType(STOP));
                                break;

                            default:
                                logger.debug("Unknown refresh channel category: {} for channel {}", chnl.getCategory(),
                                        chnl);
                                break;
                        }
                    }
                }
            }
        }

        try {
            final List<ScalarWebChannel> statusChannels = getChannelTracker().getLinkedChannelsForCategory(STATUS);
            if (statusChannels.size() > 0) {
                if (tracker.isCategoryLinked(STATUS)) {
                    final List<ApplicationStatusList> statuses = execute(ScalarWebMethod.GETAPPLICATIONSTATUSLIST)
                            .asArray(ApplicationStatusList.class);
                    for (final ApplicationStatusList status : statuses) {
                        final String name = status.getName();
                        if (name != null && StringUtils.isNotEmpty(name)) {
                            for (ScalarWebChannel chnl : statusChannels) {
                                final String[] paths = chnl.getPaths();
                                if (paths.length > 0 && StringUtils.equalsIgnoreCase(name, paths[0])) {
                                    callback.stateChanged(chnl.getChannelId(),
                                            status.isOn() ? OnOffType.ON : OnOffType.OFF);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // handled by execute
        }

        final String textFormVersion = getService().getVersion(ScalarWebMethod.GETTEXTFORM);
        if (VersionUtilities.equals(textFormVersion, ScalarWebMethod.V1_0) || StringUtils.isNotEmpty(pubKey)) {
            final List<ScalarWebChannel> textChannels = getChannelTracker().getLinkedChannelsForCategory(TEXTFORM);
            if (textChannels.size() > 0) {
                for (ScalarWebChannel chnl : textChannels) {
                    refreshTextForm(chnl.getChannelId());
                }
            }
        }
    }

    @Override
    public void refreshChannel(ScalarWebChannel channel) {
        Objects.requireNonNull(channel, "channel cannot be null");

        final String ctgy = channel.getCategory();
        if (StringUtils.equalsIgnoreCase(ctgy, TEXTFORM)) {
            refreshTextForm(channel.getChannelId());
        } else {
            refreshState();
            final String[] paths = channel.getPaths();
            if (paths.length == 0) {
                logger.debug("Refresh Channel path invalid: {}", channel);
            } else {
                final String target = paths[0];

                switch (ctgy) {
                    case APPTITLE:
                        refreshAppTitle(channel.getChannelId(), target);
                        break;

                    case APPICON:
                        refreshAppIcon(channel.getChannelId(), target);
                        break;

                    case APPDATA:
                        refreshAppData(channel.getChannelId(), target);
                        break;

                    case APPSTATUS:
                        refreshAppStatus(channel.getChannelId(), target);
                        break;

                    case STATUS:
                        refreshStatus(channel.getChannelId(), target);
                        break;

                    default:
                        logger.debug("Unknown refresh channel: {}", channel);
                        break;
                }
            }
        }
    }

    /**
     * Gets the active application. Note that this method will cache the active application applications for
     * {@link #ACTIVEAPPINTERVAL} milliseconds to prevent excessive retrieving of the application list (applications
     * don't change that often!)
     *
     * @return the active application or null if none
     */
    private @Nullable ActiveApp getActiveApp() {
        activeAppLock.lock();
        try {
            final long now = System.currentTimeMillis();
            if (activeApp == null || activeAppLastTime + ACTIVEAPPINTERVAL < now) {
                activeApp = execute(ScalarWebMethod.GETWEBAPPSTATUS).as(ActiveApp.class);
                activeAppLastTime = now;
            }

        } catch (IOException e) {
            // already handled by execute
        } finally {
            activeAppLock.unlock();
        }

        return activeApp;
    }

    /**
     * Gets the list of applications. Note that this method will cache the applications for {@link #APPLISTINTERVAL}
     * milliseconds to prevent excessive retrieving of the application list (applications don't change that often!)
     *
     * @return the non-null, possibly empty unmodifiable list of applications
     */
    private List<ApplicationList> getApplications() {
        appListLock.lock();
        try {
            final long now = System.currentTimeMillis();
            if (appListLastTime + APPLISTINTERVAL < now) {
                apps.clear();
                apps.addAll(execute(ScalarWebMethod.GETAPPLICATIONLIST).asArray(ApplicationList.class));
                appListLastTime = now;
            }

        } catch (IOException e) {
            // already handled by execute
        } finally {
            appListLock.unlock();
        }

        return Collections.unmodifiableList(apps);
    }

    /**
     * Gets the application list for the given URI
     *
     * @param appUri the non-null, non-empty application URI
     * @return the application list or null if none
     */
    private @Nullable ApplicationList getApplicationList(String appUri) {
        Validate.notEmpty(appUri, "appUri cannot be empty");
        for (final ApplicationList app : getApplications()) {
            if (StringUtils.equalsIgnoreCase(app.getUri(), appUri)) {
                return app;
            }
        }
        return null;
    }

    /**
     * Refresh app title for the given URI
     *
     * @param channelId the non-null, non-empty channel ID
     * @param appUri    the non-null, non-empty application URI
     */
    private void refreshAppTitle(String channelId, String appUri) {
        Validate.notEmpty(channelId, "channelId cannot be empty");
        Validate.notEmpty(appUri, "appUri cannot be empty");
        final ApplicationList app = getApplicationList(appUri);
        if (app != null) {
            callback.stateChanged(channelId, SonyUtil.newStringType(app.getTitle()));
        }
    }

    /**
     * Refresh app icon for the given URI
     *
     * @param channelId the non-null, non-empty channel ID
     * @param appUri    the non-null, non-empty application URI
     */
    private void refreshAppIcon(String channelId, String appUri) {
        Validate.notEmpty(channelId, "channelId cannot be empty");
        Validate.notEmpty(appUri, "appUri cannot be empty");
        final ApplicationList app = getApplicationList(appUri);
        if (app != null) {
            callback.stateChanged(channelId, SonyUtil.newStringType(app.getIcon()));
        }
    }

    /**
     * Refresh app data for the given URI
     *
     * @param channelId the non-null, non-empty channel ID
     * @param appUri    the non-null, non-empty application URI
     */
    private void refreshAppData(String channelId, String appUri) {
        Validate.notEmpty(channelId, "channelId cannot be empty");
        Validate.notEmpty(appUri, "appUri cannot be empty");
        final ApplicationList app = getApplicationList(appUri);
        if (app != null) {
            callback.stateChanged(channelId, SonyUtil.newStringType(app.getData()));
        }
    }

    /**
     * Refresh app status for the given URI
     *
     * @param channelId the non-null, non-empty channel ID
     * @param appUri    the non-null, non-empty application URI
     */
    private void refreshAppStatus(String channelId, String appUri) {
        Validate.notEmpty(channelId, "channelId cannot be empty");
        Validate.notEmpty(appUri, "appUri cannot be empty");
        final ActiveApp app = getActiveApp();
        if (app != null) {
            callback.stateChanged(channelId,
                    StringUtils.equalsIgnoreCase(appUri, app.getUri()) ? SonyUtil.newStringType(START)
                            : SonyUtil.newStringType(STOP));
        }
    }

    /**
     * Refresh status for the status name
     *
     * @param channelId  the non-null, non-empty channel ID
     * @param statusName the non-null, non-empty status name
     */
    private void refreshStatus(String channelId, String statusName) {
        Validate.notEmpty(channelId, "channelId cannot be empty");
        Validate.notEmpty(statusName, "statusName cannot be empty");
        try {
            final List<ApplicationStatusList> statuses = execute(ScalarWebMethod.GETAPPLICATIONSTATUSLIST)
                    .asArray(ApplicationStatusList.class);
            for (final ApplicationStatusList status : statuses) {
                if (StringUtils.equalsIgnoreCase(statusName, status.getName())) {
                    callback.stateChanged(channelId, status.isOn() ? OnOffType.ON : OnOffType.OFF);
                }
            }
            callback.stateChanged(channelId, OnOffType.OFF);
        } catch (IOException e) {
            logger.debug("Exception getting application status list: {}", e.getMessage());
        }

    }

    /**
     * Refresh text from a text form
     *
     * @param channelId the non-null, non-empty channel ID
     */
    private void refreshTextForm(String channelId) {
        Validate.notEmpty(channelId, "channelId cannot be empty");
        try {
            final String localEncKey = pubKey;
            if (localEncKey != null && StringUtils.isNotEmpty(localEncKey)) {
                final TextFormResult form = execute(ScalarWebMethod.GETTEXTFORM, version -> {
                    if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
                        return null;
                    }
                    return new TextFormRequest_1_1(localEncKey, null);
                }).as(TextFormResult.class);
                callback.stateChanged(channelId, SonyUtil.newStringType(form.getText()));
            }
        } catch (IOException e) {
            logger.debug("Exception getting text form: {}", e.getMessage());
        }
    }

    @Override
    public void setChannel(ScalarWebChannel channel, Command command) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(command, "command cannot be null");

        switch (channel.getCategory()) {
            case TEXTFORM:
                if (command instanceof StringType) {
                    setTextForm(command.toString());
                } else {
                    logger.debug("TEXTFORM command not an StringType: {}", command);
                }

                break;

            case APPSTATUS:
                final String[] paths = channel.getPaths();
                if (paths.length == 0) {
                    logger.debug("Set APPSTATUS Channel path invalid: {}", channel);
                } else {
                    if (command instanceof StringType) {
                        setAppStatus(paths[0], StringUtils.equalsIgnoreCase(START, command.toString()));
                    } else {
                        logger.debug("APPSTATUS command not an StringType: {}", command);
                    }
                    if (command instanceof OnOffType) {
                        setAppStatus(paths[0], command == OnOffType.ON);
                    } else {
                        logger.debug("APPSTATUS command not an OnOffType: {}", command);
                    }
                }

                break;

            default:
                logger.debug("Unhandled channel command: {} - {}", channel, command);
                break;
        }
    }

    /**
     * Sets the text in a form
     *
     * @param text the possibly null, possibly empty text
     */
    private void setTextForm(@Nullable String text) {
        final String version = getService().getVersion(ScalarWebMethod.SETTEXTFORM);
        if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
            handleExecute(ScalarWebMethod.SETTEXTFORM, text == null ? "" : text);
        } else if (VersionUtilities.equals(version, ScalarWebMethod.V1_0)) {
            final String localEncKey = pubKey;
            if (localEncKey != null && StringUtils.isNotEmpty(localEncKey)) {
                handleExecute(ScalarWebMethod.SETTEXTFORM,
                        new TextFormRequest_1_1(localEncKey, text == null ? "" : text));
            }
        } else {
            logger.debug("Unknown {} method version: {}", ScalarWebMethod.SETTEXTFORM, version);
        }
    }

    /**
     * Sets the application status
     *
     * @param appUri the non-null, non-empty application URI
     * @param on     true if active, false otherwise
     */
    private void setAppStatus(String appUri, boolean on) {
        Validate.notEmpty(appUri, "appUri cannot be empty");
        if (on) {
            handleExecute(ScalarWebMethod.SETACTIVEAPP, new ActiveApp(appUri, null));
        } else {
            handleExecute(ScalarWebMethod.TERMINATEAPPS);
        }
    }
}
