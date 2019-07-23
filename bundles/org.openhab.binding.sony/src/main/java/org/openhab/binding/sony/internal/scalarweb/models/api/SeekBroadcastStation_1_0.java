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

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SeekBroadcastStation_1_0 {
    public static final String DIR_FWD = "fwd";
    public static final String DIR_BWD = "bwd";
    public static final String TUN_AUTO = "auto";
    public static final String TUN_MANUAL = "manual";
    private final String direction;
    private final String tuning;

    public SeekBroadcastStation_1_0(boolean fwd, boolean autoTune) {
        direction = fwd ? DIR_FWD : DIR_BWD;
        tuning = autoTune ? TUN_AUTO : TUN_MANUAL;
    }

    public String getDirection() {
        return direction;
    }

    public String getTuning() {
        return tuning;
    }
}
