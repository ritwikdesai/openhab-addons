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
package org.openhab.binding.sony.internal.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;

/**
 * The class represents a sony device service capability. The capability describes the service and the
 * methods/notifications for the service. The class will only be used to serialize the definition.
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SonyServiceCapability {
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

    @Override
    public String toString() {
        return "SonyServiceCapability [serviceName=" + serviceName + ", version=" + version + ", transport=" + transport
                + ", methods=" + methods + ", notifications=" + notifications + "]";
    }
}
