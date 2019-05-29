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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.scalarweb.VersionUtilities;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SupportedApiInfo {
    final String name;
    private final Map<String, SupportedApiVersionInfo> versions;

    private final @Nullable SupportedApiVersionInfo latestVersion;

    public SupportedApiInfo(String name, List<SupportedApiVersionInfo> versions) {
        this.name = name;
        this.versions = Collections
                .unmodifiableMap(versions.stream().collect(Collectors.toMap(k -> k.version, v -> v)));

        final Optional<SupportedApiVersionInfo> latest = versions.stream().max((c1, c2) -> {
            final double d1 = VersionUtilities.parse(c1.version);
            final double d2 = VersionUtilities.parse(c2.version);
            if (d1 < d2) {
                return -1;
            }
            if (d2 > d1) {
                return 1;
            }
            return 0;
        });

        this.latestVersion = latest.isPresent() ? latest.get() : null;
    }

    public String getName() {
        return name;
    }

    public Collection<SupportedApiVersionInfo> getVersions() {
        return versions.values();
    }

    public @Nullable SupportedApiVersionInfo getVersions(String version) {
        return versions.get(version);
    }

    public @Nullable SupportedApiVersionInfo getLatestVersion() {
        return latestVersion;
    }

    @Override
    public String toString() {
        return "SupportedApiInfo [name=" + name + ", versions=" + versions + "]";
    }
}