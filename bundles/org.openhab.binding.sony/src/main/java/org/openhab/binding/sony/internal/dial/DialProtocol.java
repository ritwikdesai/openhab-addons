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
import java.net.URL;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.dial.models.DialApp;
import org.openhab.binding.sony.internal.dial.models.DialAppState;
import org.openhab.binding.sony.internal.dial.models.DialClient;
import org.openhab.binding.sony.internal.net.HttpRequest;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.net.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the protocol handler for the DIAL System. This handler will issue the protocol commands and will
 * process the responses from the DIAL system.
 *
 * @author Tim Roberts - Initial contribution
 * @param <T> the generic type for the callback
 */
@NonNullByDefault
class DialProtocol<T extends ThingCallback<String>> implements AutoCloseable {

    /** The logger */
    private Logger logger = LoggerFactory.getLogger(DialProtocol.class);

    /** The DIAL device full address */
    private final String deviceUrl;

    /** The {@link ThingCallback} that we can callback to set state and status */
    private final T callback;

    /** The {@link HttpRequest} used to make http requests */
    private final HttpRequest httpRequest;

    /** The {@link DialClient} representing the DIAL application */
    private final DialClient dialClient;

    /**
     * Constructs the protocol handler from the configuration and callback
     *
     * @param config   a non-null {@link DialConfig} (may be connected or disconnected)
     * @param callback a non-null {@link ThingCallback} to callback
     * @throws IOException if an ioexception is thrown
     */
    DialProtocol(DialConfig config, T callback) throws IOException {
        Objects.requireNonNull(config, "config cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");

        // Confirm the address is a valid URL
        final String deviceAddress = config.getDeviceAddress();
        final URL deviceURL = new URL(deviceAddress);

        this.deviceUrl = deviceURL.toExternalForm();

        this.callback = callback;

        httpRequest = NetUtil.createHttpRequest();

        final String deviceMacAddress = config.getDeviceMacAddress();
        if (deviceMacAddress != null && StringUtils.isNotBlank(deviceMacAddress)) {
            NetUtil.sendWol(deviceURL.getHost(), deviceMacAddress);
        }

        final DialClient dialClient = DialClient.get(this.deviceUrl, false);
        if (dialClient == null) {
            throw new IOException("DialState could not be retrieved from " + deviceAddress);
        }
        this.dialClient = dialClient;
    }

    /**
     * Returns the callback used by this protocol
     *
     * @return the non-null callback used by this protocol
     */
    T getCallback() {
        return callback;
    }

    /**
     * Sets the 'state' channel for a specific application id. on to start the app, off to turn it off (off generally
     * isn't supported by SONY devices - but we try as per the protocol anyway)
     *
     * @param channelId the non-null, non-empty channel id
     * @param applId    the non-null, non-empty application id
     * @param start     true to start, false otherwise
     */
    public void setState(String channelId, String applId, boolean start) {
        Validate.notEmpty(channelId, "channelId cannot be empty");
        Validate.notEmpty(applId, "applId cannot be empty");

        final URL urr = NetUtil.getUrl(dialClient.getAppUrl(), applId);
        if (urr == null) {
            logger.debug("Could not combine {} and {}", dialClient.getAppUrl(), applId);
        } else {
            final HttpResponse resp = start ? httpRequest.sendPostXmlCommand(urr.toString(), "")
                    : httpRequest.sendDeleteCommand(urr.toString(), "");
            if (resp.getHttpCode() != HttpStatus.SERVICE_UNAVAILABLE_503) {
                logger.debug("Cannot start {}, another application is currently running.", applId);
            } else if (resp.getHttpCode() != HttpStatus.CREATED_201) {
                logger.debug("Error setting the 'state' of the application: {}", resp.getHttpCode());
            }
        }
    }

    /**
     * Refresh state of a specific DIAL application
     *
     * @param channelId the non-null non-empty channel ID
     * @param applId    the non-null, non-empty application ID
     */
    public void refreshState(String channelId, String applId) {
        Validate.notEmpty(channelId, "channelId cannot be empty");
        Validate.notEmpty(applId, "applId cannot be empty");

        try {
            final URL urr = NetUtil.getUrl(dialClient.getAppUrl(), applId);
            if (urr == null) {
                logger.debug("Could not combine {} and {}", dialClient.getAppUrl(), applId);
            } else {
                final DialAppState state = DialAppState.get(urr);
                callback.stateChanged(channelId, state != null && state.isRunning() ? OnOffType.ON : OnOffType.OFF);
            }
        } catch (IOException e) {
            logger.debug("Error refreshing the 'state' of the application: {}", e.getMessage());
        }
    }

    /**
     * Refresh the name of the application
     *
     * @param channelId the non-null non-empty channel ID
     * @param applId    the non-null application id
     */
    public void refreshName(String channelId, String applId) {
        Validate.notEmpty(channelId, "channelId cannot be empty");
        Validate.notEmpty(applId, "applId cannot be empty");

        final DialApp app = dialClient.getDialApp(applId);
        callback.stateChanged(channelId, new StringType(app == null ? "(Unknown)" : app.getName()));
    }

    /**
     * Refresh the icon for the application
     *
     * @param channelId the non-null non-empty channel ID
     * @param applId    the non-null application id
     */
    public void refreshIcon(String channelId, String applId) {
        Validate.notEmpty(channelId, "channelId cannot be empty");
        Validate.notEmpty(applId, "applId cannot be empty");

        final DialApp app = dialClient.getDialApp(applId);
        final String url = app == null ? null : app.getIconUrl();

        byte[] iconData = null;
        if (url != null && StringUtils.isNotEmpty(url)) {
            final HttpResponse resp = httpRequest.sendGetCommand(url);
            if (resp.getHttpCode() == HttpStatus.OK_200) {
                iconData = resp.getContentAsBytes();
            }
        }

        if (iconData == null) {
            callback.stateChanged(channelId, UnDefType.NULL);
        } else {
            callback.stateChanged(channelId, new RawType(iconData, RawType.DEFAULT_MIME_TYPE));
        }
    }

    /**
     * Returns the list of dial apps on the sony device
     *
     * @return a non-null, maybe empty list of dial apps
     */
    public List<DialApp> getDialApps() {
        return dialClient.getDialApps();
    }

    @Override
    public void close() {
        httpRequest.close();
    }
}
