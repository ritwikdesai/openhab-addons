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

import org.eclipse.jdt.annotation.Nullable;

/**
 * The digital audio broadcasting (DAB) information class used for deserialization only
 *
 * @author Tim Roberts - Initial contribution
 */
public class DabInfo {
    private @Nullable String componentLabel;
    private @Nullable String dynamicLabel;
    private @Nullable String ensembleLabel;
    private @Nullable String serviceLabel;

    public @Nullable String getComponentLabel() {
        return componentLabel;
    }

    public @Nullable String getDynamicLabel() {
        return dynamicLabel;
    }

    public @Nullable String getEnsembleLabel() {
        return ensembleLabel;
    }

    public @Nullable String getServiceLabel() {
        return serviceLabel;
    }

    @Override
    public String toString() {
        return "DabInfo [componentLabel=" + componentLabel + ", dynamicLabel=" + dynamicLabel + ", ensembleLabel="
                + ensembleLabel + ", serviceLabel=" + serviceLabel + "]";
    }
}