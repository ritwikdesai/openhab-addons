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
package org.openhab.binding.sony.internal.scalarweb.transports;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.sony.internal.ircc.models.IrccClient;
import org.openhab.binding.sony.internal.net.Header;
import org.openhab.binding.sony.internal.net.HttpRequest;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.scalarweb.ScalarUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.openhab.binding.sony.internal.upnp.models.UpnpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SonyIrccTransport extends AbstractSonyTransport<String> {
    /** The logger */
    protected Logger logger = LoggerFactory.getLogger(SonyIrccTransport.class);

    private final IrccClient irccClient;
    /** The HTTP requestor */
    private final HttpRequest requestor;

    public SonyIrccTransport(IrccClient irccClient, HttpRequest requestor) {
        this.requestor = requestor;
        this.irccClient = irccClient;
    }

    @Override
    public Future<ScalarWebResult> executeRaw(String cmd) {
        return execute(cmd);
    }

    @Override
    public Future<ScalarWebResult> execute(String cmd) {
        final UpnpService service = irccClient.getService(IrccClient.SRV_IRCC);
        if (service == null) {
            logger.debug("IRCC Service was not found");
            return CompletableFuture
                    .completedFuture(ScalarUtilities.createErrorResult(404, "IRCC Service was not found"));
        }

        final String soap = irccClient.getSOAP(IrccClient.SRV_IRCC, IrccClient.SRV_ACTION_SENDIRCC, cmd);
        if (soap == null || StringUtils.isEmpty(soap)) {
            logger.debug("Unable to find the IRCC service/action to send IRCC");
            return CompletableFuture.completedFuture(
                    ScalarUtilities.createErrorResult(404, "Unable to find the IRCC service/action to send IRCC"));
        }

        final URL baseUrl = irccClient.getBaseUrl();
        final URL controlUrl = service.getControlUrl(baseUrl);
        if (controlUrl == null) {
            logger.debug("ControlURL for IRCC service wasn't found: {}", baseUrl);
            return CompletableFuture.completedFuture(
                    ScalarUtilities.createErrorResult(404, "ControlURL for IRCC service wasn't found: " + baseUrl));
        } else {
            logger.debug("Sending IRCC command: {} to {}", cmd, controlUrl);
            final HttpResponse resp = requestor.sendPostXmlCommand(controlUrl.toExternalForm(), soap, new Header(
                    "SOAPACTION", "\"" + service.getServiceType() + "#" + IrccClient.SRV_ACTION_SENDIRCC + "\""));

            if (resp.getHttpCode() == HttpStatus.OK_200) {
                final ScalarWebResult res = ScalarWebResult.createEmptySuccess();
                return CompletableFuture.completedFuture(res);
            } else {
                logger.debug("Execution of IRCC command {} to {} failed with {}", cmd, controlUrl, resp.getHttpCode());
                return CompletableFuture
                        .completedFuture(ScalarUtilities.createErrorResult(resp.getHttpCode(), resp.getContent()));
            }
        }
    }

    @Override
    public String getProtocolType() {
        return SonyTransport.IRCC;
    }

    @Override
    public void close() {
        logger.debug("Closing IRCC client to {}", irccClient.getBaseUrl());
        irccClient.close();
    }
}
