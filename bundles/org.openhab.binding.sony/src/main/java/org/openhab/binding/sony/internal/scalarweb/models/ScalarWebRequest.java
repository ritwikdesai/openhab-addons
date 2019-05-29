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

/**
 * This class represents a web scalar method request
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarWebRequest {

    /** The unique identifier of the request */
    private final int id;

    /** The method request number */
    private final String method;

    /** The method version */
    private final String version;

    /** The parameters for the request */
    private final Object[] params;

    /**
     * Instantiates a new scalar web request with no parameters
     *
     * @param id      the unique identifier
     * @param method  the non-null, non-empty method name
     * @param version the non-null, non-empty method version
     */
    public ScalarWebRequest(int id, String method, String version) {
        this(id, method, version, new Object[0]);
    }

    /**
     * Instantiates a new scalar web request with parameters
     *
     * @param id      the unique identifier
     * @param method  the non-null, non-empty method name
     * @param version the non-null, non-empty method version
     * @param params  the non-null, possibly empty list of parameters
     */
    public ScalarWebRequest(int id, String method, String version, Object... params) {
        Validate.notEmpty(method, "method cannot be empty");
        Validate.notEmpty(version, "version cannot be empty");
        Objects.requireNonNull(params, "params cannot be null");

        this.id = id;
        this.method = method;
        this.version = version;
        this.params = params;
    }

    /**
     * Gets the unique request identifier
     *
     * @return the unique request identifier
     */
    public int getId() {
        return id;
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
     * Gets the method version
     *
     * @return the method version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the parameters
     *
     * @return the non-null, possibly empty array of parameters
     */
    public Object[] getParams() {
        return params;
    }
}
