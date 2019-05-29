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
package org.openhab.binding.sony.internal.scalarweb;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.sony.internal.net.HttpRequest;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.net.NetUtil;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class represents a web scalar method definition that can be called
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarWebClient implements AutoCloseable {

    /** The logger */
    private Logger logger = LoggerFactory.getLogger(ScalarWebClient.class);

    /** The Constant for the sony upnp identifier */
    public static final String SONY_AV_NS = "urn:schemas-sony-com:av";

    /** The device manager */
    private final ScalarWebDeviceManager deviceManager;

    /**
     * Instantiates a new scalar web state.
     *
     * @param scalarWebUrl the non-null, non-empty scalar web url
     * @param context      the non-null data context to use
     * @throws IOException                  Signals that an I/O exception has occurred.
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException                 the SAX exception
     */
    public ScalarWebClient(String scalarWebUrl, ScalarWebContext context)
            throws IOException, ParserConfigurationException, SAXException {
        Validate.notEmpty(scalarWebUrl, "scalarWebUrl cannot be empty");
        Objects.requireNonNull(context, "context cannot be null");

        try (HttpRequest requestor = NetUtil.createHttpRequest()) {
            final HttpResponse resp = requestor.sendGetCommand(scalarWebUrl);

            if (resp.getHttpCode() == HttpStatus.OK_200) {
                final Document scalarWebDocument = resp.getContentAsXml();

                final NodeList deviceInfos = scalarWebDocument.getElementsByTagNameNS(SONY_AV_NS,
                        "X_ScalarWebAPI_DeviceInfo");
                if (deviceInfos.getLength() > 1) {
                    logger.debug("More than one X_ScalarWebAPI_DeviceInfo found - using the first valid one");
                }

                // Use the first valid one
                ScalarWebDeviceManager myDevice = null;
                for (int i = deviceInfos.getLength() - 1; i >= 0; i--) {
                    final Node deviceInfo = deviceInfos.item(0);

                    try {
                        myDevice = ScalarWebDeviceManager.create(deviceInfo, context);
                        break;
                    } catch (IOException | DOMException e) {
                        logger.debug("Exception getting creating scalarwebapi device for {}[{}]: {}",
                                deviceInfo.getNodeName(), i, e.getMessage(), e);
                    }
                }
                if (myDevice == null) {
                    throw new IOException("No valid scalar web devices found");
                }

                deviceManager = myDevice;
            } else {
                // If can't connect - try to connect to the likely websocket server directly using
                // the host name and default path
                final URL scalarUrl = new URL(scalarWebUrl);
                final URL baseUrl = new URL("http://" + scalarUrl.getHost() + ":10000/sony");
                deviceManager = new ScalarWebDeviceManager(baseUrl, context);
            }
        }

    }

    /**
     * Gets the device manager
     *
     * @return the non-null device manager
     */
    public ScalarWebDeviceManager getDevice() {
        return deviceManager;
    }

    /**
     * Gets the service for the specified name
     *
     * @param serviceName the service name
     * @return the service or null if not found
     */
    public @Nullable ScalarWebService getService(String serviceName) {
        return deviceManager.getService(serviceName);
    }

    @Override
    public String toString() {
        return deviceManager.toString();
    }

    @Override
    public void close() {
        deviceManager.close();
    }
}
