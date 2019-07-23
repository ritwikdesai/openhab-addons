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

import org.openhab.binding.sony.internal.net.HttpResponse;

/**
 * 
 * @author Tim Roberts - Initial contribution
 */
public class TransportResultHttpResponse implements TransportResult {
    private final HttpResponse response;

    public TransportResultHttpResponse(int code, String msg) {
        this.response = new HttpResponse(code, msg);
    }

    public TransportResultHttpResponse(HttpResponse response) {
        this.response = response;
    }

    public HttpResponse getResponse() {
        return response;
    }
}