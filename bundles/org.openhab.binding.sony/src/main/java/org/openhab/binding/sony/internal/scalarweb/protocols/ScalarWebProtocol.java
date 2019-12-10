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
package org.openhab.binding.sony.internal.scalarweb.protocols;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannel;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelDescriptor;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;

/**
 * The interface definition for all protocols
 *
 * @author Tim Roberts - Initial contribution
 * @param <T> the generic type for the callback
 */
@NonNullByDefault
public interface ScalarWebProtocol<T extends ThingCallback<String>> {

    /**
     * Gets the channel descriptors.
     *
     * @return the channel descriptors
     */
    public Collection<ScalarWebChannelDescriptor> getChannelDescriptors();

    /**
     * Refresh state.
     */
    public void refreshState();

    /**
     * Refresh channel.
     *
     * @param channel the non-null channel
     */
    public void refreshChannel(ScalarWebChannel channel);

    /**
     * Sets the channel.
     *
     * @param channel the non-null channel
     * @param command the non-null command
     */
    public void setChannel(ScalarWebChannel channel, Command command);

    public ScalarWebService getService();

    public boolean isDynamic();
    
    /**
     * Defines a close method to release resources. We do NOT implement AutoCloseable since that forces an exception
     * onto this method (which we don't need)
     */
    public void close();
}
