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

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class GeneralSettingsRequest {
    private final List<Setting> settings;

    public GeneralSettingsRequest(String target, String value) {
        settings = Collections.singletonList(new Setting(target, value));
    }

    @Override
    public String toString() {
        return "GeneralSettingsRequest [settings=" + settings + "]";
    }

    private class Setting {
        private final String target;
        private final String value;

        private Setting(String target, String value) {
            this.target = target;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Setting [target=" + target + ", value=" + value + "]";
        }
    }
}
