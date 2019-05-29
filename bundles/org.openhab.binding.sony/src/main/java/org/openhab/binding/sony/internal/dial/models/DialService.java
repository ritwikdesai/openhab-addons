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
package org.openhab.binding.sony.internal.dial.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * This class represents the root element in the XML for a DIAL service. The XML that will be deserialized will look
 * like
 *
 * <pre>
 * {@code
   <?xml version="1.0" encoding="UTF-8"?>
   <service>
     <app>
       <id>com.sony.videoexplorer</id>
       <name>Video Explorer</name>
       <supportAction>
         <action>start</action>
       </supportAction>
       <icon_url></icon_url>
     </app>
     <app>
       <id>com.sony.musicexplorer</id>
       <name>Music Explorer</name>
       <supportAction>
         <action>start</action>
       </supportAction>
       <icon_url></icon_url>
     </app>
     <app>
       <id>com.sony.videoplayer</id>
       <name>Video Player</name>
       <supportAction>
         <action>start</action>
       </supportAction>
       <icon_url></icon_url>
     </app>
     ...
   </service>
 * }
 * </pre>
 *
 * Please note this class is used strictly in the deserialization process and retrieval of the {@link DialApp}
 *
 * @author Tim Roberts - Initial contribution
 *
 */
@NonNullByDefault
@XStreamAlias("service")
class DialService {
    /** The list of {@link DialApp} */
    @XStreamImplicit(itemFieldName = "app")
    private @Nullable List<DialApp> apps;

    /**
     * Returns the list of {@link DialApp} for the service
     *
     * @return a non-null, possibly empty list of {@link DialApp}
     */
    public List<DialApp> getApps() {
        return Collections.unmodifiableList(apps == null ? new ArrayList<>() : apps);
    }
}
