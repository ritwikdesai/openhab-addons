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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.sony.internal.SonyBindingConstants;

/**
 * The class provides all the constants specific to the scalar system. Please note that protocol specific constants are
 * found in the individual protocol class
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class ScalarWebConstants {

    // The thing constants
    public static final ThingTypeUID THING_TYPE_SCALAR = new ThingTypeUID(SonyBindingConstants.BINDING_ID,
            SonyBindingConstants.SCALAR_THING_TYPE_PREFIX);

    public static final String ACCESSCODE_RQST = "RQST";
    public static final String ACCESSCODE_PENDING = "Access Code requested. Please update the Access Code with what is shown on the device screen";
    public static final String ACCESSCODE_NOTACCEPTED = "Access code was not accepted - please either request a new one or verify number matches what's shown on the device";

    public static final String GRP_PRIMARY = "primary";
    public static final String GRP_VIEWING = "viewing";
    public static final String GRP_CONTENT = "content";

    // The thing properties
    public static final String PROP_SERIAL = "Serial #";
    public static final String PROP_MACADDR = "MAC Address";
    public static final String PROP_AREA = "Area";
    public static final String PROP_REGION = "Region";
    public static final String PROP_GENERATION = "Generation";
    public static final String PROP_MODEL = "Model #";
    public static final String PROP_NAME = "Name";
    public static final String PROP_PRODUCT = "Product";
    public static final String PROP_INTERFACEVERSION = "Interface Version";
    public static final String PROP_SERVERNAME = "Server Name";
    public static final String PROP_PRODUCTCATEGORY = "Product Category";
    public static final String PROP_NETIF = "Network Interface";
    public static final String PROP_IPV4 = "IP4 Address";
    public static final String PROP_IPV6 = "IP6 Address";
    public static final String PROP_DNS = "DNS";
    public static final String PROP_GATEWAY = "Gateway";
    public static final String PROP_HWADDRESS = "HW Address";
    public static final String PROP_NETMASK = "Net Mask";

    // The config URI for creating thing types
    public static final String CFG_URI = "thing-type:sony:scalarconfig";

    // The timeout (in seconds) to wait on a response
    public static final Integer RSP_WAIT_TIMEOUTSECONDS = 10;
}
