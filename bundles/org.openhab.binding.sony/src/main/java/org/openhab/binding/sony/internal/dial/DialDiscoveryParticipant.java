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
package org.openhab.binding.sony.internal.dial;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.upnp.UpnpDiscoveryParticipant;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.UDN;
import org.openhab.binding.sony.internal.AbstractDiscoveryParticipant;
import org.openhab.binding.sony.internal.SonyBindingConstants;
import org.openhab.binding.sony.internal.UidUtils;
import org.openhab.binding.sony.internal.dial.models.DialClient;
import org.openhab.binding.sony.internal.net.NetUtil;
import org.openhab.binding.sony.internal.providers.SonyDefinitionProvider;
import org.openhab.binding.sony.internal.transports.SonyTransport;
import org.openhab.binding.sony.internal.transports.SonyTransportFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This implementation of the {@link UpnpDiscoveryParticipant} provides discovery of Sony DIAL protocol devices.
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class DialDiscoveryParticipant extends AbstractDiscoveryParticipant implements UpnpDiscoveryParticipant {

    /**
     * Creates the discovery participant 
     */
    public DialDiscoveryParticipant() {
        super(SonyBindingConstants.DIAL_THING_TYPE_PREFIX);
    }

    @Override
    public @Nullable DiscoveryResult createResult(RemoteDevice device) {
        Objects.requireNonNull(device, "device cannot be null");

        final ThingUID uid = getThingUID(device);
        if (uid == null) {
            return null;
        }

        final RemoteDeviceIdentity identity = device.getIdentity();
        final URL dialURL = identity.getDescriptorURL();

        String deviceId;
        try (SonyTransport transport = SonyTransportFactory.createHttpTransport(dialURL.toString())) {
            final DialClient dialClient = DialClient.get(transport, dialURL.toString());
            if (dialClient == null || !dialClient.hasDialService()) {
                logger.debug("DIAL device didn't implement any device infos - ignoring: {}", identity);
                return null;
            }

            deviceId = dialClient.getFirstDeviceId();
        } catch (IOException | URISyntaxException e) {
            logger.debug("DIAL device exception {}: {}", device.getIdentity(), e.getMessage(), e);
            return null;
        }

        final DialConfig config = new DialConfig();

        String macAddress = getMacAddress(identity, uid);
        if (macAddress == null && NetUtil.isMacAddress(deviceId)) {
            macAddress = deviceId;
        }
        config.setDiscoveredMacAddress(macAddress);
        config.setDeviceAddress(dialURL.toString());

        return DiscoveryResultBuilder.create(uid).withProperties(config.asProperties())
                .withProperty("DialUDN", UidUtils.getThingId(identity.getUdn()))
                .withRepresentationProperty("DialUDN")
                .withLabel(getLabel(device, "DIAL")).build();
    }

    @Override
    public @Nullable ThingUID getThingUID(RemoteDevice device) {
        Objects.requireNonNull(device, "device cannot be null");

        if (isSonyDevice(device)) {
            // if (isScalarThingType(device)) {
            //     logger.debug("Found a SCALAR thing type for this DIAL thing - ignoring DIAL");
            //     return null;
            // }
            final String modelName = getModelName(device);
            if (modelName == null || StringUtils.isEmpty(modelName)) {
                logger.debug("Found Sony device but it has no model name - ignoring");
                return null;
            }

            final RemoteService dialService = device.findService(
                    new ServiceId(SonyBindingConstants.DIAL_SERVICESCHEMA, SonyBindingConstants.SONY_DIALSERVICENAME));
            if (dialService != null) {
                final RemoteDeviceIdentity identity = device.getIdentity();
                if (identity != null) {
                    final UDN udn = device.getIdentity().getUdn();
                    final String thingID = UidUtils.getThingId(udn);

                    if (thingID != null) {
                        logger.debug("Found Sony DIAL service: {}", udn);
                        final ThingTypeUID modelUID = getThingTypeUID(modelName);
                        return new ThingUID(modelUID == null ? DialConstants.THING_TYPE_DIAL : modelUID, thingID);
                    }
                } else {
                    logger.debug("Found Sony DIAL service but it had no identity!");
                }
            }
        }
        return null;
    }

    @Reference
    public void setSonyDefinitionProvider(SonyDefinitionProvider sonyDefinitionProvider) {
        Objects.requireNonNull(sonyDefinitionProvider, "sonyDefinitionProvider cannot be null");
        this.sonyDefinitionProvider = sonyDefinitionProvider;
    }

    public void unsetSonyDefinitionProvider(SonyDefinitionProvider sonyDefinitionProvider) {
        this.sonyDefinitionProvider = null;
    }
}
