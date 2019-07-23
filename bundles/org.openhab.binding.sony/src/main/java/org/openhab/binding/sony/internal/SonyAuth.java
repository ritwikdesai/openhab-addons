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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Objects;

import com.google.gson.Gson;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.sony.internal.ircc.models.IrccClient;
import org.openhab.binding.sony.internal.ircc.models.IrccSystemInformation;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.net.NetUtil;
import org.openhab.binding.sony.internal.scalarweb.gson.GsonUtilities;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebError;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebMethod;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebRequest;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.openhab.binding.sony.internal.scalarweb.models.api.ActRegisterId;
import org.openhab.binding.sony.internal.scalarweb.models.api.ActRegisterOptions;
import org.openhab.binding.sony.internal.transports.SonyTransport;
import org.openhab.binding.sony.internal.transports.TransportOption;
import org.openhab.binding.sony.internal.transports.TransportOptionHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains all the logic to authorized against a sony device (either Scalar or IRCC)
 * 
 * @author Tim Roberts - Initial contribution
 */
public class SonyAuth {
    /** The logger */
    private Logger logger = LoggerFactory.getLogger(SonyAuth.class);

    private final Gson gson = GsonUtilities.getApiGson();

    private final @Nullable GetIrccClient getIrccClient;

    private final String activationUrl;
    private final String activationVersion;

    public SonyAuth(URL url) {
        activationUrl = NetUtil.getSonyUrl(url, ScalarWebService.ACCESSCONTROL);
        activationVersion = ScalarWebMethod.V1_0;
        getIrccClient = null;
    }

    public SonyAuth(@Nullable GetIrccClient getIrccClient) {
        this(getIrccClient, null);
    }

    public SonyAuth(@Nullable GetIrccClient getIrccClient, @Nullable ScalarWebService accessControlService) {
        this.getIrccClient = getIrccClient;

        String actUrl = null, actVersion = null;

        if (accessControlService != null) {
            actUrl = accessControlService == null ? null
                    : accessControlService.getTransport().getBaseUrl().toExternalForm();
            actVersion = accessControlService == null ? null
                    : accessControlService.getVersion(ScalarWebMethod.ACTREGISTER);
        }

        this.activationUrl = actUrl;
        this.activationVersion = actVersion == null || StringUtils.isEmpty(actVersion) ? ScalarWebMethod.V1_0
                : actVersion;
    }

    private String getDeviceIdHeaderName() {
        final IrccClient irccClient = getIrccClient == null ? null : getIrccClient.getClient();
        final IrccSystemInformation sysInfo = irccClient == null ? null : irccClient.getSystemInformation();
        final String actionHeader = sysInfo == null ? null : sysInfo.getActionHeader();
        return "X-" + (actionHeader == null || StringUtils.isEmpty(actionHeader) ? "CERS-DEVICE-ID" : actionHeader);
    }

    private @Nullable Integer getRegistrationMode() {
        final IrccClient irccClient = getIrccClient == null ? null : getIrccClient.getClient();
        return irccClient == null ? null : irccClient.getRegistrationMode();
    }

    private @Nullable String getRegistrationUrl() {
        final IrccClient irccClient = getIrccClient == null ? null : getIrccClient.getClient();
        return irccClient == null ? null : irccClient.getUrlForAction(IrccClient.AN_REGISTER);
    }

    private @Nullable String getActivationUrl() {
        if (activationUrl != null && StringUtils.isNotEmpty(activationUrl)) {
            return activationUrl;
        }

        final IrccClient irccClient = getIrccClient == null ? null : getIrccClient.getClient();
        return irccClient == null ? null : NetUtil.getSonyUrl(irccClient.getBaseUrl(), ScalarWebService.ACCESSCONTROL);
    }

