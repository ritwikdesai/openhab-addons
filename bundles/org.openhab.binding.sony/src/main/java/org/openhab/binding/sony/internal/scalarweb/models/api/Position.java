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

import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This class represents the request to set the picture-in-picture (PIP) location and is used for serialization only
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class Position {

    /** The PIP position */
    private final String position;

    /**
     * Instantiates a new position
     *
     * @param position the non-null, non-empty position
     */
    public Position(String position) {
        Validate.notEmpty(position, "position cannot be empty");
        this.position = position;
    }

    /**
     * Gets the position
     *
     * @return the position
     */
    public String getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "Position [position=" + position + "]";
    }
}
