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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.net.NetUtil;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebConstants;
import org.openhab.binding.sony.internal.transports.SonyTransport;
import org.openhab.binding.sony.internal.transports.TransportOptionHeader;

/**
 * This class contains all the logic to authorized against a sony device (either Scalar or IRCC)
 * 
 * @author Tim Roberts - Initial contribution
 */
public class SonyAuthChecker {
    private SonyTransport transport;
    private @Nullable String accessCode;

    public SonyAuthChecker(SonyTransport transport, @Nullable String accessCode) {
        Objects.requireNonNull(transport, "transport cannot be null");

        this.transport = transport;
        this.accessCode = accessCode;
    }

    public CheckResult checkResult(CheckResultCallback checkResult) {
        // Check to see if auth
        final String localAccessCode = accessCode;
        if (localAccessCode != null && !StringUtils.equalsIgnoreCase(ScalarWebConstants.ACCESSCODE_RQST, localAccessCode)) {
            final TransportOptionHeader authHeader = new TransportOptionHeader(NetUtil.createAccessCodeHeader(localAccessCode));
            try {

                transport.setOption(authHeader);
                if (AccessResult.OK.equals(checkResult.checkResult())) {
                    return CheckResult.OK_HEADER;
                }
            } finally {
                transport.removeOption(authHeader);
            }
        }

        final AccessResult res = checkResult.checkResult();
        if (AccessResult.OK.equals(res)) {
            return CheckResult.OK_COOKIE;
        }

        return new CheckResult(res);
    }

    public interface CheckResultCallback {
        AccessResult checkResult();
    }
}