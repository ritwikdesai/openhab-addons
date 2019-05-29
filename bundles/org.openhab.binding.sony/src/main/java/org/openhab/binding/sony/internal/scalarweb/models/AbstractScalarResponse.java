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
package org.openhab.binding.sony.internal.scalarweb.models;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.scalarweb.ScalarUtilities;
import org.openhab.binding.sony.internal.scalarweb.gson.GsonUtilities;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractScalarResponse {
    protected abstract JsonArray getPayload();

    public <T> T as(Class<T> clazz, GetClass<T> getC) throws IOException {
        return this.as(getC.getC());
    }

    public <T> List<T> asArray(Class<T> clazz, GetClass<T> getC) throws IOException {
        return (List<T>) this.asArray(getC.getC());
    }

    // public <T, S extends T> List<T> asArray(Class<T> clazz, GetClass<S> getC) throws IOException {
    // return (List<T>) this.asArray(getC.getC());
    // }

    public interface GetClass<T> {
        Class<? extends T> getC();
    }

    /**
     * Converts this generic response into the specified type
     *
     * @param       <T> the generic type that will be returned
     * @param clazz the class to cast to
     * @return the object cast to class
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public <T> T as(Class<T> clazz) throws IOException {
        // First see if there is a constructor that takes a ScalarWebResult (us)
        // If so - call it with us
        // Otherwise try to use GSON to construct the class and set the fields
        try {
            Constructor<T> constr = clazz.getConstructor(this.getClass());
            return constr.newInstance(this);
        } catch (NoSuchMethodException e) {
            final JsonArray localResults = getPayload();
            if (isBlank(localResults)) {
                return null;
            } else if (localResults.size() == 1) {
                JsonElement elm = localResults.get(0);
                if (elm.isJsonArray()) {
                    final JsonArray arry = elm.getAsJsonArray();
                    if (arry.size() == 1) {
                        elm = arry.get(0);
                    } else {
                        elm = arry;
                    }
                }
                final Gson gson = GsonUtilities.getApiGson();

                if (elm.isJsonObject()) {
                    final JsonObject jobj = elm.getAsJsonObject();
                    jobj.add("fieldNames", ScalarUtilities.getFields(jobj));
                    return gson.fromJson(jobj, clazz);
                } else {
                    return gson.fromJson(elm, clazz);
                }
            }
            // logger.debug(">>> Couldn't do it", e);
            throw new IllegalArgumentException(
                    "Cannot construct ScalarWebResult for " + clazz + " with results: " + localResults, e);

        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            // logger.debug(">>> Couldn't do it thrown", e);
            throw new IllegalArgumentException(
                    "Constructor with ScalarWebResult argument can't be called: " + e.getMessage(), e);
        }
    }

    /**
     * Converts this generic response into an array of the specified type
     *
     * @param       <T> the generic type that will be returned
     * @param clazz the class to cast to
     * @return a non-null, possibly empty list of objects converted to the class
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public <T> List<T> asArray(Class<T> clazz) throws IOException {
        // use a local logger
        // final Logger logger = LoggerFactory.getLogger(ScalarWebResult.class);

        final JsonArray localResults = getPayload();
        final Gson gson = new Gson();
        final List<T> rc = new ArrayList<T>();

        for (JsonElement resElm : localResults) {
            if (resElm.isJsonArray()) {
                for (JsonElement elm : resElm.getAsJsonArray()) {
                    if (elm.isJsonObject()) {
                        final JsonObject jobj = elm.getAsJsonObject();
                        jobj.add("fieldNames", ScalarUtilities.getFields(jobj));
                        rc.add(gson.fromJson(jobj, clazz));
                    } else {
                        rc.add(gson.fromJson(elm, clazz));
                    }
                }
            } else {
                if (resElm.isJsonObject()) {
                    final JsonObject jobj = resElm.getAsJsonObject();
                    jobj.add("fieldNames", ScalarUtilities.getFields(jobj));
                    rc.add(gson.fromJson(jobj, clazz));
                } else {
                    rc.add(gson.fromJson(resElm, clazz));
                }
            }
        }
        return rc;
    }

    /**
     * Utility method to check if the associated array is empty. This will include if the array consists of ONLY other
     * arrays and those arrays are empty
     *
     * @param arry the json array to check
     * @return true if empty or null, false otherwise
     */
    protected static boolean isBlank(@Nullable JsonArray arry) {
        if (arry == null || arry.size() == 0) {
            return true;
        }
        for (JsonElement elm : arry) {
            if (elm.isJsonArray()) {
                if (!isBlank(elm.getAsJsonArray())) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }
}
