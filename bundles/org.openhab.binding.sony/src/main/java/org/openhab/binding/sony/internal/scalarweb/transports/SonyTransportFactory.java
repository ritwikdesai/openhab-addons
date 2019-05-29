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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebRequest;
import org.openhab.binding.sony.internal.scalarweb.models.api.ServiceProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SonyTransportFactory {

    private final Logger logger = LoggerFactory.getLogger(SonyTransportFactory.class);

    private final URL baseUrl;
    private final Gson gson;
    private final @Nullable WebSocketClient webSocketClient;
    private final @Nullable ScheduledExecutorService scheduler;

    public SonyTransportFactory(URL baseUrl, Gson gson, @Nullable WebSocketClient webSocketClient,
            @Nullable ScheduledExecutorService scheduler) {
        this.baseUrl = baseUrl;
        this.gson = gson;
        this.webSocketClient = webSocketClient;
        this.scheduler = scheduler;
    }

    public @Nullable SonyTransport<ScalarWebRequest> getSonyTransport(ServiceProtocol serviceProtocol) {
        String protocol;
        if (serviceProtocol.hasWebsocketProtocol()) {
            protocol = SonyTransport.WEBSOCKET;
        } else if (serviceProtocol.hasHttpProtocol()) {
            protocol = SonyTransport.HTTP;
        } else {
            protocol = SonyTransport.AUTO;
        }
        final String serviceName = serviceProtocol.getServiceName();
        final SonyTransport<ScalarWebRequest> transport = getSonyTransport(serviceName, protocol);
        return transport == null ? getSonyTransport(serviceName, SonyTransport.AUTO) : transport;
    }

    public @Nullable SonyTransport<ScalarWebRequest> getSonyTransport(String serviceName) {
        return getSonyTransport(serviceName, SonyTransport.AUTO);
    }

    public @Nullable SonyTransport<ScalarWebRequest> getSonyTransport(String serviceName, String protocol) {
        switch (protocol) {
            case SonyTransport.AUTO:
                final SonyWebSocketTransport wst = createWebSocketTransport(serviceName);
                return wst == null ? createHttpTransport(serviceName) : wst;

            case SonyTransport.HTTP:
                return createHttpTransport(serviceName);

            case SonyTransport.WEBSOCKET:
                return createWebSocketTransport(serviceName);

            default:
                logger.debug("Unknown protocol: {}", protocol);
                return null;
        }
    }

    private @Nullable SonyWebSocketTransport createWebSocketTransport(String serviceName) {
        final WebSocketClient localWebSocketClient = webSocketClient;
        if (localWebSocketClient == null) {
            logger.debug("No websocket client specified - cannot create an websocket transport");
            return null;
        }

        try {
            final String baseFile = baseUrl.getFile();
            final URI uri = new URI(
                    String.format("ws://%s:%d/%s", baseUrl.getHost(), baseUrl.getPort() > 0 ? baseUrl.getPort() : 10000,
                            baseFile + (baseFile.endsWith("/") ? "" : "/")
                                    + (serviceName.startsWith("/") ? serviceName.substring(1) : serviceName)))
                                            .normalize();
            return new SonyWebSocketTransport(localWebSocketClient, uri, gson, scheduler);
        } catch (URISyntaxException | InterruptedException | ExecutionException | TimeoutException | IOException e) {
            logger.debug("Exception occurred creating transport: {}", e.getMessage());
            return null;
        }
    }

    private SonyHttpTransport createHttpTransport(String serviceName) {
        final String base = baseUrl.toString();
        final String baseUrlString = base + (base.endsWith("/") ? "" : "/")
                + (serviceName.startsWith("/") ? serviceName.substring(1) : serviceName);

        return new SonyHttpTransport(baseUrlString, gson);
    }
}
