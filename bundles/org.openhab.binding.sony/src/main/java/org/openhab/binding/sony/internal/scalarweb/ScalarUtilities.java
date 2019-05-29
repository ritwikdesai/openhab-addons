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
package org.openhab.binding.sony.internal.scalarweb;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * A utilities class for scalar web
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarUtilities {

    /**
     * Creates the error result from the code and reason
     *
     * @param httpCode the http code
     * @param reason   the reason
     * @return the scalar web result
     */
    public static ScalarWebResult createErrorResult(int httpCode, String reason) {
        return new ScalarWebResult(new HttpResponse(httpCode, reason));
    }

    /**
     * Gets the fields from a json object
     *
     * @param obj the non-null json object
     * @return the non-null, possibly empty fields
     */
    public static JsonArray getFields(JsonObject obj) {
        Objects.requireNonNull(obj, "obj cannot be null");
        final JsonArray arry = new JsonArray();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            arry.add(new JsonPrimitive(entry.getKey()));
        }

        return arry;
    }
}