    /**
     * Request access by initiating the registration or doing the activation if on the second step
     *
     * @param accessCode the access code (null for initial setup)
     * @return the http response
     */
    public AccessCheckResult requestAccess(SonyTransport transport, @Nullable String accessCode) {
        Objects.requireNonNull(transport, "transport cannot be null");

        logger.debug("Requesting access: " + accessCode);

        if (accessCode != null) {
            transport.setOption(new TransportOptionHeader(NetUtil.createAccessCodeHeader(accessCode)));
        }
        transport.setOption(new TransportOptionHeader(getDeviceIdHeaderName(), NetUtil.getDeviceId()));

        final ScalarWebResult result = scalarActRegister(transport, accessCode);
        final HttpResponse httpResponse = result.getHttpResponse();

        final String registrationUrl = getRegistrationUrl();
        if (httpResponse.getHttpCode() == HttpStatus.UNAUTHORIZED_401) {
            if (registrationUrl == null || StringUtils.isEmpty(registrationUrl)) {
                return accessCode == null ? AccessCheckResult.PENDING : AccessCheckResult.NOTACCEPTED;
            }
        }

        if (result.getDeviceErrorCode() == ScalarWebError.NOTIMPLEMENTED
                || (result.getDeviceErrorCode() == ScalarWebError.HTTPERROR
                        && httpResponse.getHttpCode() == HttpStatus.SERVICE_UNAVAILABLE_503)
                || httpResponse.getHttpCode() == HttpStatus.UNAUTHORIZED_401
                || httpResponse.getHttpCode() == HttpStatus.FORBIDDEN_403) {
            if (registrationUrl != null && StringUtils.isNotEmpty(registrationUrl)) {
                final HttpResponse irccResponse = irccRegister(transport, accessCode);
                if (irccResponse.getHttpCode() == HttpStatus.OK_200) {
                    return AccessCheckResult.OK;
                } else if (irccResponse.getHttpCode() == HttpStatus.UNAUTHORIZED_401) {
                    return AccessCheckResult.PENDING;
                } else {
                    return new AccessCheckResult(irccResponse);
                }
            }
        }

        if (result.getDeviceErrorCode() == ScalarWebError.DISPLAYISOFF) {
            return AccessCheckResult.DISPLAYOFF;
        }

        if (httpResponse.getHttpCode() == HttpStatus.SERVICE_UNAVAILABLE_503) {
            return AccessCheckResult.HOMEMENU;
        }

        if (httpResponse.getHttpCode() == HttpStatus.OK_200
                || result.getDeviceErrorCode() == ScalarWebError.ILLEGALARGUMENT) {
            return AccessCheckResult.OK;
        }

        return new AccessCheckResult(httpResponse);
    }

    /**
     * Register an access renewal
     *
     * @return the non-null {@link HttpResponse}
     */
    public AccessCheckResult registerRenewal(SonyTransport transport) {
        Objects.requireNonNull(transport, "transport cannot be null");

        logger.debug("Registering Renewal");

        transport.setOption(new TransportOptionHeader(getDeviceIdHeaderName(), NetUtil.getDeviceId()));

        final ScalarWebResult response = scalarActRegister(transport, null);

        // if good response, return it
        if (response.getHttpResponse().getHttpCode() == HttpStatus.OK_200) {
            return AccessCheckResult.OK;
        }

        // If we got a 401 (unauthorized) and there is no ircc registration url
        // return it as well
        final String registrationUrl = getRegistrationUrl();
        if (response.getHttpResponse().getHttpCode() == HttpStatus.UNAUTHORIZED_401
                && (registrationUrl == null || StringUtils.isEmpty(registrationUrl))) {
            return AccessCheckResult.NEEDSPAIRING;
        }

        final HttpResponse irccResponse = irccRenewal(transport);
        if (irccResponse.getHttpCode() == HttpStatus.OK_200) {
            return AccessCheckResult.OK;
        } else {
            return new AccessCheckResult(irccResponse);
        }
    }

