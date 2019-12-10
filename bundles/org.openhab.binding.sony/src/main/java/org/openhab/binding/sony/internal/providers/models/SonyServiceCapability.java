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
package org.openhab.binding.sony.internal.providers.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.SonyMatcher;
import org.openhab.binding.sony.internal.SonyMatcherUtils;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;

/**
 * The class represents a sony device service capability. The capability describes the service and the
 * methods/notifications for the service. The class will only be used to serialize the definition.
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SonyServiceCapability implements SonyMatcher {
    /** The service name */
    private final String serviceName;

    /** The service version */
    private final String version;

    /** The transport used for this service */
    private final String transport;

    /** The methods defined for the service */
    private final List<ScalarWebMethod> methods;

    /** The notifications that can be sent from the service */
    private final List<ScalarWebMethod> notifications;

    static final Comparator<SonyMatcher> COMPARATOR = Comparator
            .comparing((SonyMatcher e) -> ((SonyServiceCapability) e).getServiceName())
            .thenComparing(e -> ((SonyServiceCapability) e).getVersion());

    /**
     * Constructs the capability from the parameters
     *
     * @param serviceName   a non-null, non-empty service name
     * @param version       a non-null, non-empty service version
     * @param transport     a non-null, non-empty transport
     * @param methods       a non-null, possibly empty list of methods
     * @param notifications a non-null, possibly empty list of notifications
     */
    public SonyServiceCapability(String serviceName, String version, String transport, List<ScalarWebMethod> methods,
            List<ScalarWebMethod> notifications) {
        Validate.notEmpty(serviceName, "serviceName cannot be empty");
        Validate.notEmpty(version, "version cannot be empty");
        Validate.notEmpty(transport, "transport cannot be empty");
        Objects.requireNonNull(methods, "methods cannot be null");
        Objects.requireNonNull(notifications, "notifications cannot be null");

        this.serviceName = serviceName;
        this.version = version;
        this.transport = transport;
        this.methods = new ArrayList<>(methods);
        this.notifications = new ArrayList<>(notifications);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getVersion() {
        return version;
    }

    public String getTransport() {
        return transport;
    }

    public List<ScalarWebMethod> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    public List<ScalarWebMethod> getNotifications() {
        return Collections.unmodifiableList(notifications);
    }

    @Override
    public boolean matches(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof SonyServiceCapability)) {
            return false;
        }

        final SonyServiceCapability other = (SonyServiceCapability) obj;

        return StringUtils.equalsIgnoreCase(serviceName, other.serviceName)
                && StringUtils.equalsIgnoreCase(version, other.version)
                && StringUtils.equalsIgnoreCase(transport, other.transport)
                && SonyMatcherUtils.matches(methods, other.methods, ScalarWebMethod.COMPARATOR)
                && SonyMatcherUtils.matches(notifications, other.notifications, ScalarWebMethod.COMPARATOR);
    }

    @Override
    public String toString() {
        return "SonyServiceCapability [serviceName=" + serviceName + ", version=" + version + ", transport=" + transport
                + ", methods=" + methods + ", notifications=" + notifications + "]";
    }
}
