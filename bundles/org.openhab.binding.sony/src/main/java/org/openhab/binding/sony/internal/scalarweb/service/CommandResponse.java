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
package org.openhab.binding.sony.internal.scalarweb.service;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The implementation of the protocol handles the Audio service
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class CommandResponse {
    /** True if the call was successful, false otherwise */
    private final boolean success;

    /** The optional message if not successful */
    @Nullable
    private final String message;

    /** The optional message if not successful */
    @Nullable
    private final String results;

    public CommandResponse(String results) {
        this.success = true;
        this.message = null;
        this.results = results;
    }

    public CommandResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.results = null;
    }

    @Override
    public String toString() {
        return "CommandResponse [success=" + success + ", message=" + message + ", results=" + results + "]";
    }
}
