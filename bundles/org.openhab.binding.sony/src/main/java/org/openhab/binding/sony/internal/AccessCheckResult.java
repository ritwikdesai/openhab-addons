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
package org.openhab.binding.sony.internal;

import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.openhab.binding.sony.internal.net.HttpResponse;

/**
 * This enum represents what type of action is needed when we first connect to the device
 * 
 * @author Tim Roberts - Initial contribution
 */
public class AccessCheckResult {
    /** OK - device either needs no pairing or we have already paird */
    public static final AccessCheckResult OK = new AccessCheckResult("ok", "OK");
    public static final AccessCheckResult NEEDSPAIRING = new AccessCheckResult("needspairing", "Device needs pairing");
    public static final AccessCheckResult SERVICEMISSING = new AccessCheckResult("servicemissing",
            "Service is missing");
    /** Device needs pairing but the display is off */
    public static final AccessCheckResult DISPLAYOFF = new AccessCheckResult("displayoff",
            "Unable to request an access code - Display is turned off (must be on to see code)");
    /** Need to be in the home menu */
    public static final AccessCheckResult HOMEMENU = new AccessCheckResult("homemenu",
            "Unable to request an access code - HOME menu not displayed on device. Please display the home menu and try again.");
    /** Need to be in the home menu */
    public static final AccessCheckResult PENDING = new AccessCheckResult("pending",
            "Access Code requested. Please update the Access Code with what is shown on the device screen.");
    public static final AccessCheckResult NOTACCEPTED = new AccessCheckResult("notaccepted",
            "Access code was not accepted - please either request a new one or verify number matches what's shown on the device.");
    /** Some other error */
    public static final String OTHER = "other";

    private String code;
    private String msg;

    /**
     * Creates the result from the code/msg
     *
     * @param code the non-null, non-empty code
     * @param msg  the non-null, non-empty msg
     */
    public AccessCheckResult(String code, String msg) {
        Validate.notEmpty(code, "code cannot be empty");
        Validate.notEmpty(msg, "msg cannot be empty");

        this.code = code;
        this.msg = msg;
    }

    /**
     * Constructs the result from the response
     * @param resp the non-null response
     */
    public AccessCheckResult(HttpResponse resp) {
        Objects.requireNonNull(resp, "resp cannot be null");
        this.code = AccessCheckResult.OTHER;

        final String content = resp.getContent();
        this.msg = resp.getHttpCode() + " - " + (StringUtils.isEmpty(content) ? resp.getHttpReason() : content);
    }

    /**
     * Returns the related code
     *
     * @return a non-null, non-empty code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Returns the related message
     *
     * @return a non-null, non-empty message
     */
    public String getMsg() {
        return this.msg;
    }
}
