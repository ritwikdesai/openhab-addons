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
package org.openhab.binding.sony.internal.scalarweb.service;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.io.net.http.WebSocketFactory;
import org.openhab.binding.sony.internal.SonyUtil;
import org.openhab.binding.sony.internal.scalarweb.gson.GsonUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebRequest;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.openhab.binding.sony.internal.transports.SonyTransport;
import org.openhab.binding.sony.internal.transports.SonyTransportFactory;
import org.openhab.binding.sony.internal.transports.TransportOptionAutoAuth;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

/**
 * The implementation of the protocol handles the Audio service
 *
 * @author Tim Roberts - Initial contribution
 * @param <T> the generic type for the callback
 */
@NonNullByDefault
@Component()
public class SonyServlet extends HttpServlet {

    private static final long serialVersionUID = -8873654812522111922L;

    /** The logger. */
    private Logger logger = LoggerFactory.getLogger(SonyServlet.class);

    private static final String SONY_PATH = "/sony";
    private static final String SONYAPP_PATH = "/sony/app";

    private @NonNullByDefault({}) HttpService httpService;

    /** The websocket client to use */
    private @NonNullByDefault({}) WebSocketClient webSocketClient;

    private final Gson gson = GsonUtilities.getApiGson();

    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool("sony");

    @Reference
    protected void setWebSocketFactory(WebSocketFactory webSocketFactory) {
        this.webSocketClient = webSocketFactory.getCommonWebSocketClient();
    }

    protected void unsetWebSocketFactory(WebSocketFactory webSocketFactory) {
        this.webSocketClient = null;
    }

    @Reference
    public void setHttpService(HttpService httpService) {
        Objects.requireNonNull(httpService, "httpService cannot be null");
        this.httpService = httpService;
    }

    public void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    @Activate
    public void activate() {
        final HttpService localHttpService = httpService;
        if (localHttpService == null) {
            return;
        }
        try {
            localHttpService.registerServlet(SONYAPP_PATH, this, new Hashtable<>(),
                    localHttpService.createDefaultHttpContext());
            localHttpService.registerResources(SONY_PATH, "web/sonyapp/dist", localHttpService.createDefaultHttpContext());
            logger.debug("Started Sony Web service at {}", SONY_PATH);
        } catch (ServletException | NamespaceException e) {
            logger.debug("Exception starting status servlet: {}", e.getMessage(), e);
        }

    }

    @Deactivate
    public void deactivate() {
    }

    @Override
    protected void doPost(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp)
            throws ServletException, IOException {
        Objects.requireNonNull(req, "req cannot be null");
        Objects.requireNonNull(resp, "resp cannot be null");
        final CommandRequest cmdRqst = gson.fromJson(req.getReader(), CommandRequest.class);

        final String baseUrl = cmdRqst.getBaseUrl();
        if (baseUrl == null || StringUtils.isEmpty(baseUrl)) {
            SonyUtil.write(resp, gson.toJson(new CommandResponse(false, "baseUrl is required")));
            return;
        }

        final String serviceName = cmdRqst.getServiceName();
        if (serviceName == null || StringUtils.isEmpty(serviceName)) {
            SonyUtil.write(resp, gson.toJson(new CommandResponse(false, "serviceName is required")));
            return;
        }

        final String transportName = cmdRqst.getTransport();
        if (transportName == null || StringUtils.isEmpty(transportName)) {
            SonyUtil.write(resp, gson.toJson(new CommandResponse(false, "transport is required")));
            return;
        }

        final String command = cmdRqst.getCommand();
        if (command == null || StringUtils.isEmpty(command)) {
            SonyUtil.write(resp, gson.toJson(new CommandResponse(false, "command is required")));
            return;
        }

        final String version = cmdRqst.getVersion();
        if (version == null || StringUtils.isEmpty(version)) {
            SonyUtil.write(resp, gson.toJson(new CommandResponse(false, "version is required")));
            return;
        }

        final String parms = cmdRqst.getParms();
        // parms can be empty
        if (parms == null) {
            SonyUtil.write(resp, gson.toJson(new CommandResponse(false, "parms is required")));
            return;
        }

        final SonyTransportFactory factory = new SonyTransportFactory(new URL(baseUrl), gson, webSocketClient,
                scheduler);

        try (final SonyTransport transport = factory.getSonyTransport(serviceName, transportName)) {
            if (transport == null) {
                SonyUtil.write(resp, gson.toJson(new CommandResponse(false, "No transport of type: " + transportName)));
                return;
            } else {
                final String cmd = "{\"id\":100,\"method\":\"" + command + "\",\"version\":\"" + version
                        + "\",\"params\":[" + parms + "]}";

                final ScalarWebRequest rqst = gson.fromJson(cmd, ScalarWebRequest.class);
                        
                final ScalarWebResult result = transport.execute(rqst, TransportOptionAutoAuth.TRUE);
                if (result.isError()) {
                    SonyUtil.write(resp, gson.toJson(new CommandResponse(false, result.getDeviceErrorDesc())));
                } else {
                    final JsonArray ja = result.getResults();
                    final String resString = gson.toJson(ja);
                    SonyUtil.write(resp, gson.toJson(new CommandResponse(resString)));
                }
            }
        }
    }
}
