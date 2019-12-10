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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonSyntaxException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.providers.models.SonyDeviceCapability;
import org.openhab.binding.sony.internal.providers.models.SonyThingDefinition;

/**
 * An implementation of a {@link SonySource} that will source thing types from
 * json files within the user data folder
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SonyFolderSource extends AbstractSonySource {
    /** Various constants used by the SonyFolderSource */
    private static final String FOLDERBASE = ConfigConstants.getUserDataFolder() + File.separator + "sony";
    private static final String PROP_WATCHDOG_INTERVAL = "SonyFolderSource.WatchDog.Interval";
    private static final String PROP_THINGTYPES = "SonyFolderSource.Folder.ThingTypes";
    private static final String PROP_DEFTYPES = "SonyFolderSource.Folder.DefinitionTypes";
    private static final String PROP_DEFCAPS = "SonyFolderSource.Folder.DefinitionCapabilities";

    /** Folder paths */
    private final String folderThingTypes;
    private final String folderDefThingTypes;
    private final String folderDefCapability;

    /** The folder watcher (null if none being watched) */
    private final AtomicReference<@Nullable Future<?>> watcher = new AtomicReference<>(null);

    /** The folder watch dog (null if none being watched) */
    private final AtomicReference<@Nullable Future<?>> watchDog = new AtomicReference<>(null);

    /**
     * Constructs the source and starts the various threads
     * 
     * @param scheduler  a non-null scheduler to use
     * @param properties a non-null, possibly empty map of properties
     */
    public SonyFolderSource(ScheduledExecutorService scheduler, Map<String, String> properties) {
        Objects.requireNonNull(scheduler, "scheduler cannot be null");
        Objects.requireNonNull(properties, "properties cannot be null");

        // create the folders we will use
        folderThingTypes = FOLDERBASE + getProperty(properties, PROP_THINGTYPES);;
        folderDefThingTypes = FOLDERBASE + getProperty(properties, PROP_DEFTYPES);
        folderDefCapability = FOLDERBASE + getProperty(properties, PROP_DEFCAPS);

        SonyUtil.createFolder(folderThingTypes);
        SonyUtil.createFolder(folderDefThingTypes);
        SonyUtil.createFolder(folderDefCapability);

        // Setup our watcher
        watcher.getAndSet(scheduler.submit(new Watcher()));

        // Keeps the watcher alive in case it encounters an error
        final int watchDogTime = getPropertyInt(properties, PROP_WATCHDOG_INTERVAL);
        SonyUtil.cancel(watchDog.getAndSet(scheduler.schedule(() -> {
            final Future<?> fut = watcher.get();
            if (fut == null || fut.isDone()) {
                SonyUtil.cancel(watcher.getAndSet(scheduler.submit(new Watcher())));
            }
        }, watchDogTime, TimeUnit.SECONDS)));
    }

    /**
     *
     * This private helper class will watch the file system for changes and recreate
     * thing types if a file is added/changed/deleted
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

                readFiles(folderThingTypes);

                logger.debug("Starting watch for new/modified entries {}", folderThingTypes);
                try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                    final Path path = Paths.get(folderThingTypes);

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
                                readFiles(folderThingTypes);
                            } else {
                                final Path eventPath = (Path) event.context();
                                if (eventPath == null) {
                                    logger.debug("Watch notification without an path in the context: {}", event);
                                } else {
                                    readFile(eventPath.toAbsolutePath().toString());
                                }
                            }
                        }
                        key.reset();
                    }
                }
            } catch (JsonSyntaxException | IOException e) {
                logger.info("Watcher encountered an exception: {}", e.getMessage(), e);
            } catch (InterruptedException e) {
                logger.debug("Watcher was interrupted");
            }
        }
    }

    @Override
    public void writeThingDefinition(SonyThingDefinition thingTypeDefinition) {
        Objects.requireNonNull(thingTypeDefinition, "thingTypeDefinition cannot be null");

        final File file = new File(
                folderDefThingTypes + File.separator + thingTypeDefinition.getModelName() + "." + JSONEXT);
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
                folderDefCapability + File.separator + deviceCapability.getModelName() + "." + JSONEXT);
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
