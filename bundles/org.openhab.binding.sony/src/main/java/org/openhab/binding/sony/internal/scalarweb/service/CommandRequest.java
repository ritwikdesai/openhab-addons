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
public class CommandRequest {
    private @Nullable String baseUrl;
    private @Nullable String serviceName;
    private @Nullable String transport;
    private @Nullable String command;
    private @Nullable String version;
    private @Nullable String parms;

    public @Nullable String getBaseUrl() {
        return baseUrl;
    }

    public @Nullable String getServiceName() {
        return serviceName;
    }

    public @Nullable String getTransport() {
        return transport;
    }

    public @Nullable String getCommand() {
        return command;
    }

    public @Nullable String getVersion() {
        return version;
    }

    public @Nullable String getParms() {
        return parms;
    }

    @Override
    public String toString() {
        return "CommandRequest [baseUrl=" + baseUrl + ", serviceName=" + serviceName + ", transport=" + transport
                + ", command=" + command + ", version=" + version + ", parms=" + parms + "]";
    }
}
