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
package org.openhab.binding.sony.internal.scalarweb.gson;

import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebEvent;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.openhab.binding.sony.internal.scalarweb.models.api.SupportedApi;
import org.openhab.binding.sony.internal.scalarweb.models.api.SupportedApiInfo;
import org.openhab.binding.sony.internal.scalarweb.models.api.SupportedApiVersionInfo;

/**
 * This utilities class provides standard gson related utility methods
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class GsonUtilities {
    /**
     * Creates a generic {@link Gson} object to use for generic serialization/deserialization
     * @return a non-null Gson object
     */
    public static Gson getDefaultGson() {
        return new GsonBuilder().disableHtmlEscaping().create();
    }

    /**
     * Creates a {@link Gson} object suited for API operations and will include a number of custom deserializers
     * @return a non-null Gson object
     */
    public static Gson getApiGson() {
        return new GsonBuilder().disableHtmlEscaping()
                .registerTypeAdapter(ScalarWebEvent.class, new ScalarWebEventDeserializer())
                .registerTypeAdapter(ScalarWebResult.class, new ScalarWebResultDeserializer())
                .registerTypeAdapter(SupportedApi.class, new SupportedApiDeserializer())
                .registerTypeAdapter(SupportedApiInfo.class, new SupportedApiInfoDeserializer())
                .registerTypeAdapter(SupportedApiVersionInfo.class, new SupportedApiVersionInfoDeserializer()).create();
    }

        /**
     * Converts the json object into an array based on the element specified
     *
     * @param jo          the non-null json object to convert
     * @param elementName the non-null, non-empty element name to use
     * @return the array the array returned
     */
    public static JsonArray getArray(JsonObject jo, String elementName) {
        Objects.requireNonNull(jo, "jo cannot be null");
        Validate.notEmpty(elementName, "elementName cannot be empty");

        final JsonArray ja = new JsonArray();

        final JsonElement sing = jo.get(elementName);
        if (sing != null && sing.isJsonArray()) {
            ja.addAll(sing.getAsJsonArray());
        }

        final JsonElement plur = jo.get(elementName + "s");
        if (plur != null && plur.isJsonArray()) {
            ja.addAll(plur.getAsJsonArray());
        }

        return ja;
    }
}
