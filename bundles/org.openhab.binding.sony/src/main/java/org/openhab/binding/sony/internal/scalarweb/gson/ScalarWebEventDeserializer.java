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

import java.lang.reflect.Type;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebEvent;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * This class represents the deserializer to deserialize a json element to a {@link ScalarWebEvent}
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarWebEventDeserializer implements JsonDeserializer<ScalarWebEvent> {
    @Override
    public ScalarWebEvent deserialize(@Nullable JsonElement je, @Nullable Type type,
            @Nullable JsonDeserializationContext context) throws JsonParseException {
        Objects.requireNonNull(je, "je cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        if (je instanceof JsonObject) {
            final JsonObject jo = je.getAsJsonObject();

            final JsonElement methodElm = jo.get("method");
            final JsonElement versionElm = jo.get("version");

            return new ScalarWebEvent(methodElm.getAsString(), getArray(jo, "params"), versionElm.getAsString());
        }
        throw new JsonParseException("The json element isn't a JsonObject and cannot be deserialized");
    }

    /**
     * Converts the json object into an array based on the element specified
     *
     * @param jo          the json object to convert
     * @param elementName the element name to use
     * @return the array the array returned
     */
    private JsonArray getArray(JsonObject jo, String elementName) {
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
