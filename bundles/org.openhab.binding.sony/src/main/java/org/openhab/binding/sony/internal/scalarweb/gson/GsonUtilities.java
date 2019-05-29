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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebEvent;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.openhab.binding.sony.internal.scalarweb.models.api.SupportedApi;
import org.openhab.binding.sony.internal.scalarweb.models.api.SupportedApiInfo;
import org.openhab.binding.sony.internal.scalarweb.models.api.SupportedApiVersionInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class GsonUtilities {
    public static Gson getDefaultGson() {
        return new GsonBuilder().disableHtmlEscaping().create();
    }

    public static Gson getApiGson() {
        return new GsonBuilder().disableHtmlEscaping()
                .registerTypeAdapter(ScalarWebEvent.class, new ScalarWebEventDeserializer())
                .registerTypeAdapter(ScalarWebResult.class, new ScalarWebResultDeserializer())
                .registerTypeAdapter(SupportedApi.class, new SupportedApiDeserializer())
                .registerTypeAdapter(SupportedApiInfo.class, new SupportedApiInfoDeserializer())
                .registerTypeAdapter(SupportedApiVersionInfo.class, new SupportedApiVersionInfoDeserializer()).create();
    }
}
