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
package org.openhab.binding.sony.internal.simpleip;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.AbstractConfig;
import org.openhab.binding.sony.internal.SonyUtil;

/**
 * Configuration class for the {@link SimpleIpHandler}.
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SimpleIpConfig extends AbstractConfig {

    /** The commands map file. */
    private @Nullable String commandsMapFile;

    /** The network interface the sony system listens on (eth0 or wlan0). */
    private @Nullable String netInterface;

    
    // ---- the following properties are not part of the config.xml (and are properties) ----

    /** The commands map file. */
    private @Nullable String discoveredCommandsMapFile;

    @Override
    public @Nullable String getDeviceIpAddress() {
        return super.getDeviceAddress();
    }

    @Override
    public @Nullable Integer getDevicePort() {
        return SimpleIpConstants.PORT;
    }

    /**
     * Gets the network interface being used.
     *
     * @return the network interface
     */
    public String getNetInterface() {
        final String localNetInterface = netInterface;
        return localNetInterface == null || StringUtils.isEmpty(localNetInterface) ? "eth0" : localNetInterface;
    }

    /**
     * Sets the network interface being used.
     *
     * @param netInterface the network interface
     */
    public void setNetInterface(String netInterface) {
        this.netInterface = netInterface;
    }

    /**
     * Gets the commands map file name.
     *
     * @return the commands map file name
     */

    public @Nullable String getCommandsMapFile() {
        return commandsMapFile == null || StringUtils.isEmpty(commandsMapFile) ? discoveredCommandsMapFile : commandsMapFile;
    }

    /**
     * Sets the command map file name.
     *
     * @param commandsMapFile the command map file name
     */
    public void setCommandsMapFile(String commandsMapFile) {
        this.commandsMapFile = commandsMapFile;
    }

    /**
     * Sets the discovered command map file name.
     *
     * @param discoveredCommandsMapFile the command map file name
     */
    public void setDiscoveredCommandsMapFile(String discoveredCommandsMapFile) {
        this.discoveredCommandsMapFile = discoveredCommandsMapFile;
    }

    @Override
    public Map<String, Object> asProperties() {
        final Map<String, Object> props = super.asProperties();

        props.put("discoveredCommandsMapFile", SonyUtil.convertNull(discoveredCommandsMapFile, ""));
        conditionallyAddProperty(props, "commandsMapFile", commandsMapFile);
        conditionallyAddProperty(props, "netInterface", netInterface);
        
        return props;
    }
}
