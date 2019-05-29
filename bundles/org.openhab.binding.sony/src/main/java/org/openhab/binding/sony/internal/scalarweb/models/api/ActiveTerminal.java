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
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Sets the active terminal (the power status of a zone)
 *
 * @author Tim Roberts - Initial contribution
 *
 */
@NonNullByDefault
public class ActiveTerminal {
    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive";

    private final String uri;
    private final String active;

    public ActiveTerminal(String uri, String active) {
        Validate.notEmpty(uri, "uri cannot be empty");
        Validate.notEmpty(active, "active cannot be empty");
        this.uri = uri;
        this.active = active;
    }

    public String getUri() {
        return uri;
    }

    public String getActive() {
        return active;
    }

    @Override
    public String toString() {
        return "ActiveTerminal [uri=" + uri + ", active=" + active + "]";
    }
}