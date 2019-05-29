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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SupportedApiVersionInfo {
    private final String authLevel;
    private final Set<String> protocols;
    final String version;

    public SupportedApiVersionInfo(String authLevel, Set<String> protocols, String version) {
        this.authLevel = authLevel;
        this.protocols = protocols;
        this.version = version;
    }

    public String getAuthLevel() {
        return authLevel;
    }

    public Set<String> getProtocols() {
        return protocols;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "SupportedApiVersionInfo [authLevel=" + authLevel + ", protocols=" + protocols + ", version=" + version
                + "]";
    }
}