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

import org.eclipse.jdt.annotation.Nullable;

/**
 * 
 * @author Tim Roberts - Initial contribution
 */
public class TransportPayloadHttp implements TransportPayload
{
    private final String url;
    private final @Nullable String payload;

    public TransportPayloadHttp(String url) {
        this(url, null);
    }

    public TransportPayloadHttp(String url, @Nullable String payload) {
        this.url = url;
        this.payload = payload;
    }

    public String getUrl() {
        return url;
    }

    public @Nullable String getPayload() {
        return payload;
    }
}
