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

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.sony.internal.ExpiringMap;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.scalarweb.ScalarUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebEvent;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebRequest;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SonyWebSocketTransport extends AbstractSonyTransport {
    private final Logger logger = LoggerFactory.getLogger(SonyWebSocketTransport.class);

    private static final int CONN_EXPIRE_TIMEOUT_SECONDS = 10;
    private static final int CMD_EXPIRE_TIMEOUT_SECONDS = 30;
    private static final int PING_SECONDS = 5;

    private final URI uri;
    private final Gson gson;
    private final ExpiringMap<Integer, CompletableFuture<TransportResult>> futures;

    private @Nullable Session session;

    private int ping = 0;

    private final @Nullable ScheduledFuture<?> pingTask;

    private final JsonParser parser = new JsonParser();

    public SonyWebSocketTransport(WebSocketClient webSocketClient, URI uri, Gson gson,
            @Nullable ScheduledExecutorService scheduler)
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        super(uri);
        Objects.requireNonNull(webSocketClient, "webSocketClient cannot be null");
        Objects.requireNonNull(uri, "uri cannot be null");
        Objects.requireNonNull(gson, "gson cannot be null");

        this.gson = gson;
        this.uri = uri;

        futures = new ExpiringMap<Integer, CompletableFuture<TransportResult>>(scheduler, CMD_EXPIRE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS);
        futures.addExpireListener((k, v) -> {
            v.cancel(true);
        });

        logger.debug("Starting websocket connection to {}", uri);
        webSocketClient.connect(new WebSocketCallback(), uri, new ClientUpgradeRequest())
                .get(CONN_EXPIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        logger.debug("Websocket connection successful to {}", uri);

        // Setup pinging to prevent connection from timing out due to inactivity
        if (scheduler == null) {
            pingTask = null;
        } else {
            pingTask = scheduler.scheduleWithFixedDelay(() -> {
                final Session localSession = session;
                if (localSession != null) {
                    final RemoteEndpoint remote = localSession.getRemote();

                    final ByteBuffer payload = ByteBuffer.allocate(4).putInt(ping++);
                    try {
                        logger.debug("Pinging {}", uri);
                        remote.sendPing(payload);
                    } catch (IOException e) {
                        logger.debug("Pinging {} failed: {}", uri, e.getMessage());
                    }
                }
            }, PING_SECONDS, PING_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    public String getProtocolType() {
        return SonyTransport.WEBSOCKET;
    }

    @Override
    public void close() {
        futures.close();

        // if there is an old web socket then clean up and destroy
        final Session localSession = session;
        if (localSession != null && !localSession.isOpen()) {
            logger.debug("Closing session {}", uri);
            try {
                localSession.close();
            } catch (Exception e) {
                logger.debug("Closing of session {} failed: {}", uri, e.getMessage(), e);
            }
        }
        session = null;

        if (pingTask != null) {
            SonyUtil.cancel(pingTask);
        }
    }

    @Override
    public CompletableFuture<TransportResult> execute(TransportPayload payload, TransportOption... options) {
        if (!(payload instanceof TransportPayloadScalarWebRequest)) {
            throw new IllegalArgumentException(
                    "payload must be a TransportPayloadRequest: " + payload.getClass().getName());
        }

        final Session localSession = session;
        if (localSession == null) {
            return CompletableFuture.completedFuture(new TransportResultScalarWebResult(
                    ScalarUtilities.createErrorResult(HttpStatus.INTERNAL_SERVER_ERROR_500,
                            "No session established yet - wait for it to be connected")));
        }

        final ScalarWebRequest cmd = ((TransportPayloadScalarWebRequest) payload).getPayload();
        final String jsonRequest = gson.toJson(cmd);
        try {
            final CompletableFuture<TransportResult> future = new CompletableFuture<>();
            futures.put(cmd.getId(), future);

            logger.debug("Sending {} to {}", jsonRequest, uri);
            localSession.getRemote().sendString(jsonRequest);
            return future;
        } catch (IOException e) {
            logger.debug("IOException sending {} to {}: {}", jsonRequest, uri, e.getMessage(), e);
            return CompletableFuture.completedFuture(new TransportResultScalarWebResult(
                    ScalarUtilities.createErrorResult(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage())));
        }
    }

    @WebSocket
    public class WebSocketCallback {
        @OnWebSocketConnect
        public void onConnect(Session session) {
            logger.debug("Connected successfully to server {}", uri);
            SonyWebSocketTransport.this.session = session;
        }

        @OnWebSocketMessage
        public void onMessage(String message) {
            try {
                final JsonObject json = parser.parse(message).getAsJsonObject();
                if (json.has("id")) {
                    final ScalarWebResult result = gson.fromJson(json, ScalarWebResult.class);
                    final Integer resultId = result.getId();
                    final CompletableFuture<TransportResult> future = futures.get(resultId);
                    if (future != null) {
                        logger.debug("Response received from server: {}", message);
                        futures.remove(resultId);
                        future.complete(new TransportResultScalarWebResult(result));
                    } else {
                        logger.debug("Response received from server but a waiting command wasn't found - ignored: {}",
                                message);
                    }
                } else {
                    final ScalarWebEvent event = gson.fromJson(json, ScalarWebEvent.class);
                    logger.debug("Event received from server: {}", message);
                    fireEvent(event);
                }
            } catch (JsonParseException e) {
                logger.debug("JSON parsing error: {} for {}", e.getMessage(), message, e);
            }
        }

        @OnWebSocketError
        public void onError(Throwable t) {
            if (t instanceof UpgradeException) {
                final UpgradeException e = (UpgradeException) t;
                // 404 happens when the individual service has no websocket connection
                // but there is a websocket server listening for other services
                if (e.getResponseStatusCode() == HttpStatus.NOT_FOUND_404) {
                    logger.debug("No websocket listening for specific service {}", e.getRequestURI());
                    return;
                } else if (e.getResponseStatusCode() == 0) {
                    // Weird second exception thrown when you get a connection refused
                    // when using upgrade - ignore this since it was logged below
                    return;
                }
            }

            // suppress stack trace on connection refused
            if (StringUtils.containsIgnoreCase(t.getMessage(), "connection refused")) {
                logger.debug("Connection refused for {}: {}", uri, t.getMessage());
                return;
            }

            // suppress stack trace on connection refused
            if (StringUtils.containsIgnoreCase(t.getMessage(), "idle timeout")) {
                logger.debug("Idle Timeout for {}: {}", uri, t.getMessage());
                return;
            }

            logger.debug("Exception occurred during websocket communication for {}: {}", uri, t.getMessage(), t);
            fireOnError(t);
        }

        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
            final Session localSession = session;
            if (localSession != null) {
                logger.debug("Closing session from close event {}", uri);
                localSession.close();
            }
            session = null;
        }
    }
}