    /**
     * Register the specified access code
     *
     * @param accessCode the possibly null access code
     * @return the non-null {@link HttpResponse}
     */
    private HttpResponse irccRegister(SonyTransport transport, @Nullable String accessCode) {
        Objects.requireNonNull(transport, "transport cannot be null");

        final String registrationUrl = getRegistrationUrl();
        if (registrationUrl == null || StringUtils.isEmpty(registrationUrl)) {
            return new HttpResponse(HttpStatus.SERVICE_UNAVAILABLE_503, "No registration URL");
        }

        // Do the registration first with what the mode says,
        // then try it again with the other mode (so registration mode sometimes lie)
        final String[] registrationTypes = new String[2];
        if (getRegistrationMode() == 2) {
            registrationTypes[0] = "new";
            registrationTypes[1] = "initial";
        } else {
            registrationTypes[0] = "initial";
            registrationTypes[1] = "new";
        }

        final TransportOption[] headers = accessCode == null ? new TransportOption[0]
                : new TransportOption[] { new TransportOptionHeader(NetUtil.createAuthHeader(accessCode)) };
        try {
            final String rqst = "?name=" + URLEncoder.encode(NetUtil.getDeviceName(), "UTF-8") + "&registrationType="
                    + registrationTypes[0] + "&deviceId=" + URLEncoder.encode(NetUtil.getDeviceId(), "UTF-8");
            final HttpResponse resp = transport.executeGet(registrationUrl + rqst, headers);
            if (resp.getHttpCode() != HttpStatus.BAD_REQUEST_400) {
                return resp;
            }
        } catch (UnsupportedEncodingException e) {
            // let it got to the next one...
        }

        try {
            final String rqst2 = "?name=" + URLEncoder.encode(NetUtil.getDeviceName(), "UTF-8") + "&registrationType="
                    + registrationTypes[1] + "&deviceId=" + URLEncoder.encode(NetUtil.getDeviceId(), "UTF-8");
            return transport.executeGet(registrationUrl + rqst2, headers);
        } catch (UnsupportedEncodingException e) {
            return new HttpResponse(HttpStatus.SERVICE_UNAVAILABLE_503, e.toString());
        }
    }

    private HttpResponse irccRenewal(SonyTransport transport) {
        Objects.requireNonNull(transport, "transport cannot be null");

        final String registrationUrl = getRegistrationUrl();
        if (registrationUrl == null || StringUtils.isEmpty(registrationUrl)) {
            return new HttpResponse(HttpStatus.SERVICE_UNAVAILABLE_503, "No registration URL");
        }

        try {
            final String parms = "?name=" + URLEncoder.encode(NetUtil.getDeviceName(), "UTF-8")
                    + "&registrationType=renewal&deviceId=" + URLEncoder.encode(NetUtil.getDeviceId(), "UTF-8");
            return transport.executeGet(registrationUrl + parms);
        } catch (UnsupportedEncodingException e) {
            return new HttpResponse(HttpStatus.SERVICE_UNAVAILABLE_503, e.toString());
        }
    }

    /**
     * Helper method to execute an ActRegister to register the system
     *
     * @param accessCode the access code to use (or null to initiate the first step of ActRegister)
     * @return the scalar web result
     */
    private ScalarWebResult scalarActRegister(SonyTransport transport, @Nullable String accessCode) {
        Objects.requireNonNull(transport, "transport cannot be null");

        final String actReg = gson.toJson(new ScalarWebRequest(1, ScalarWebMethod.ACTREGISTER, activationVersion,
                new ActRegisterId(), new Object[] { new ActRegisterOptions() }));

        final String actUrl = getActivationUrl();
        final HttpResponse r = transport.executePostJson(actUrl, actReg,
                accessCode == null ? new TransportOption[0]
                        : new TransportOption[] { new TransportOptionHeader(NetUtil.createAuthHeader(accessCode)) });

        if (r.getHttpCode() == HttpStatus.OK_200) {
            return gson.fromJson(r.getContent(), ScalarWebResult.class);
        } else {
            return new ScalarWebResult(r);
        }
    }

    public interface GetIrccClient {
        @Nullable
        IrccClient getClient();
    }
}