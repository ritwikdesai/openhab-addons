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
package org.openhab.binding.sony.internal.scalarweb.models.api;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.scalarweb.transports.SonyTransport;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ServiceProtocol {

    private final String serviceName;
    private final Set<String> protocols;

    public ServiceProtocol(String serviceName, Set<String> protocols) {
        Validate.notEmpty(serviceName, "serviceName cannot be empty");
        Objects.requireNonNull(protocols, "protocols cannot be null");

        this.serviceName = serviceName;
        this.protocols = protocols;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Set<String> getProtocols() {
        return protocols;
    }

    public boolean hasWebsocketProtocol() {
        return protocols.contains(SonyTransport.WEBSOCKET);
    }

    public boolean hasHttpProtocol() {
        return protocols.contains(SonyTransport.HTTP);
    }

    @Override
    public int hashCode() {
        return serviceName.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ServiceProtocol other = (ServiceProtocol) obj;
        return StringUtils.equalsIgnoreCase(serviceName, other.serviceName);
    }

    @Override
    public String toString() {
        return "ServiceProtocol [serviceName=" + serviceName + ", protocols=" + protocols + "]";
    }
}