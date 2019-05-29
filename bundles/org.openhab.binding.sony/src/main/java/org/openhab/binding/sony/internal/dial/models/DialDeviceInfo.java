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
package org.openhab.binding.sony.internal.dial.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * The class representing a DIAL device and it's information. The element being deserialized will typically look like:
 *
 * <pre>
 * {@code
       <av:X_DIALEX_DeviceInfo xmlns:av="urn:schemas-sony-com:av">
         <av:X_DIALEX_AppsListURL>http://192.168.1.12:50202/appslist</av:X_DIALEX_AppsListURL>
         <av:X_DIALEX_DeviceID>B0:00:04:07:DD:7E</av:X_DIALEX_DeviceID>
         <av:X_DIALEX_DeviceType>BDP_DIAL</av:X_DIALEX_DeviceType>
       </av:X_DIALEX_DeviceInfo>
 * }
 * </pre>
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
@XStreamAlias("X_DIALEX_DeviceInfo")
class DialDeviceInfo {

    /** The apps list url. */
    @XStreamAlias("X_DIALEX_AppsListURL")
    private @Nullable String appsListUrl;

    /** The device id. */
    @XStreamAlias("X_DIALEX_DeviceID")
    private @Nullable String deviceId;

    /** The device type. */
    @XStreamAlias("X_DIALEX_DeviceType")
    private @Nullable String deviceType;

    /** The list of {@link DialApp} */
    private @Nullable List<DialApp> apps;

    /**
     * Private constructor to construct the object - only called from the {@link #withApps(List)}
     *
     * @param appsListUrl the possibly null, possibly empty application list URL
     * @param deviceId    the possibly null, possibly empty application device ID
     * @param deviceType  the possibly null, possibly empty application device type
     * @param apps        the non-null, possibly empty list of {@link DialApp}
     */
    private DialDeviceInfo(@Nullable String appsListUrl, @Nullable String deviceId, @Nullable String deviceType,
            List<DialApp> apps) {
        Objects.requireNonNull(apps, "apps cannot be null");
        this.appsListUrl = appsListUrl;
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.apps = apps;
    }

    /**
     * Constructs a new {@link DialDeviceInfo} with the new list of {@link DialApp}
     *
     * @param apps a non-null, possibly empty list of {@link DialApp}
     * @return a non-null {@link DialDeviceInfo}
     */
    DialDeviceInfo withApps(List<DialApp> apps) {
        Objects.requireNonNull(apps, "apps cannot be null");
        return new DialDeviceInfo(appsListUrl, deviceId, deviceType, apps);
    }

    /**
     * Get's the application list URL
     *
     * @return a possibly null, possibly empty application list URL
     */
    @Nullable
    String getAppsListUrl() {
        return appsListUrl;
    }

    /**
     * Gets the device id
     *
     * @return a possibly null, possibly empty device id
     */

    public @Nullable String getDeviceId() {
        return deviceId;
    }

    /**
     * Gets the device type
     *
     * @return a possibly null, possibly empty device type
     */
    public @Nullable String getDeviceType() {
        return deviceType;
    }

    /**
     * Gets the list of {@link DialApp}
     *
     * @return the non-null, possibly empty list of {@link DialApp}
     */
    public List<DialApp> getDialApps() {
        return Collections.unmodifiableList(apps == null ? new ArrayList<>() : apps);
    }
}
