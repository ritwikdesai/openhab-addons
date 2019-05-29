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

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SupportedApiServices {
    private final String[] services;

    public SupportedApiServices(String... services) {
        this.services = services;
    }

    public String[] getServices() {
        return services;
    }

    @Override
    public String toString() {
        return "SupportedApiServices [services=" + Arrays.toString(services) + "]";
    }
}
