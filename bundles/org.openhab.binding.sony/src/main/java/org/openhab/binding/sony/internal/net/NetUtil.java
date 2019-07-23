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
package org.openhab.binding.sony.internal.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class provides utility methods related to general network activities
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class NetUtil {
    /**
     * Gets the remote device identifier. Sony only requires it to be similar to a mac address and constant across
     * sessions. Sony AVs require the "MediaRemote:" part of the device ID (all other devices don't care).
     *
     * @return the non-null, non-empty device id
     */
    public static String getDeviceId() {
        return "MediaRemote:00-11-22-33-44-55";
    }

    /**
     * Gets the remote device name. The remote name will simply be "openHab({{getDeviceId()}})"
     *
     * @return the non-null, non-empty device name
     */
    public static String getDeviceName() {
        return "openHAB (" + getDeviceId() + ")";
    }

    /**
     * Creates an authorization header using the specified access code
     *
     * @param accessCode the access code
     * @return the non-null header
     */
    public static Header createAuthHeader(String accessCode) {
        final String authCode = java.util.Base64.getEncoder()
                .encodeToString((":" + StringUtils.leftPad(accessCode, 4, "0")).getBytes());
        return new Header("Authorization", "Basic " + authCode);
    }

    /**
     * Creates an access code header using the specified access code
     *
     * @param accessCode the access code
     * @return the non-null header
     */
    public static Header createAccessCodeHeader(String accessCode) {
        return new Header("X-Auth-PSK", accessCode);
    }

    /**
     * Returns the base url (protocol://domain{:port}) for a given url
     * @param url a non-null URL
     * @return the base URL
     */
    public static String toBaseUrl(URL url) {
        Objects.requireNonNull(url, "url cannot be null");

        final String protocol = url.getProtocol();
        final String host = url.getHost();
        final int port = url.getPort();

        return port == -1 ? String.format("%s://%s", protocol, host)
                : String.format("%s://%s:%d", protocol, host, port);
    }

    public static String getSonyUrl(URL baseUrl, String serviceName) {
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
        Validate.notEmpty(serviceName, "serviceName cannot be empty");

        final String protocol = baseUrl.getProtocol();
        final String host = baseUrl.getHost();

        return String.format("%s://%s/sony/%s", protocol, host, serviceName);        
    }

    /**
     * Send a wake on lan (WOL) packet to the specified ipAddress and macAddress
     *
     * @param ipAddress  the non-null, non-empty ip address
     * @param macAddress the non-null, non-empty mac address
     * @throws IOException if an IO exception occurs sending the WOL packet
     */
    public static void sendWol(String ipAddress, String macAddress) throws IOException {
        Validate.notEmpty(ipAddress, "ipAddress cannot be empty");
        Validate.notEmpty(macAddress, "macAddress cannot be empty");

        final byte[] macBytes = new byte[6];
        final String[] hex = macAddress.split("(\\:|\\-)");
        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                macBytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }

        final byte[] bytes = new byte[6 + 16 * macBytes.length];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) 0xff;
        }
        for (int i = 6; i < bytes.length; i += macBytes.length) {
            System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
        }

        // logger.debug("Sending WOL to " + ipAddress + " (" + macAddress + ")");

        // Resolve the ipaddress (in case it's a name)
        final InetAddress address = InetAddress.getByName(ipAddress);

        final byte[] addrBytes = address.getAddress();
        addrBytes[addrBytes.length - 1] = (byte) 255;
        final InetAddress broadcast = InetAddress.getByAddress(addrBytes);

        final DatagramPacket packet = new DatagramPacket(bytes, bytes.length, broadcast, 9);
        final DatagramSocket socket = new DatagramSocket();
        socket.send(packet);
        socket.close();
    }

    /**
     * Determines if the specified address is potentially formatted as a mac address or not
     *
     * @param potentialMacAddress a possibly null, possibly empty mac address
     * @return true if formatted like a mac address, false otherwise
     */
    public static boolean isMacAddress(@Nullable String potentialMacAddress) {
        if (potentialMacAddress == null || potentialMacAddress.length() != 17) {
            return false;
        }
        for (int i = 5; i >= 1; i--) {
            final char c = potentialMacAddress.charAt(i * 3 - 1);
            if (c != ':' && c != '-') {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the mac address represented by the byte array or null if not a WOL representation
     *
     * @param wakeOnLanBytes the possibly null wake on lan bytes
     * @return the mac address or null if not a mac address
     */
    public @Nullable static String getMacAddress(byte @Nullable [] wakeOnLanBytes) {
        if (wakeOnLanBytes != null && wakeOnLanBytes.length >= 12) {
            final StringBuffer macAddress = new StringBuffer(16);
            for (int i = 6; i < 12; i++) {
                macAddress.append(StringUtils.leftPad(Integer.toHexString(wakeOnLanBytes[i]), 2, '0'));
                macAddress.append(":");
            }
            macAddress.deleteCharAt(macAddress.length() - 1);
            return macAddress.toString();
        }
        return null;
    }

    /**
     * Get's the URL that is relative to another URL
     *
     * @param baseUrl  the non-null base url
     * @param otherUrl the non-null, non-empty other url
     * @return the combined URL or null if a malformed
     */
    public static @Nullable URL getUrl(URL baseUrl, String otherUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
        Validate.notEmpty(otherUrl, "otherUrl cannot be empty");
        try {
            return new URL(baseUrl, otherUrl);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Send a request to the specified ipaddress/port using a socket connection. Any results will be sent back via the
     * callback.
     *
     * @param ipAddress the non-null, non-empty ip address
     * @param port      the port
     * @param request   the non-null, non-empty request
     * @param callback  the non-null callback
     * @throws IOException if an IO exception occurs sending the request
     */
    public static void sendSocketRequest(String ipAddress, int port, String request, SocketSessionListener callback)
            throws IOException {
        Validate.notEmpty(ipAddress, "ipAddress cannot be empty");
        Validate.notEmpty(request, "request cannot be empty");
        Objects.requireNonNull(callback, "callback cannot be null");

        final Socket socket = new Socket();
        try {
            socket.setSoTimeout(3000);
            socket.connect(new InetSocketAddress(ipAddress, port));

            PrintStream ps = new PrintStream(socket.getOutputStream());
            BufferedReader bf = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            ps.println(request + "\n");
            ps.flush();

            int c;
            StringBuilder sb = new StringBuilder(100);
            while (true) {
                try {
                    c = bf.read();
                    if (c == -1) {
                        final String str = sb.toString();
                        callback.responseReceived(str);
                        break;
                    }
                    final char ch = (char) c;
                    if (ch == '\n') {
                        final String str = sb.toString();
                        sb.setLength(0);
                        if (callback.responseReceived(str)) {
                            break;
                        }
                    }
                    sb.append(ch);
                } catch (SocketTimeoutException e) {
                    final String str = sb.toString();
                    callback.responseReceived(str);
                    break;

                } catch (IOException e) {
                    callback.responseException(e);
                    break;
                }
            }
        } finally {
            socket.close();
        }
    }
}
