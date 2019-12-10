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

/**
 * 
 * @author Tim Roberts - Initial contribution
 */
public class CheckResult extends AccessResult {
    public static final CheckResult OK_HEADER = new CheckResult("okHeader", "OK");
    public static final CheckResult OK_COOKIE = new CheckResult("okCookie", "OK");

    private CheckResult(String code, String msg) {
        super(code, msg);
    }

    public CheckResult(AccessResult res) {
        super(res.getCode(), res.getMsg());
    }
}
