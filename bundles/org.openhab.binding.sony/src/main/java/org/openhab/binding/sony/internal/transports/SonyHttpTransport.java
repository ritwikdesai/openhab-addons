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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.sony.internal.net.Header;
import org.openhab.binding.sony.internal.net.HttpRequest;
import org.openhab.binding.sony.internal.net.NetUtil;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SonyHttpTransport extends AbstractSonyTransport {
    /** The logger */
    protected Logger logger = LoggerFactory.getLogger(SonyHttpTransport.class);

    /** The HTTP request object to use */
    private final HttpRequest requestor;

    /** GSON used to serialize/deserialize objects */
    private final Gson gson;

    /**
     *
     * @param baseUrl
     * @param gson
     * @param filters
     * @throws MalformedURLException
     */
    public SonyHttpTransport(String baseUrl, Gson gson) throws MalformedURLException {
        super(new URL(baseUrl));
        Objects.requireNonNull(gson, "gson cannot be null");

        requestor = new HttpRequest();

        requestor.addHeader("User-Agent", "OpenHab/Sony/Binding");
        requestor.addHeader("X-CERS-DEVICE-INFO", "OpenHab/Sony/Binding");
        requestor.addHeader("X-CERS-DEVICE-ID", NetUtil.getDeviceId());
        requestor.addHeader("Connection", "close");

        this.requestor.register(new SonyContentTypeFilter());
        this.requestor.register(new SonyAuthFilter(getBaseUrl(), () -> {
            return getOptions(TransportOptionAutoAuth.class).stream().anyMatch(e -> e.isAutoAuth());
        }));
        this.setOption(TransportOptionAutoAuth.FALSE);

        this.gson = gson;
    }

    @Override
    public CompletableFuture<? extends TransportResult> execute(TransportPayload payload, TransportOption... options) {
        final TransportOptionMethod method = getOptions(TransportOptionMethod.class, options).stream().findFirst()
                .orElse(TransportOptionMethod.POST_JSON);

        if (method == TransportOptionMethod.GET) {
            if (!(payload instanceof TransportPayloadHttp)) {
                throw new IllegalArgumentException(
                        "payload must be a TransportPayloadHttp: " + payload.getClass().getName());
            }

            return executeGet((TransportPayloadHttp) payload, options);
        } else if (method == TransportOptionMethod.DELETE) {
            if (!(payload instanceof TransportPayloadHttp)) {
                throw new IllegalArgumentException(
                        "payload must be a TransportPayloadHttp: " + payload.getClass().getName());
            }

            return executeDelete((TransportPayloadHttp) payload, options);
        } else if (method == TransportOptionMethod.POST_XML) {
            if (!(payload instanceof TransportPayloadHttp)) {
                throw new IllegalArgumentException(
                        "payload must be a TransportPayloadHttp: " + payload.getClass().getName());
            }

            return executePostXml((TransportPayloadHttp) payload, options);
        } else {
            if (payload instanceof TransportPayloadScalarWebRequest) {
                return executePostJson((TransportPayloadScalarWebRequest) payload, options).thenApply(r -> {
                    final ScalarWebResult res = gson.fromJson(r.getResponse().getContent(), ScalarWebResult.class);
                    return new TransportResultScalarWebResult(res);
                });
            } else if (payload instanceof TransportPayloadHttp) {
                return executePostJson((TransportPayloadHttp) payload, options);
            } else {
                throw new IllegalArgumentException(
                        "payload must be a TransportPayloadHttp or TransportPayloadScalarWebRequest: "
                                + payload.getClass().getName());
            }
        }
    }

    private CompletableFuture<TransportResultHttpResponse> executeGet(TransportPayloadHttp cmd,
            TransportOption... options) {
        final Header[] headers = getHeaders(options);
        return CompletableFuture
                .completedFuture(new TransportResultHttpResponse(requestor.sendGetCommand(cmd.getUrl(), headers)));
    }

    private CompletableFuture<TransportResultHttpResponse> executeDelete(TransportPayloadHttp cmd,
            TransportOption... options) {
        final Header[] headers = getHeaders(options);
        return CompletableFuture
                .completedFuture(new TransportResultHttpResponse(requestor.sendDeleteCommand(cmd.getUrl(), headers)));
    }

    private CompletableFuture<TransportResultHttpResponse> executePostJson(TransportPayloadScalarWebRequest request,
            TransportOption... options) {
        final String jsonRequest = gson.toJson(request.getPayload());

        final Header[] headers = getHeaders(options);
        return CompletableFuture.completedFuture(new TransportResultHttpResponse(
                requestor.sendPostJsonCommand(getBaseUrl().toExternalForm(), jsonRequest, headers)));
    }

    private CompletableFuture<TransportResultHttpResponse> executePostJson(TransportPayloadHttp request,
            TransportOption... options) {
        final String payload = request.getPayload();
        Objects.requireNonNull(payload, "payload cannot be null"); // may be empty however

        final Header[] headers = getHeaders(options);

        return CompletableFuture.completedFuture(new TransportResultHttpResponse(
                requestor.sendPostJsonCommand(request.getUrl(), payload, headers)));
    }

    private CompletableFuture<TransportResultHttpResponse> executePostXml(TransportPayloadHttp request,
            TransportOption... options) {
        Objects.requireNonNull(request, "request cannot be null");

        final String payload = request.getPayload();
        Objects.requireNonNull(payload, "payload cannot be null"); // may be empty however

        final Header[] headers = getHeaders(options);
        return CompletableFuture.completedFuture(new TransportResultHttpResponse(
                requestor.sendPostXmlCommand(request.getUrl(), payload, headers)));
    }

    @Override
    public String getProtocolType() {
        return SonyTransport.HTTP;
    }

    @Override
    public void close() {
        logger.debug("Closing http client");
        requestor.close();
    }
}
