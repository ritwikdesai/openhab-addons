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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.net.FilterOption;
import org.openhab.binding.sony.internal.scalarweb.gson.GsonUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebRequest;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.openhab.binding.sony.internal.scalarweb.models.api.ActRegisterId;
import org.openhab.binding.sony.internal.scalarweb.models.api.ActRegisterOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * This class represents authorization filter used to reauthorize our webscalar connection
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarAuthFilter implements ClientRequestFilter, ClientResponseFilter {

    /** The logger */
    private final Logger logger = LoggerFactory.getLogger(ScalarAuthFilter.class);

    /** The map of current cookies used for authentication */
    private final Map<String, NewCookie> cookies = new HashMap<>();

    /** The name of the authorization cookie */
    private static final String AUTHCOOKIENAME = "auth";

    /** The base URL of the access control service */
    private final String baseUrl;

    private static final String OPT_AUTOAUTH = "autoAuth";
    public static final FilterOption OPTION_AUTOAUTH = new FilterOption(OPT_AUTOAUTH, Boolean.TRUE);

    private final PropDelegate propDelegate;

    /**
     * Instantiates a new scalar auth filter from the device information
     *
     * @param baseUrl the non-null, non-empty base URL for the access control service
     */
    public ScalarAuthFilter(String baseUrl, PropDelegate propDelegate) {
        Validate.notEmpty(baseUrl, "baseUrl cannot be empty");
        Objects.requireNonNull(propDelegate, "propDelegate cannot be null");
        this.baseUrl = baseUrl;
        this.propDelegate = propDelegate;
    }

    @Override
    public void filter(@Nullable ClientRequestContext requestCtx, @Nullable ClientResponseContext responseCtx)
            throws IOException {
        Objects.requireNonNull(responseCtx, "responseCtx cannot be null");

        // The response may included an auth cookie that we need to save
        final Map<String, NewCookie> newCookies = responseCtx.getCookies();
        if (newCookies != null && newCookies.size() > 0) {
            if (newCookies.containsKey(AUTHCOOKIENAME)) {
                logger.debug("Auth cookie found and saved");
            }
            cookies.clear();
            cookies.putAll(newCookies);
        }
    }

    @Override
    public void filter(@Nullable ClientRequestContext requestCtx) throws IOException {
        Objects.requireNonNull(requestCtx, "requestCtx cannot be null");

        boolean authNeeded = true;

        // If we had a prior cookies, check to see if any expired
        if (cookies.size() > 0) {
            // Iterate the cookies
            for (final Iterator<Map.Entry<String, NewCookie>> it = cookies.entrySet().iterator(); it.hasNext();) {
                final Map.Entry<String, NewCookie> entry = it.next();

                final String cookieName = entry.getKey();
                final NewCookie cookie = entry.getValue();

                // Has the cookie expired...
                final Date expiryDate = cookie.getExpiry();
                if (expiryDate != null && new Date().after(expiryDate)) {
                    it.remove();
                } else {
                    if (AUTHCOOKIENAME.equalsIgnoreCase(cookieName)) {
                        authNeeded = false;
                    }
                }
            }
        }

        if (authNeeded && propDelegate.getProperty(OPT_AUTOAUTH) == Boolean.TRUE) {
            logger.debug("Trying to renew our authorization cookie");
            final Client client = ClientBuilder.newClient();
            // client.register(new LoggingFilter());

            final int idx = baseUrl.lastIndexOf("/");
            String deviceUrl = idx >= 0 ? baseUrl.substring(0, idx) : baseUrl;

            final String actControlUrl = deviceUrl + "/" + ScalarWebService.ACCESSCONTROL;

            final WebTarget target = client.target(actControlUrl);
            final Gson gson = GsonUtilities.getDefaultGson();

            final String json = gson.toJson(new ScalarWebRequest(1, ScalarWebMethod.ACTREGISTER, ScalarWebMethod.V1_0,
                    new ActRegisterId(), new Object[] { new ActRegisterOptions() }));
            final Response rsp = target.request().post(Entity.json(json));

            final Map<String, NewCookie> newCookies = rsp.getCookies();
            if (newCookies != null) {
                final NewCookie authCookie = newCookies.get(AUTHCOOKIENAME);
                if (authCookie != null) {
                    logger.debug("Authorization cookie was renewed");
                    cookies.put(AUTHCOOKIENAME, authCookie);
                } else {
                    logger.debug("No authorization cookie was returned");
                }
            } else {
                logger.debug("No authorization cookie was returned");
            }
        }

        requestCtx.getHeaders().put("Cookie", new ArrayList<Object>(cookies.values()));
    }

    public interface PropDelegate {
        public @Nullable Object getProperty(String key);
    }
}
