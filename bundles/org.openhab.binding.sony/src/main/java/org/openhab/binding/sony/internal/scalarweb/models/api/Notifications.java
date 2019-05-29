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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class Notifications {
    private final List<Notification> enabled;
    private final List<Notification> disabled;

    public Notifications() {
        this.enabled = new ArrayList<>();
        this.disabled = new ArrayList<>();
    }

    public Notifications(List<Notification> enabled, List<Notification> disabled) {
        this.enabled = enabled;
        this.disabled = disabled;
    }

    public List<Notification> getEnabled() {
        return enabled;
    }

    public List<Notification> getDisabled() {
        return disabled;
    }

    @Override
    public String toString() {
        return "Notifications [enabled=" + enabled + ", disabled=" + disabled + "]";
    }
}
