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
package org.openhab.binding.sony.internal.scalarweb.transports;

import java.io.Closeable;
import java.util.concurrent.Future;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public interface SonyTransport<T> extends Closeable {
    String AUTO = "auto";
    String HTTP = "xhrpost:jsonizer";
    String WEBSOCKET = "websocket:jsonizer";
    String IRCC = "ircc";

    String OPTION_HEADER = "header";
    String OPTION_FILTER = "filter";

    /**
     * Execute the specified json request with the specified HTTP headers
     *
     * @param jsonRequest the non-null, non-empty command to execute (json or xml body)
     * @param headers     the possibly null, possibly empty list of headers to include (dependent on the transport)
     * @return the non-null future result
     */
    public Future<ScalarWebResult> execute(T cmd);

    public Future<ScalarWebResult> executeRaw(String cmd);

    public void addOption(String optionName, Object option);

    public void removeOption(String optionName, Object option);

    public void addListener(SonyTransportListener listener);

    public boolean removeListener(SonyTransportListener listener);

    public String getProtocolType();

    @Override
    public void close();
}
