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
package org.openhab.binding.sony.internal.dial.models;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.sony.internal.net.HttpRequest;
import org.openhab.binding.sony.internal.net.HttpResponse;

import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * This class represents the state of a DIAL device. The DIAL state will include all the devices specified and the URL
 * to access the device
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class DialClient {
    /** The list of {@link DialDeviceInfo} */
    @XStreamImplicit
    private final List<DialDeviceInfo> deviceInfos;

    /** The url to get application state */
    private final URL appUrl;

    /**
     * Constructs the instance from the specified URL and list of {@link DialDeviceInfo}
     *
     * @param appUrl a non-null application URL
     * @param infos  a non-null, possibly emply list of {@link DialDeviceInfo}
     */
    private DialClient(URL appUrl, List<DialDeviceInfo> infos) {
        Objects.requireNonNull(appUrl, "appUrl cannot be null");
        Objects.requireNonNull(infos, "infos cannot be null");

        this.appUrl = appUrl;
        deviceInfos = infos;
    }

    /**
     * Attempts to retrieve the {@link DialClient} from the specified URL. Null will be returned if the URL contained an
     * invalid representation
     *
     * @param dialUrl              a non-null, non-empty URL to find
     * @param ignoreAuthentication true to ignore authentication exceptions, false otherwise
     * @return the {@link DialClient} if found, null otherwise
     * @throws IOException if an IO exception occurs getting the client
     */
    public @Nullable static DialClient get(String dialUrl, boolean ignoreAuthentication) throws IOException {
        Validate.notEmpty(dialUrl, "dialUrl cannot be empty");

        try (final HttpRequest requestor = new HttpRequest()) {
            final HttpResponse resp = requestor.sendGetCommand(dialUrl);
            if (resp.getHttpCode() != HttpStatus.OK_200) {
                throw resp.createException();
            }

            final String content = resp.getContent();
            final DialRoot root = DialXmlReader.ROOT.fromXML(content);
            if (root == null) {
                return null;
            }

            final List<DialDeviceInfo> newInfos = new ArrayList<>();

            for (final DialDeviceInfo info : root.getDevices()) {
                final String appsListUrl = info.getAppsListUrl();
                if (appsListUrl != null && StringUtils.isNotBlank(appsListUrl)) {
                    final HttpResponse appsResp = requestor.sendGetCommand(appsListUrl);
                    if (appsResp.getHttpCode() == HttpStatus.OK_200) {
                        final DialService service = DialXmlReader.SERVICE.fromXML(appsResp.getContent());
                        if (service != null) {
                            newInfos.add(info.withApps(service.getApps()));
                        }
                    } else {
                        if (ignoreAuthentication && appsResp.getHttpCode() == 403) {
                            newInfos.add(info);
                        } else {
                            throw appsResp.createException();
                        }
                    }
                }
            }

            final String appUrl = resp.getResponseHeader("Application-URL");
            return new DialClient(new URL(appUrl), newInfos);
        }
    }

    /**
     * Returns the device application URL
     *
     * @return the non-null device application URL
     */
    public URL getAppUrl() {
        return appUrl;
    }

    /**
     * Checks to see if the state has any services
     *
     * @return true, if successful, false otherwise
     */
    public boolean hasDialService() {
        return deviceInfos.size() > 0;
    }

    /**
     * Returns the first device ID or null if there are no devices
     *
     * @return the first device ID or null
     */
    public @Nullable String getFirstDeviceId() {
        return deviceInfos.stream().map(e -> e.getDeviceId()).filter(e -> StringUtils.isNotEmpty(e)).findFirst()
                .orElse(null);
    }

    /**
     * Returns the collection of {@link DialApp} for this device
     *
     * @return a non-null, possibly empty list of {@link DialApp}
     */
    public List<DialApp> getDialApps() {
        final List<DialApp> apps = new ArrayList<>();

        for (DialDeviceInfo ddi : deviceInfos) {
            final List<DialApp> dialApps = ddi.getDialApps();
            for (DialApp app : dialApps) {
                apps.add(app);
            }
        }
        return apps;
    }

    /**
     * Gets the dial app related to the application ID.
     *
     * @param applId a non-null non-empty application id
     *
     * @return the dial application or null if not found
     */
    public @Nullable DialApp getDialApp(String applId) {
        Validate.notEmpty(applId, "applId cannot be empty");

        for (DialDeviceInfo ddi : deviceInfos) {
            final List<DialApp> dialApps = ddi.getDialApps();
            for (DialApp app : dialApps) {
                if (StringUtils.equalsIgnoreCase(applId, app.getId())) {
                    return app;
                }
            }
        }
        return null;
    }
}
