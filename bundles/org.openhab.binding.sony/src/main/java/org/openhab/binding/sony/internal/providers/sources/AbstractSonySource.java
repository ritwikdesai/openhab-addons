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
package org.openhab.binding.sony.internal.providers.sources;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.common.AbstractUID;
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
import org.openhab.binding.sony.internal.providers.SonyProviderListener;
import org.openhab.binding.sony.internal.providers.models.SonyThingChannelDefinition;
import org.openhab.binding.sony.internal.providers.models.SonyThingDefinition;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannel;
import org.openhab.binding.sony.internal.scalarweb.gson.GsonUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a {@link SonySource} that will source thing types from
 * json files within the user data folder
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractSonySource implements SonySource {
    /** The logger */
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /** The json file extension we are looking for */
    protected static final String JSONEXT = "json";

    /** The GSON that will be used for deserialization */
    protected final Gson gson = GsonUtilities.getDefaultGson();

    /** THe lock protecting the state (thingTypeDefinitions, thingTypes and groupTypes - not listeners) */
    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();

    /** Our reference of thing type uids to thing type definitions */
    private final Map<ThingTypeUID, SonyThingDefinition> thingTypeDefinitions = new HashMap<>();

    /** Our reference of thing type uids to thing types */
    private final Map<ThingTypeUID, ThingType> thingTypes = new HashMap<>();

    /** Our reference of thing type uids to thing types */
    private final Map<ChannelGroupTypeUID, ChannelGroupType> groupTypes = new HashMap<>();

    /** The list of listeners */
    private final ReadWriteLock listenerLock = new ReentrantReadWriteLock();
    private final Map<String, List<SonyProviderListener>> listeners = new HashMap<>();

    @Override
    public Collection<ThingType> getThingTypes() {
        final Lock readLock = stateLock.readLock();
        readLock.lock();
        try {
            return thingTypes.values();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID) {
        Objects.requireNonNull(thingTypeUID, "thingTypeUID cannot be null");

        final Lock readLock = stateLock.readLock();
        readLock.lock();
        try {
            return thingTypes.get(thingTypeUID);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID) {
        Objects.requireNonNull(channelGroupTypeUID, "channelGroupTypeUID cannot be null");
        final Lock readLock = stateLock.readLock();
        readLock.lock();
        try {
            return groupTypes.get(channelGroupTypeUID);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public @Nullable Collection<ChannelGroupType> getChannelGroupTypes() {
        final Lock readLock = stateLock.readLock();
        readLock.lock();
        try {
            return groupTypes.values();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public @Nullable SonyThingDefinition getSonyThingTypeDefinition(ThingTypeUID thingTypeUID) {
        Objects.requireNonNull(thingTypeUID, "thingTypeUID cannot be null");
        final Lock readLock = stateLock.readLock();
        readLock.lock();
        try {
            return thingTypeDefinitions.get(thingTypeUID);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Will read all files in the DB and store the related thing types
     *
     * @throws IOException if an IO exception occurs reading the files
     */
    protected void readFiles(String folder) throws IOException, JsonSyntaxException {
        logger.debug("Reading all files in {}", folder);

        final Lock writeLock = stateLock.writeLock();
        writeLock.lock();
        try {
            // clear out prior entries
            groupTypes.clear();
            thingTypes.clear();
            thingTypeDefinitions.clear();

            List<Map.Entry<ThingType, SonyThingDefinition>> results = new ArrayList<>();
            for (File file : new File(folder).listFiles()) {
                if (file.isFile()) {
                    results.addAll(readFile(file.getAbsolutePath()));
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Reads the specified file path, validates the syntax and stores the new thing
     * type
     *
     * @param filePath a possibly null, possibly empty file path to read
     * @throws IOException if an IO Exception occurs reading the file
     */
    protected List<Map.Entry<ThingType, SonyThingDefinition>> readFile(@Nullable String filePath)
            throws IOException, JsonSyntaxException {
        if (filePath != null && StringUtils.isEmpty(filePath)) {
            logger.debug("Unknown file: {}", filePath);
            return new ArrayList<>();
        }

        final String fileName = FilenameUtils.getName(filePath);

        final String ext = FilenameUtils.getExtension(filePath);
        if (!StringUtils.equalsIgnoreCase(JSONEXT, ext)) {
            logger.debug("Ignoring {} since it's not a .{} file", fileName, JSONEXT);
            return new ArrayList<>();
        }

        logger.debug("Reading file {} as a SonyThingDefinition[]", filePath);
        final String contents = FileUtils.readFileToString(new File(filePath));

        final JsonElement def = gson.fromJson(contents, JsonElement.class);
        if (def.isJsonArray()) {
            final SonyThingDefinition[] ttds = gson.fromJson(def, SonyThingDefinition[].class);
            return addThingDefinitions(fileName, ttds);
        } else {
            final SonyThingDefinition ttd = gson.fromJson(def, SonyThingDefinition.class);
            return addThingDefinitions(fileName, new SonyThingDefinition[] { ttd });
        }
    }

    protected List<Map.Entry<ThingType, SonyThingDefinition>> addThingDefinitions(String referenceName,
            SonyThingDefinition[] ttds) {
        logger.debug("Processing {}", referenceName);
        int idx = 0;

        final List<Map.Entry<ThingType, SonyThingDefinition>> results = new ArrayList<>();

        final Lock writeLock = stateLock.writeLock();
        writeLock.lock();
        try {
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
                        // channelValidationMessage.add("Invalid channelID element (must be a valid
                        // UID): " + channelId);
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
                        channelValidationMessage
                                .add("Invalid channelType element (must be a valid UID): " + channelType);
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

                                    return ChannelGroupTypeBuilder.instance(
                                            new ChannelGroupTypeUID(SonyBindingConstants.BINDING_ID, groupTypeId),
                                            groupLabel).withChannelDefinitions(channels).build();
                                }));

                        final List<ChannelGroupDefinition> gDefs = cgd.entrySet().stream()
                                .map(gt -> new ChannelGroupDefinition(gt.getKey(), gt.getValue().getUID()))
                                .collect(Collectors.toList());

                        final URI configUri = new URI(configUriStr);
                        final ThingType thingType = ThingTypeBuilder
                                .instance(SonyBindingConstants.BINDING_ID, thingTypeId, label)
                                .withConfigDescriptionURI(configUri).withDescription(desc)
                                .withChannelGroupDefinitions(gDefs).build();

                        final ThingTypeUID uid = thingType.getUID();

                        groupTypes.putAll(cgd.values().stream().collect(Collectors.toMap(k -> k.getUID(), v -> v)));
                        thingTypes.put(uid, thingType);
                        thingTypeDefinitions.put(uid, ttd);

                        results.add(new AbstractMap.SimpleEntry<>(thingType, ttd));

                        fireThingTypeFound(uid);

                        logger.debug("Successfully created a thing type {} from {}", thingType.getUID(), referenceName);

                    } catch (URISyntaxException e) {
                        validationMessage.add("Configuration URI (" + configUriStr + ") was not a valid URI");
                    }
                }

                if (validationMessage.size() > 0) {
                    logger.debug("Error creating a thing type from element #{} ({}) in {}:", idx, modelName,
                            referenceName);
                    for (String msg : validationMessage) {
                        logger.debug("   " + msg);
                    }
                }
            }
            return results;
        } finally {
            writeLock.unlock();
        }
    }

    public void addListener(String modelName, ThingTypeUID currentThingTypeUID, SonyProviderListener listener) {
        Validate.notEmpty(modelName, "modelName cannot be empty");
        Objects.requireNonNull(currentThingTypeUID, "currentThingTypeUID cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        final Lock writeLock = listenerLock.writeLock();
        try {
            writeLock.lock();

            List<SonyProviderListener> list = listeners.get(modelName);
            if (list == null) {
                list = new ArrayList<>();
                listeners.put(modelName, list);
            }
            if (!list.contains(listener)) {
                list.add(listener);
            }
        } finally {
            writeLock.unlock();
        }

        final String serviceName = SonyUtil.getServiceName(currentThingTypeUID);
        final ThingTypeUID uidForModel = findMaxForModel(serviceName, modelName);
        if (uidForModel != null && !Objects.equals(uidForModel, currentThingTypeUID)) {
            listener.thingTypeFound(uidForModel);
        }
    }

    protected @Nullable ThingTypeUID findMaxForModel(String serviceName, String modelName) {
        final Lock readLock = stateLock.readLock();
        try {
            readLock.lock();

            ThingTypeUID max = null;
            Integer maxVers = null;
            for (ThingTypeUID uid : thingTypes.keySet()) {
                if (SonyUtil.isModelMatch(uid, serviceName, modelName)) {
                    final Integer vers = SonyUtil.getModelVersion(uid);
                    if (maxVers == null || vers > maxVers) {
                        max = uid;
                        maxVers = vers;
                    }
                }
            }

            return max;
        } finally {
            readLock.unlock();
        }

    }

    public boolean removeListener(SonyProviderListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        final Lock writeLock = listenerLock.writeLock();
        try {
            writeLock.lock();
            return this.listeners.values().stream().map(e -> {
                return e.remove(listener);
            }).anyMatch(e -> e);
        } finally {
            writeLock.unlock();
        }
    }

    private void fireThingTypeFound(ThingTypeUID uid) {
        Objects.requireNonNull(uid, "uid cannot be null");

        final String serviceName = SonyUtil.getServiceName(uid);
        final Integer vers = SonyUtil.getModelVersion(uid);

        final Lock readLock = listenerLock.readLock();
        try {
            readLock.lock();            
            this.listeners.entrySet().forEach(e -> {
                final String modelName = e.getKey();
                if (SonyUtil.isModelMatch(uid, serviceName, modelName)) {
                    final ThingTypeUID maxUid = findMaxForModel(serviceName, modelName);
                    final Integer maxVers = SonyUtil.getModelVersion(maxUid);

                    if (maxVers == null || vers >= maxVers) {
                        e.getValue().stream().forEach(f -> f.thingTypeFound(uid));
                    }
                }
            });
        } finally {
            readLock.unlock();
        }
    }

    protected static int getPropertyInt(Map<String, String> properties, String key) {
        Objects.requireNonNull(properties, "properties cannot be null");
        Validate.notEmpty(key, "key cannot be empty");

        final String prop = getProperty(properties, key);
        try {
            return Integer.parseInt(prop);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Property key " + key + " was not a valid number: " + prop);
        }
    }

    protected static String getProperty(Map<String, String> properties, String key) {
        Objects.requireNonNull(properties, "properties cannot be null");
        Validate.notEmpty(key, "key cannot be empty");

        String prop = null;
        if (properties.containsKey(key)) {
            prop = properties.get(key);
        }

        if (prop == null || StringUtils.isEmpty(prop)) {
            throw new IllegalArgumentException("Property key " + key + " was not found");

        }
        return prop;
    }
}
