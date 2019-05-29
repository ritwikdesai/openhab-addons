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
package org.openhab.binding.sony.internal.upnp.models;

import java.net.URL;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.net.NetUtil;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * This class represents the deserialized results of an UPNP service. The following is an example of the
 * results that will be deserialized:
 *
 * <pre>
 * {@code
     need example
 * }
 * </pre>
 *
 * @author Tim Roberts - Initial Contribution
 *
 */
@NonNullByDefault
@XStreamAlias("service")
public class UpnpService {

    /** The service identifier */
    @XStreamAlias("serviceId")
    private @Nullable String serviceId;

    /** The service type */
    @XStreamAlias("serviceType")
    private @Nullable String serviceType;

    /** The scpd url */
    @XStreamAlias("SCPDURL")
    private @Nullable String scpdUrl;

    /** The control url */
    @XStreamAlias("controlURL")
    private @Nullable String controlUrl;

    /**
     * Gets the SCPD URL given the base URL
     * 
     * @param baseUrl the non-null base url to use as a reference
     *
     * @return the control url
     */
    public @Nullable URL getScpdUrl(URL baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");

        final String localScpdUrl = scpdUrl;
        return localScpdUrl == null || StringUtils.isEmpty(localScpdUrl) ? null : NetUtil.getUrl(baseUrl, localScpdUrl);
    }

    /**
     * Gets the control url
     * 
     * @param baseUrl the non-null base url to use as a reference
     * @return the control url
     */
    public @Nullable URL getControlUrl(URL baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");

        final String localControlUrl = controlUrl;
        return localControlUrl == null || StringUtils.isEmpty(localControlUrl) ? null
                : NetUtil.getUrl(baseUrl, localControlUrl);
    }

    /**
     * Gets the service type
     *
     * @return the service type
     */
    public @Nullable String getServiceType() {
        return serviceType;
    }

    /**
     * Gets the service id
     *
     * @return the service id
     */
    public @Nullable String getServiceId() {
        return serviceId;
    }
}