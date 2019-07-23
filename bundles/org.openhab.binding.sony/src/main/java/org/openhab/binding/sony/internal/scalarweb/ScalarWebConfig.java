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
package org.openhab.binding.sony.internal.scalarweb;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.AbstractConfig;

/**
 * Configuration class for the scalar web service
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarWebConfig extends AbstractConfig {

    /** The access code. */
    private @Nullable String accessCode;

    /** The commands map file. */
    private @Nullable String commandsMapFile;

    /** The URL to the IRCC service */
    private @Nullable String irccUrl;

    /**
     * Returns the IP address or host name.
     *
     * @return the IP address or host name
     */
    public @Nullable String getIpAddress() {
        try {
            return new URL(getDeviceAddress()).getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Gets the access code.
     *
     * @return the access code
     */
    public @Nullable String getAccessCode() {
        return accessCode;
    }

    /**
     * Sets the access code.
     *
     * @param accessCode the new access code
     */
    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    /**
     * Gets the commands map file.
     *
     * @return the commands map file
     */
    public @Nullable String getCommandsMapFile() {
        return commandsMapFile;
    }

    /**
     * Sets the commands map file.
     *
     * @param commandsMapFile the new commands map file
     */
    public void setCommandsMapFile(String commandsMapFile) {
        this.commandsMapFile = commandsMapFile;
    }

    /**
     * Get the IRCC url to use
     *
     * @return the ircc url
     */
    public @Nullable String getIrccUrl() {
        return irccUrl;
    }

    /**
     * Sets the IRCC url to use
     *
     * @param irccUrl the ircc url
     */
    public void setIrccUrl(String irccUrl) {
        this.irccUrl = irccUrl;
    }

    @Override
    public Map<String, Object> asProperties() {
        final Map<String, Object> props = super.asProperties();
        
        conditionallyAddProperty(props, "accessCode", accessCode);
        conditionallyAddProperty(props, "commandsMapFile", commandsMapFile);
        conditionallyAddProperty(props, "irccUrl", irccUrl);
        
        return props;
    }
}
