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
package org.openhab.binding.sony.internal.transports;

/**
 * 
 * @author Tim Roberts - Initial contribution
 */
public class TransportOptionMethod implements TransportOption {
    private static final String METHOD_GET = "get";
    private static final String METHOD_POSTXML = "postxml";
    private static final String METHOD_POSTJSON = "postjson";
    private static final String METHOD_DELETE = "delete";

    public static final TransportOptionMethod GET = new TransportOptionMethod(METHOD_GET);
    public static final TransportOptionMethod POST_XML = new TransportOptionMethod(METHOD_POSTXML);
    public static final TransportOptionMethod POST_JSON = new TransportOptionMethod(METHOD_POSTJSON);
    public static final TransportOptionMethod DELETE = new TransportOptionMethod(METHOD_DELETE);
    
    private final String method;

    private TransportOptionMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    } 
}