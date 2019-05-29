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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.sony.internal.net.FilterOption;
import org.openhab.binding.sony.internal.net.Header;
import org.openhab.binding.sony.internal.net.HttpRequest;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.net.NetUtil;
import org.openhab.binding.sony.internal.scalarweb.ScalarUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebRequest;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SonyHttpTransport extends AbstractSonyTransport<ScalarWebRequest> {
    /** The logger */
    protected Logger logger = LoggerFactory.getLogger(SonyHttpTransport.class);

    /** The HTTP request object to use */
    private final HttpRequest requestor;

    /** The base URL to contact */
    private final String baseUrl;

    /** GSON used to serialize/deserialize objects */
    private final Gson gson;

    /**
     *
     * @param baseUrl
     * @param gson
     * @param filters
     */
    public SonyHttpTransport(String baseUrl, Gson gson) {
        Validate.notEmpty(baseUrl, "baseUrl cannot be empty");
        Objects.requireNonNull(gson, "gson cannot be null");

        this.requestor = NetUtil.createHttpRequest();
        this.requestor.register(new ScalarAuthFilter(baseUrl, p -> {
            for (FilterOption fo : getOption(SonyTransport.OPTION_FILTER, FilterOption.class)) {
                if (StringUtils.equalsIgnoreCase(p, fo.getKey())) {
                    return fo.getValue();
                }
            }

            return null;
        }));

        this.baseUrl = baseUrl;
        this.gson = gson;
    }

    @Override
    public Future<ScalarWebResult> executeRaw(String cmd) {
        final List<Header> headers = getOption(OPTION_HEADER, Header.class);

        final HttpResponse resp = requestor.sendPostJsonCommand(baseUrl, cmd,
                headers.toArray(new Header[headers.size()]));

        // logger.debug(">>> sending: {} to {}", jsonRequest, baseUrl);

        if (resp.getHttpCode() == HttpStatus.OK_200) {
            // logger.debug(">>> contents: {}", resp.getContent());
            final ScalarWebResult result = gson.fromJson(resp.getContent(), ScalarWebResult.class);
            return CompletableFuture.completedFuture(result);
        } else {
            return CompletableFuture
                    .completedFuture(ScalarUtilities.createErrorResult(resp.getHttpCode(), resp.getContent()));
        }
    }

    @Override
    public Future<ScalarWebResult> execute(ScalarWebRequest cmd) {
        final String jsonRequest = gson.toJson(cmd);
        return executeRaw(jsonRequest);
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
