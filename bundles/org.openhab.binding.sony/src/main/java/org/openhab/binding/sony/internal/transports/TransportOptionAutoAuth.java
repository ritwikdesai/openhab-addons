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
package org.openhab.binding.sony.internal.transports;

/**
 * 
 * @author Tim Roberts - Initial contribution
 */
public class TransportOptionAutoAuth implements TransportOption {
    public static final TransportOptionAutoAuth TRUE = new TransportOptionAutoAuth(true);
    public static final TransportOptionAutoAuth FALSE = new TransportOptionAutoAuth(false);

    private final boolean autoAuth;

    public TransportOptionAutoAuth(boolean autoAuth) {
        this.autoAuth = autoAuth;
    }

    public boolean isAutoAuth() {
        return autoAuth;
    }
}
