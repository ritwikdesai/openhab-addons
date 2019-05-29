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
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.sony.internal.scalarweb.gson.GsonUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class GeneralSettings_1_0 {

    private final List<GeneralSetting> generalSettings = new ArrayList<>();

    public GeneralSettings_1_0(ScalarWebResult results) {
        Objects.requireNonNull(results, "results cannot be null");

        final JsonArray rsts = results.getResults();
        if (rsts == null) {
            throw new JsonParseException("No results to deserialize");
        }

        final Gson gson = GsonUtilities.getApiGson();

        for (JsonElement elm : rsts) {
            if (elm.isJsonArray()) {
                for (JsonElement arr : elm.getAsJsonArray()) {
                    generalSettings.add(gson.fromJson(arr, GeneralSetting.class));
                }
            }
        }
    }

    public List<GeneralSetting> getSettings() {
        return generalSettings;
    }

    @Override
    public String toString() {
        return "SpeakerSettings_1_1 [settings=" + generalSettings + "]";
    }
}
