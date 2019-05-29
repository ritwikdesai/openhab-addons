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
package org.openhab.binding.sony.internal.providers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.common.AbstractUID;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelDefinitionBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelGroupDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.openhab.binding.sony.internal.SonyBindingConstants;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannel;
import org.openhab.binding.sony.internal.scalarweb.gson.GsonUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * An implementation of a {@link SonySource} that will source thing types from json files within the user data folder
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SonyFolderSource implements SonySource {
    /** The logger */
    private Logger logger = LoggerFactory.getLogger(SonyFolderSource.class);

    /** Folder paths */
    private static final String FOLDERBASE = ConfigConstants.getUserDataFolder() + File.separator + "sony";
    private static final String FOLDERDB = FOLDERBASE + File.separator + "db";
    private static final String FOLDERTHINGTYPES = FOLDERDB + File.separator + "types";

    private static final String FOLDERDEF = FOLDERBASE + File.separator + "definitions";
    private static final String FOLDERDEFTHINGTYPES = FOLDERDEF + File.separator + "types";
    private static final String FOLDERDEFCAPABILITY = FOLDERDEF + File.separator + "capability";

    /** The json file extension we are looking for */
    private static final String JSONEXT = "json";

    /** Scheduler used to schedule events */
    protected final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool("SonyFolderThingTypeProvider");

    /** Our reference of thing type uids to thing types */
    private final Map<ThingTypeUID, ThingType> thingTypes = new ConcurrentHashMap<>();

    /** Our reference of thing type uids to thing types */
    private final Map<ChannelGroupTypeUID, ChannelGroupType> groupTypes = new ConcurrentHashMap<>();

    /** Our reference of thing type uids to thing type definitions */
    private final Map<ThingTypeUID, SonyThingDefinition> thingTypeDefinitions = new ConcurrentHashMap<>();

    /** The folder watcher (null if none being watched) */
    private final AtomicReference<@Nullable Future<?>> watcher = new AtomicReference<>(null);

    /** The folder watcher (null if none being watched) */
    private final AtomicReference<@Nullable Future<?>> watchDog = new AtomicReference<>(null);

    /** The GSON that will be used for deserialization */
    private final Gson gson = GsonUtilities.getDefaultGson();

    /**
     * Constructs the source and starts the various threads
     */
    public SonyFolderSource() {
        // create the folders we will use
        SonyUtil.createFolder(FOLDERTHINGTYPES);
        SonyUtil.createFolder(FOLDERDEFTHINGTYPES);
        SonyUtil.createFolder(FOLDERDEFCAPABILITY);

        watcher.getAndSet(scheduler.submit(new Watcher()));

        // Keeps the watcher alive in case it encounters an error
        watchDog.getAndSet(scheduler.schedule(() -> {
            final Future<?> fut = watcher.get();
            if (fut == null || fut.isDone()) {
                SonyUtil.cancel(watcher.getAndSet(scheduler.submit(new Watcher())));
            }
        }, 30, TimeUnit.SECONDS));
    }

    @Override
    public Collection<ThingType> getThingTypes() {
        return thingTypes.values();
    }

    @Override
    public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID) {
        Objects.requireNonNull(thingTypeUID, "thingTypeUID cannot be null");
        return thingTypes.get(thingTypeUID);
    }

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID) {
        Objects.requireNonNull(channelGroupTypeUID, "channelGroupTypeUID cannot be null");
        return groupTypes.get(channelGroupTypeUID);
    }

    @Override
    public @Nullable Collection<ChannelGroupType> getChannelGroupTypes() {
        return groupTypes.values();
    }

    @Override
    public @Nullable SonyThingDefinition getSonyThingTypeDefinition(ThingTypeUID thingTypeUID) {
        Objects.requireNonNull(thingTypeUID, "thingTypeUID cannot be null");
        return thingTypeDefinitions.get(thingTypeUID);
    }

    /**
     *
     * This private helper class will watch the file system for changes and recreate thing types if a file is
     * added/changed/deleted
     *
     * @author Tim Roberts - Initial contribution
     */
    private class Watcher implements Runnable {
        @Override
        public void run() {
            try {
                if (SonyUtil.isInterrupted()) {
                    return;
                }

                readFiles();

                logger.debug("Starting watch for new/modified entries {}", FOLDERTHINGTYPES);
                try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                    final Path path = Paths.get(FOLDERTHINGTYPES);

                    if (SonyUtil.isInterrupted()) {
                        return;
                    }

                    path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                        if (SonyUtil.isInterrupted()) {
                            return;
                        }

                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (SonyUtil.isInterrupted()) {
                                return;
                            }

                            if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                                logger.debug("File deletion occurred, reloading ALL definitions");
                                readFiles();
                            } else {
                                readFile(((Path) event.context()).toAbsolutePath().toString());
                            }
                        }
                        key.reset();
                    }
                }
            } catch (IOException e) {
                logger.info("Watcher encountered an IOException: {}", e.getMessage(), e);
            } catch (InterruptedException e) {
                logger.debug("Watcher was interrupted");
            }
        }
    }

    /**
     * Will read all files in the DB and store the related thing types
     *
     * @throws IOException if an IO exception occurs reading the files
     */
    private void readFiles() throws IOException {
        logger.debug("Reading all files in {}", FOLDERTHINGTYPES);

        // clear out prior entries
        thingTypes.clear();
        thingTypeDefinitions.clear();

        final File folder = new File(FOLDERTHINGTYPES);
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                readFile(file.getAbsolutePath());
            }
        }
    }

    /**
     * Reads the specified file path, validates the syntax and stores the new thing type
     *
     * @param filePath a possibly null, possibly empty file path to read
     * @throws IOException if an IO Exception occurs reading the file
     */
    private void readFile(@Nullable String filePath) throws IOException {
        if (filePath != null && StringUtils.isEmpty(filePath)) {
            logger.debug("Unknown file: {}", filePath);
            return;
        }

        final String fileName = FilenameUtils.getName(filePath);

        final String ext = FilenameUtils.getExtension(filePath);
        if (!StringUtils.equalsIgnoreCase(JSONEXT, ext)) {
            logger.debug("Ignoring {} since it's not a .{} file", fileName, JSONEXT);
            return;
        }

        final String contents = FileUtils.readFileToString(new File(filePath));

        final SonyThingDefinition[] ttds = gson.fromJson(contents, SonyThingDefinition[].class);

        logger.debug("Processing {}", fileName);
        int idx = 0;
        for (SonyThingDefinition ttd : ttds) {
            idx++;

            final List<String> validationMessage = new ArrayList<>();
            final Map<String, String> channelGroups = ttd.getChannelGroups();

            final String service = ttd.getService();
            if (service == null || StringUtils.isEmpty(service)) {
                validationMessage.add("Invalid/missing service element");
            } else if (!service.matches(AbstractUID.SEGMENT_PATTERN)) {
                validationMessage.add("Invalid service element (must be a valid UID): " + service);
            }

            final String modelName = ttd.getModelName();
            if (modelName == null || StringUtils.isEmpty(modelName)) {
                validationMessage.add("Invalid/missing modelName element");
            } else if (!modelName.matches(AbstractUID.SEGMENT_PATTERN)) {
                validationMessage.add("Invalid modelName element (must be a valid UID): " + modelName);
            }

            final String label = ttd.getLabel();
            final String desc = ttd.getDescription();

            final List<SonyThingChannelDefinition> chls = ttd.getChannels();

            final Map<String, List<ChannelDefinition>> cds = new HashMap<>();
            for (SonyThingChannelDefinition chl : chls) {

                final List<String> channelValidationMessage = new ArrayList<>();

                final String mappedId = chl.getMappedChannelId();
                final String channelId = chl.getChannelId();
                if (channelId == null || StringUtils.isEmpty(channelId)) {
                    channelValidationMessage.add("Missing channelID element");
                    continue;
                    // } else if (!channelId.matches(AbstractUID.SEGMENT_PATTERN)) {
                    // channelValidationMessage.add("Invalid channelID element (must be a valid UID): " + channelId);
                    // continue;
                }
                final String chlIdToUse = mappedId == null ? channelId : mappedId;

                final String groupId = StringUtils.substringBefore(chlIdToUse, "#");
                if (groupId == null || StringUtils.isEmpty(groupId)) {
                    channelValidationMessage.add("Missing groupID from channelId: " + chlIdToUse);
                    continue;
                }

                final String idWithoutGroup = StringUtils.substringAfter(chlIdToUse, "#");

                final String channelType = chl.getChannelType();
                if (channelType == null || StringUtils.isEmpty(channelType)) {
                    channelValidationMessage.add("Missing channelType element");
                    continue;
                } else if (!channelType.matches(AbstractUID.SEGMENT_PATTERN)) {
                    channelValidationMessage.add("Invalid channelType element (must be a valid UID): " + channelType);
                    continue;
                }

                final Map<String, String> props = new HashMap<>();
                for (Entry<@Nullable String, @Nullable String> entry : chl.getProperties().entrySet()) {
                    final @Nullable String propKey = entry.getKey();
                    final @Nullable String propValue = entry.getValue();
                    if (propKey == null || StringUtils.isEmpty(propKey)) {
                        channelValidationMessage.add("Missing property key value");
                    } else {
                        props.put(propKey, propValue == null ? "" : propValue);
                    }
                }

                props.put(ScalarWebChannel.CNL_BASECHANNELID, channelId);

                if (channelValidationMessage.size() == 0) {
                    List<ChannelDefinition> chlDefs = cds.get(groupId);
                    if (chlDefs == null) {
                        chlDefs = new ArrayList<>();
                        cds.put(groupId, chlDefs);
                    }

                    chlDefs.add(new ChannelDefinitionBuilder(idWithoutGroup,
                            new ChannelTypeUID(SonyBindingConstants.BINDING_ID, channelType)).withProperties(props)
                                    .build());
                } else {
                    validationMessage.addAll(channelValidationMessage);
                }
            }

            if (chls.size() == 0) {
                validationMessage.add("Has no valid channels");
                continue;
            }

            final String configUriStr = ttd.getConfigUri();
            if (configUriStr == null || StringUtils.isEmpty(configUriStr)) {
                validationMessage.add("Invalid thing definition - missing configUri string");
            }

            final String thingTypeId = service + "-" + modelName;
            if (validationMessage.size() == 0) {
                try {
                    final Map<String, ChannelGroupType> cgd = cds.entrySet().stream()
                            .collect(Collectors.toMap(k -> k.getKey(), e -> {
                                final String groupId = e.getKey();
                                final List<ChannelDefinition> channels = e.getValue();
                                final String groupLabel = channelGroups.getOrDefault(groupId, groupId);
                                final String groupTypeId = thingTypeId + "-" + groupId;

                                return ChannelGroupTypeBuilder
                                        .instance(new ChannelGroupTypeUID(SonyBindingConstants.BINDING_ID, groupTypeId),
                                                groupLabel)
                                        .withChannelDefinitions(channels).build();
                            }));

                    groupTypes.putAll(cgd.values().stream().collect(Collectors.toMap(k -> k.getUID(), v -> v)));

                    final List<ChannelGroupDefinition> gDefs = cgd.entrySet().stream()
                            .map(gt -> new ChannelGroupDefinition(gt.getKey(), gt.getValue().getUID()))
                            .collect(Collectors.toList());

                    final URI configUri = new URI(configUriStr);
                    final ThingType thingType = ThingTypeBuilder
                            .instance(SonyBindingConstants.BINDING_ID, thingTypeId, label)
                            .withConfigDescriptionURI(configUri).withDescription(desc)
                            .withChannelGroupDefinitions(gDefs).build();

                    thingTypes.put(thingType.getUID(), thingType);
                    thingTypeDefinitions.put(thingType.getUID(), ttd);

                    logger.debug("Successfully created a thing type {} from {}", thingType.getUID(), fileName);

                } catch (URISyntaxException e) {
                    validationMessage.add("Configuration URI (" + configUriStr + ") was not a valid URI");
                }
            }

            if (validationMessage.size() > 0) {
                logger.debug("Error creating a thing type from element #{} ({}) in file {}:", idx, modelName, fileName);
                for (String msg : validationMessage) {
                    logger.debug("   " + msg);
                }
            }
        }
    }

    @Override
    public void writeThingDefinition(SonyThingDefinition thingTypeDefinition) {
        Objects.requireNonNull(thingTypeDefinition, "thingTypeDefinition cannot be null");

        final File file = new File(
                FOLDERDEFTHINGTYPES + File.separator + thingTypeDefinition.getModelName() + "." + JSONEXT);
        if (file.exists()) {
            logger.debug("File for thing definition already exists (write ignored): {}", file);
            return;
        }

        final String json = gson.toJson(new SonyThingDefinition[] { thingTypeDefinition });

        try {
            FileUtils.write(file, json);
        } catch (IOException e) {
            logger.debug("IOException writing thing defintion to {}: {}", file, e.getMessage(), e);
        }
    }

    @Override
    public void writeDeviceCapabilities(SonyDeviceCapability deviceCapability) {
        Objects.requireNonNull(deviceCapability, "deviceCapability cannot be null");

        final File file = new File(
                FOLDERDEFCAPABILITY + File.separator + deviceCapability.getModelName() + "." + JSONEXT);
        if (file.exists()) {
            logger.debug("File for thing definition already exists (write ignored): {}", file);
            return;
        }

        final String json = gson.toJson(deviceCapability);

        try {
            FileUtils.write(file, json);
        } catch (IOException e) {
            logger.debug("IOException writing methods to {}: {}", file, e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        SonyUtil.cancel(watchDog.get());
        SonyUtil.cancel(watcher.get());
    }

}
