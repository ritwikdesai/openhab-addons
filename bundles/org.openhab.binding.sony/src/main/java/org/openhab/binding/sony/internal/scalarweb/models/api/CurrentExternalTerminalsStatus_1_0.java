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

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class represents a current external terminal status
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class CurrentExternalTerminalsStatus_1_0 {

    public static final String META_ZONEOUTPUT = "meta:zone:output";

    /** The status of the terminal */
    private @Nullable String active;

    /** The connection status */
    private @Nullable String connection;

    /** The icon for the terminal */
    private @Nullable String iconUrl;

    /** The label of the terminal */
    private @Nullable String label;

    /** The meta identifying information on the terminal */
    private @Nullable String meta;

    /** The outputs associated with this terminal (usually an input) */
    private @Nullable String @Nullable [] outputs;

    /** The title (name) of the terminal */
    private @Nullable String title;

    /** The uri identifying the terminal */
    private @Nullable String uri;

    public CurrentExternalTerminalsStatus_1_0(String uri, String title) {
        this.uri = uri;
        this.title = title;
        this.meta = META_ZONEOUTPUT;
    }

    /**
     * Gets the status of the terminal
     *
     * @return the status of the terminal
     */
    public @Nullable String getActive() {
        return active;
    }

    /**
     * Gets the connection of the terminal
     *
     * @return the connection of the terminal
     */
    public @Nullable String getConnection() {
        return connection;
    }

    /**
     * Gets the icon of the terminal
     *
     * @return the icon of the terminal
     */
    public @Nullable String getIconUrl() {
        return iconUrl;
    }

    /**
     * Gets the label of the terminal
     *
     * @return the label of the terminal
     */
    public @Nullable String getLabel() {
        return label;
    }

    public @Nullable String getMeta() {
        return meta;
    }

    public @Nullable String @Nullable [] getOutputs() {
        return outputs;
    }

    /**
     * Gets the title of the terminal
     *
     * @return the title of the terminal
     */
    public @Nullable String getTitle() {
        return title;
    }

    public String getTitle(String defaultValue) {
        final String titleOrLabel = title == null || StringUtils.isEmpty(title) ? label : title;
        return titleOrLabel == null || StringUtils.isEmpty(titleOrLabel) ? defaultValue : titleOrLabel;
    }

    /**
     * Gets the uri of the terminal
     *
     * @return the uri of the terminal
     */
    public @Nullable String getUri() {
        return uri;
    }

    public boolean isOutput() {
        return StringUtils.equalsIgnoreCase(meta, META_ZONEOUTPUT) || StringUtils.startsWith(uri, Scheme.EXT_OUTPUT);

    }

    @Override
    public String toString() {
        return "CurrentExternalTerminalsStatus_1_0 [uri=" + uri + ", title=" + title + ", connection=" + connection
                + ", label=" + label + ", iconUrl=" + iconUrl + ", active=" + active + ", outputs="
                + Arrays.toString(outputs) + ", meta=" + meta + "]";
    }
}
