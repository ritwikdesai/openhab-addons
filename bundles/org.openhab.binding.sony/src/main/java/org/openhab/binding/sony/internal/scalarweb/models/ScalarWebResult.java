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

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.scalarweb.gson.ScalarWebResultDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

/**
 * This class represents a web scalar method result (to a request). This result will be created either by the
 * {@link ScalarWebResultDeserializer} when deserializing results or directly with an HttpResponse if an error occurred.
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarWebResult extends AbstractScalarResponse {

    /** The logger */
    private Logger logger = LoggerFactory.getLogger(ScalarWebResult.class);

    /** The unique request identifier */
    private final int id;

    /** The related httpResponse */
    private final HttpResponse httpResponse;

    /** The results of the request */
    private final JsonArray results;

    /** The any errors that occurred */
    private final JsonArray errors;

    /**
     * Instantiates a new scalar web result from the specified response
     *
     * @param response a non-null response
     */
    public ScalarWebResult(HttpResponse response) {
        Objects.requireNonNull(response, "response cannot be null");
        this.id = -1;
        this.httpResponse = response;
        this.results = new JsonArray();
        this.errors = new JsonArray();

        if (response.getHttpCode() != HttpStatus.OK_200) {
            this.errors.add(new JsonPrimitive(ScalarWebError.HTTPERROR));
            this.errors.add(new JsonPrimitive(response.getHttpReason()));
        }
    }

    /**
     * Instantiates a new scalar web result.
     *
     * @param id      the unique request id
     * @param results the results (might be null if errors)
     * @param errors  the errors (might be null if no errors - probably empty however)
     */
    public ScalarWebResult(int id, JsonArray results, JsonArray errors) {
        Objects.requireNonNull(results, "results cannot be null");
        Objects.requireNonNull(errors, "errors cannot be null");

        logger.debug(">>> result: {}, {}, {}", id, results, errors);
        this.id = id;
        this.results = results;
        this.errors = errors;

        if (errors.size() == 0) {
            this.httpResponse = new HttpResponse(HttpStatus.OK_200, "OK");
        } else {
            this.httpResponse = new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, getDeviceErrorDesc());
        }

    }

    /**
     * Helper method to create a successful empty result
     *
     * @return a non-null result
     */
    public static ScalarWebResult createEmptySuccess() {
        return new ScalarWebResult(-1, new JsonArray(), new JsonArray());
    }

    /**
     * Helper method to create a NOTIMPLEMENTED result
     *
     * @param methodName the non-null, non-empty method name not implemented
     * @return a non-null result
     */
    public static ScalarWebResult createNotImplemented(String methodName) {
        Validate.notEmpty(methodName, "methodName cannot be empty");
        final JsonArray ja = new JsonArray();
        ja.add(ScalarWebError.NOTIMPLEMENTED);
        ja.add(methodName + " is not implemented");
        return new ScalarWebResult(-1, new JsonArray(), ja);
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
     * Gets the results
     *
     * @return the results (possibly empty)
     */
    public JsonArray getResults() {
        return results;
    }

    /**
     * Checks if there are results
     *
     * @return true if results, false otherwise
     */
    public boolean hasResults() {
        return !isBlank(results);
    }

    /**
     * Checks if there are any errors
     *
     * @return true if there are errors, false otherwise
     */
    public boolean isError() {
        final JsonArray localErrors = errors;
        return !isBlank(localErrors);
    }

    /**
     * Gets the HTTP response for this request.
     *
     * @return the non-null http response
     */
    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    /**
     * Returns the device error code related to this request (will be NONE if no errors)
     *
     * @return the device error code
     */
    public int getDeviceErrorCode() {
        final JsonArray localErrors = errors;
        if (isBlank(localErrors)) {
            return ScalarWebError.NONE;
        } else {
            final String rcStr = localErrors.get(0).getAsString();
            try {
                return Integer.parseInt(rcStr);
            } catch (NumberFormatException e) {
                return ScalarWebError.UNKNOWN;
            }
        }
    }

    /**
     * Returns the device error description
     *
     * @return a non-null, possibly empty (if no errors) error description
     */
    public String getDeviceErrorDesc() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(errors.get(i).getAsString());
        }
        return sb.toString();
    }

    @Override
    public <T> T as(Class<T> clazz) throws IOException {
        if (isError()) {
            throw getHttpResponse().createException();
        }

        return super.as(clazz);
    }

    @Override
    public <T> List<T> asArray(Class<T> clazz) throws IOException {
        if (isError()) {
            throw getHttpResponse().createException();
        }

        return super.asArray(clazz);
    }

    @Override
    protected JsonArray getPayload() {
        return results;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("id: ");
        sb.append(id);

        final JsonArray localResults = results;
        final JsonArray localErrors = errors;

        if (localErrors.size() > 0) {
            sb.append(", Error: ");
            sb.append(localErrors.toString());
        } else {
            sb.append(", Results: ");
            sb.append(localResults.toString());
        }

        return sb.toString();
    }
}
