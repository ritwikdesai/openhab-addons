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
package org.openhab.binding.sony.internal;

import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.jupnp.model.types.UDN;

/**
 * Utility class for various UID type of operations
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class UidUtils {

    /** The delimiter for channel groups */
    private static final char DELIMITER = '-';

    /**
     * Gets the device id from the UUID string ("[uuid:]{deviceid}::etc") from the specified {@link UDN}
     *
     * @param udn a non-null {@link UDN}
     * @return the device id or null (possibly empty) if not found
     */
    @Nullable
    public static String getDeviceId(UDN udn) {
        Objects.requireNonNull(udn, "udn cannot be null");

        final String uuid = udn.getIdentifierString();

        final String[] uuidParts = StringUtils.split(uuid, ':');
        if (uuidParts == null || uuidParts.length == 0) {
            return null;
        } else if (uuidParts.length == 1) {
            return uuidParts[0]; // probably was just "{deviceid}" or "{deviceid}:etc"
        } else if (uuidParts.length == 2) {
            return uuidParts[1]; // probably was "uuid:{deviceid}.."
        }
        return uuid;
    }

    /**
     * Create a {@link ThingUID} for the specified {@link ThingTypeUID} and {@link UDN}
     *
     * @param udn a non-null {@link UDN}
     * @return a possibly null {@link ThingUID}
     */
    @Nullable
    public static String getThingId(UDN udn) {
        Objects.requireNonNull(udn, "udn cannot be null");

        final String uuid = getDeviceId(udn);
        if (StringUtils.isEmpty(uuid)) {
            return null;
        }

        // Not entirely correct however most UUIDs are version 1
        // which makes the last node the mac address
        // Close enough to unique for our purposes - we just
        // verify the mac address is 12 characters in length
        // if not, we fall back to using the full uuid
        final String[] uuidParts = StringUtils.split(uuid, '-');
        final String macAddress = uuidParts[uuidParts.length - 1];
        return macAddress.length() == 12 ? macAddress : uuid;
    }

    /**
     * Create a {@link ThingUID} for the specified {@link ThingTypeUID} and {@link UDN}
     *
     * @param thingTypeId a non-null {@link ThingTypeUID}
     * @param udn         a non-null {@link UDN}
     * @return a possibly null {@link ThingUID}
     */
    public static ThingUID createThingUID(ThingTypeUID thingTypeId, UDN udn) {
        Objects.requireNonNull(thingTypeId, "thingTypeId cannot be null");
        Objects.requireNonNull(udn, "udn cannot be null");
        return new ThingUID(thingTypeId, getThingId(udn));
    }

    /**
     * Generates the ThingTypeUID for the given prefix and {@link ThingUID}. The resulting UID will be prefix +
     * {@link #DELIMITER} + {@link ThingUID#getId()}
     *
     * @param prefix  the non-null, non-empty prefix to add
     * @param thingId the non-null {@link ThingUID}
     * @return the non-null thing type UID
     */
    public static ThingTypeUID createThingTypeUID(String prefix, String thingId) {
        Validate.notEmpty(prefix, "prefix cannot be empty");
        Validate.notEmpty(thingId, "thingId cannot be empty");
        return new ThingTypeUID(SonyBindingConstants.BINDING_ID, prefix + DELIMITER + thingId);
    }

    /**
     * Encodes an identifier to a UID. A UID only allows alphanumerics, '-' and '_' and this will encode any values
     * outside of that. The encoding follows these rules for each character:
     * <ol>
     * <li>If an alphanumeric, simply code as the same character</li>
     * <li>If a period (very common), encode as a single '-' (dash)</li>
     * <li>If a dash (not as common), encode as "--" (double dash)</li>
     * <li>If a underscore (not common), encode as "__" (double underscore)</li>
     * <li>All other characters, convert to base36 and encode as "_XYYY" where X is the base36 variable length and YYY
     * is the base36 variable value</li>
     * </ol>
     *
     * @param id the non-null, non empty ID to encode
     * @return the non-null, non empty encoded UID
     */
    public static String encodeToUID(String id) {
        Validate.notEmpty(id, "id cannot be empty");

        final StringBuilder newUid = new StringBuilder(id.length() * 2);

        final char[] uidChars = id.toCharArray();
        final int uidCharsLen = uidChars.length;
        for (int x = 0; x < uidCharsLen; x++) {
            final char c = uidChars[x];
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                newUid.append(c);
            } else if (c == '.') {
                newUid.append('-');
            } else if (c == '-') {
                newUid.append("--");
            } else if (c == '_') {
                newUid.append("__");
            } else {
                final String base36 = Integer.toString(c, 36);
                final int base36Len = base36.length();

                // base36Len should be, at most, 6 in length for a java character
                // but since we can handle a single digit - validate to 9
                if (base36Len > 9) {
                    throw new IllegalArgumentException(
                            "Base36 lenght for a character > 9 - which shouldn't be possible!: " + id + ":" + x);
                }

                newUid.append('_');
                newUid.append(base36Len);
                newUid.append(base36);
            }
        }

        return newUid.toString();
    }

    /**
     * Decodes the UID to the original ID. This method uses the rules specified in {@link #encodeToUID(String)} to
     * decode the ID
     *
     * @param uid a non-null, non empty UID
     * @return a non-null, non empty string representing the ID
     */
    public static String decodeFromUID(String uid) {
        Validate.notEmpty(uid, "uid cannot be empty");

        final StringBuilder newUid = new StringBuilder(uid.length());

        final char[] uidChars = uid.toCharArray();
        final int uidCharsLen = uidChars.length;
        for (int x = 0; x < uidCharsLen; x++) {
            final char c = uidChars[x];

            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                newUid.append(c);
            } else if (c == '-') {
                if (x + 1 < uidCharsLen) {
                    if (uidChars[x + 1] == '-') {
                        newUid.append(uidChars[++x]);
                    } else {
                        newUid.append('.');
                    }
                } else {
                    newUid.append('.');
                }
            } else if (c == '_') {
                if (x + 1 < uidCharsLen) {
                    final char lenChar = uidChars[++x];
                    if (lenChar == '_') {
                        newUid.append('_');
                    } else {
                        final int len = lenChar;
                        if (x + len < uidCharsLen) {
                            final String base36 = new String(uidChars, x, len);
                            x += len;
                            newUid.append(Integer.parseInt(base36, 36));
                        } else {
                            throw new IllegalArgumentException(
                                    "uid cannot be decoded - base36 incomplete: " + uid + " (" + x + ")");
                        }
                    }
                } else {
                    throw new IllegalArgumentException(
                            "uid cannot be decoded - ended in a single underscore: " + uid + " (" + x + ")");
                }
            } else {
                throw new IllegalArgumentException(
                        "uid cannot be decoded - unknown control character: " + uid + " (" + x + ")");
            }
        }

        return newUid.toString();

    }
}
