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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.scalarweb.gson.GsonUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebRequest;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.openhab.binding.sony.internal.transports.SonyHttpTransport;
import org.openhab.binding.sony.internal.transports.SonyTransport;
import org.openhab.binding.sony.internal.transports.SonyTransportFactory;
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
public class ScalarWebClientFactory {

    /** The logger */
    private Logger logger = LoggerFactory.getLogger(ScalarWebClientFactory.class);

    private static final String LIKELY_PATH = "/sony";
    private static final int LIKELY_PORT = -1;

    /** Default audio port for soundbars/receiver (websocket) */
    private static final int HOME_AUDIO_PORT = 10000;
    
    /** Default audio port for wireless speakers (websocket) */
    private static final int PERSONAL_AUDIO_PORT = 54480;

    /** Websocket guide path */
    private static final String LIKELY_GUIDE_PATH = LIKELY_PATH + "/guide";

    public ScalarWebClient get(String scalarWebUrl, ScalarWebContext context) throws IOException, ParserConfigurationException, SAXException, URISyntaxException {
        Validate.notEmpty(scalarWebUrl, "scalarWebUrl cannot be empty");
        Objects.requireNonNull(context, "context cannot be null");

        return get(new URL(scalarWebUrl), context);
    }
    
    public ScalarWebClient get(URL scalarWebUrl, ScalarWebContext context)
            throws IOException, ParserConfigurationException, SAXException, URISyntaxException {
        Objects.requireNonNull(scalarWebUrl, "scalarWebUrl cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        if (StringUtils.isEmpty(scalarWebUrl.getPath())) {
            return getDefaultClient(scalarWebUrl, context);
        } else {
            return queryScalarWebSclient(scalarWebUrl, context);
        }
    }
    
    private ScalarWebClient getDefaultClient(URL scalarWebUrl, ScalarWebContext context) throws DOMException, IOException, URISyntaxException {
        Objects.requireNonNull(scalarWebUrl, "scalarWebUrl cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        final URL homeAudioUrl = new URL(scalarWebUrl.getProtocol(), scalarWebUrl.getHost(), HOME_AUDIO_PORT, LIKELY_GUIDE_PATH);
        logger.debug("Testing Default Scalar Web client to see if it's a home audio device (AVR/Soundbar): {}", homeAudioUrl);
        try (SonyTransport transport = new SonyHttpTransport(homeAudioUrl.toExternalForm(),
                GsonUtilities.getApiGson())) {
            final ScalarWebResult homeAudioRes = transport
                    .execute(new ScalarWebRequest(1, ScalarWebMethod.GETVERSIONS, ScalarWebMethod.V1_0));
            if (homeAudioRes.getHttpResponse().getHttpCode() == HttpStatus.OK_200) {
                final URL baseUrl = new URL(scalarWebUrl.getProtocol(), scalarWebUrl.getHost(), HOME_AUDIO_PORT, LIKELY_PATH);
                return new ScalarWebClient(new ScalarWebDeviceManager(baseUrl, context));
            }
        }

        final URL personalAudioUrl = new URL(scalarWebUrl.getProtocol(), scalarWebUrl.getHost(), PERSONAL_AUDIO_PORT, LIKELY_GUIDE_PATH);
        logger.debug("Testing Default Scalar Web client to see if it's a personal audio device (Wireless speaker): {}", homeAudioUrl);
        try (SonyTransport transport = new SonyHttpTransport(personalAudioUrl.toExternalForm(),
        GsonUtilities.getApiGson())) {
            final ScalarWebResult personalAudioRes = transport
                    .execute(new ScalarWebRequest(1, ScalarWebMethod.GETVERSIONS, ScalarWebMethod.V1_0));
            if (personalAudioRes.getHttpResponse().getHttpCode() == HttpStatus.OK_200) {
                final URL baseUrl = new URL(scalarWebUrl.getProtocol(), scalarWebUrl.getHost(), PERSONAL_AUDIO_PORT, LIKELY_PATH);
                return new ScalarWebClient(new ScalarWebDeviceManager(baseUrl, context));
            }
        }


        final URL baseUrl = new URL(scalarWebUrl.getProtocol(), scalarWebUrl.getHost(), LIKELY_PORT, LIKELY_PATH);
        return new ScalarWebClient(new ScalarWebDeviceManager(baseUrl, context));
    }
    
    /**
     * Instantiates a new scalar web state.
     *
     * @param scalarWebUrl the non-null, scalar web url
     * @param context the non-null data context to use
     * @throws URISyntaxException
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException the SAX exception
     */
    public ScalarWebClient queryScalarWebSclient(URL scalarWebUrl, ScalarWebContext context) throws URISyntaxException, ParserConfigurationException, SAXException, IOException {
        Objects.requireNonNull(scalarWebUrl, "scalarWebUrl cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        try (SonyTransport transport = SonyTransportFactory.createHttpTransport(scalarWebUrl.toExternalForm())) {
            final HttpResponse resp = transport.executeGet(scalarWebUrl.toExternalForm());

            if (resp.getHttpCode() == HttpStatus.OK_200) {
                final Document scalarWebDocument = resp.getContentAsXml();

                final NodeList deviceInfos = scalarWebDocument.getElementsByTagNameNS(ScalarWebDeviceManager.SONY_AV_NS,
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

                return new ScalarWebClient(myDevice);
            } else {
                // If can't connect - try to connect to the likely websocket server directly using
                // the host name and default path
                return getDefaultClient(scalarWebUrl, context);
            }
        }
    }
}
