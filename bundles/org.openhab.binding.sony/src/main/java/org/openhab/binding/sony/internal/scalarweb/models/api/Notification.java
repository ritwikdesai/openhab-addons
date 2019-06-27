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

import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The class represents a specific notification and is used for serialization/deserialization only
 *
 * @author Tim Roberts - Initial contribution
 */
public class Notification {
    /** The name of the notification */
    private @Nullable final String name;

    /** The version of the notification */
    private @Nullable final String version;

    /**
     * Constructs the notification from the name/version
     * @param name a non-null, non-empty name
     * @param version a non-null, non-empty version
     */
    public Notification(String name, String version) {
        Validate.notEmpty(name, "name cannot be empty");
        Validate.notEmpty(version, "version cannot be empty");
        
        this.name = name;
        this.version = version;
    }

    /**
     * Get's the name for this notification
     * @return the name
     */
    public @Nullable String getName() {
        return name;
    }

    /**
     * Get's the version for this notification
     * @return the version
     */
    public @Nullable String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Notification [name=" + name + ", version=" + version + "]";
    }
}