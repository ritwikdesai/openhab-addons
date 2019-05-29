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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ParentalInfo {
    private @Nullable String rating;
    private @Nullable String system;
    private @Nullable String country;

    public @Nullable String getRating() {
        return rating;
    }

    public @Nullable String getSystem() {
        return system;
    }

    public @Nullable String getCountry() {
        return country;
    }

    @Override
    public String toString() {
        return "ParentalInfo [rating=" + rating + ", system=" + system + ", country=" + country + "]";
    }
}
