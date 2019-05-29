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

import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.sony.internal.scalarweb.gson.ScalarWebEventDeserializer;

import com.google.gson.JsonArray;

/**
 * This class represents a web scalar event result (sent to us from the device). This result will be created by the
 * {@link ScalarWebEventDeserializer} when deserializing the event.
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarWebEvent extends AbstractScalarResponse {

    // audio notifications
    public static final String NOTIFYVOLUMEINFORMATION = "notifyVolumeInformation";
    public static final String NOTIFYWIRELESSSURROUNDINFO = "notifyWirelessSurroundInfo";

    // AV Notifications
    public static final String NOTIFYPLAYINGCONTENTINFO = "notifyPlayingContentInfo";
    public static final String NOTIFYEXTERNALTERMINALSTATUS = "notifyExternalTerminalStatus";
    public static final String NOTIFYAVAILABLEPLAYBACKFUNCTION = "notifyAvailablePlaybackFunction";

    // system notifications
    public static final String NOTIFYPOWERSTATUS = "notifyPowerStatus";

    /** The method name for the event */
    private final String method;

    /** The parameters for the event */
    private final JsonArray params;

    /** The event version */
    private final String version;

    /**
     * Instantiates a new scalar web event
     *
     * @param methodName the non-null, non-empty method name
     * @param parmas     the non-null, possibly empty parameters
     * @param version    the non-null, non-empty version
     */
    public ScalarWebEvent(String method, JsonArray params, String version) {
        Validate.notEmpty(method, "method cannot be empty");
        Objects.requireNonNull(params, "params cannot be null");
        Validate.notEmpty(version, "version cannot be empty");

        this.method = method;
        this.params = params;
        this.version = version;
    }

    /**
     * Gets the method name
     *
     * @return the method name
     */
    public String getMethod() {
        return method;
    }

    /**
     * Gets the version
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    @Override
    protected JsonArray getPayload() {
        return params;
    }

    @Override
    public String toString() {
        return "ScalarWebEvent [method=" + method + ", params=" + params + ", version=" + version + "]";
    }
}
