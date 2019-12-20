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
package org.openhab.binding.sony.internal.scalarweb;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;

/**
 * This class represents a web scalar method definition that can be called
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarWebClient implements AutoCloseable {

    /** The device manager */
    private final ScalarWebDeviceManager deviceManager;

    /**
     * Instantiates a new scalar web state.
     *
     * @param deviceManager the non-null device manager
     */
    public ScalarWebClient(ScalarWebDeviceManager deviceManager) {
        Objects.requireNonNull(deviceManager, "deviceManager cannot be null");

        this.deviceManager = deviceManager;
    }

    /**
     * Gets the device manager
     *
     * @return the non-null device manager
     */
    public ScalarWebDeviceManager getDevice() {
        return deviceManager;
    }

    /**
     * Gets the service for the specified name
     *
     * @param serviceName the service name
     * @return the service or null if not found
     */
    public @Nullable ScalarWebService getService(String serviceName) {
        return deviceManager.getService(serviceName);
    }

    @Override
    public String toString() {
        return deviceManager.toString();
    }

    @Override
    public void close() {
        deviceManager.close();
    }
}
