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
package org.openhab.binding.sony.internal.dial;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.sony.internal.dial.models.DialClient;
import org.openhab.binding.sony.internal.dial.models.DialDeviceInfo;
import org.openhab.binding.sony.internal.dial.models.DialRoot;
import org.openhab.binding.sony.internal.dial.models.DialXmlReader;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.transports.SonyTransport;
import org.openhab.binding.sony.internal.transports.SonyTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a factory for creating {@link DialClient}
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class DialClientFactory {
    /** The logger */
    private Logger logger = LoggerFactory.getLogger(DialClientFactory.class);

    /**
     * Attempts to retrieve the {@link DialClient} from the specified URL. Null will be returned if the URL contained an
     * invalid representation
     *
     * @param transport a non-null transport to use
     * @param dialUrl a non-null, non-empty URL to find
     * @return the {@link DialClient} if found, null otherwise
     * @throws IOException if an IO exception occurs getting the client
     */
    public @Nullable DialClient get(final String dialUrl) throws IOException {
        Validate.notEmpty(dialUrl, "dialUrl cannot be empty");

        try {
            final URL dialURL = new URL(dialUrl);
            if (StringUtils.isEmpty(dialURL.getPath())) {
                return createDefaultClient(dialUrl);
            } else {
                return queryDialClient(dialUrl);
            }
        } catch (URISyntaxException e) {
            logger.debug("Malformed DIAL URL: {}", dialUrl, e);
            return null;
        }
    }


    /**
     * Private method to create a default dial client (assumes the URLs are standard)
     * @param dialUrl a non-null, non-emtpy dial URL
     * @return a non-null default {@link DialClient}
     * @throws MalformedURLException if the URL was malformed
     */
    private DialClient createDefaultClient(final String dialUrl) throws MalformedURLException {
        Validate.notEmpty(dialUrl, "dialUrl cannot be empty");

        logger.debug("Creating default DIAL client for {}", dialUrl);
        final String appUrl = dialUrl + "/DIAL/apps/";
        final DialDeviceInfo ddi = new DialDeviceInfo(dialUrl + "/DIAL/sony/applist", null, null);
        return new DialClient(new URL(appUrl), Collections.singletonList(ddi));
    }

    /**
     * Private method to create a dial client by querying it's parameters
     * @param dialUrl a non-null, non-emtpy dial URL
     * @return a possibly null (if no content can be found) {@link DialClient}
     * @throws URISyntaxException if a URI exception occurred
     * @throws IOException if an IO exception occurred
     */
    private @Nullable DialClient queryDialClient(final String dialUrl) throws URISyntaxException, IOException {
        Validate.notEmpty(dialUrl, "dialUrl cannot be empty");

        logger.debug("Querying DIAL client: {}", dialUrl);
        try (SonyTransport transport = SonyTransportFactory.createHttpTransport(dialUrl)) {
            final HttpResponse resp = transport.executeGet(dialUrl);
            if (resp.getHttpCode() != HttpStatus.OK_200) {
                throw resp.createException();
            }

            final String content = resp.getContent();
            final DialRoot root = DialXmlReader.ROOT.fromXML(content);
            if (root == null) {
                logger.debug("No content found from {}: {}", dialUrl, content);
                return null;
            }

            final String appUrl = resp.getResponseHeader("Application-URL");
            logger.debug("Creating DIAL client: {} - {}", appUrl, content);
            return new DialClient(new URL(appUrl), root.getDevices());
        }
    }
}
