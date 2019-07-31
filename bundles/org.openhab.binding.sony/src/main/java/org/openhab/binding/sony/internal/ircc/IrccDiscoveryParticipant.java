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
package org.openhab.binding.sony.internal.ircc;

import java.io.IOException;
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
import org.openhab.binding.sony.internal.ircc.models.IrccClient;
import org.openhab.binding.sony.internal.ircc.models.IrccSystemInformation;
import org.openhab.binding.sony.internal.providers.SonyDefinitionProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This implementation of the {@link UpnpDiscoveryParticipant} provides discovery of Sony IRCC protocol devices.
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class IrccDiscoveryParticipant extends AbstractDiscoveryParticipant implements UpnpDiscoveryParticipant {

    /**
     * Constructs the participant
     */
    public IrccDiscoveryParticipant() {
        super(SonyBindingConstants.IRCC_THING_TYPE_PREFIX);
    }

    @Override
    public @Nullable DiscoveryResult createResult(RemoteDevice device) {
        Objects.requireNonNull(device, "device cannot be null");

        final ThingUID uid = getThingUID(device);
        if (uid == null) {
            return null;
        }

        final RemoteDeviceIdentity identity = device.getIdentity();
        final URL irccURL = identity.getDescriptorURL();

        String sysWolAddress = null;

        try {
            final IrccClient irccClient = new IrccClient(irccURL.toString());
            final IrccSystemInformation systemInformation = irccClient.getSystemInformation();
            sysWolAddress = systemInformation.getWolMacAddress();
        } catch (IOException e) {
            logger.debug("Exception getting device info: {}", e.getMessage(), e);
            return null;
        }

        final IrccConfig config = new IrccConfig();
        config.setCommandsMapFile("ircc-" + uid.getId() + ".map");
        config.setDeviceMacAddress(
                sysWolAddress == null || StringUtils.isEmpty(sysWolAddress) ? getMacAddress(identity, uid)
                        : sysWolAddress);

        config.setDeviceAddress(irccURL.toString());

        return DiscoveryResultBuilder.create(uid).withProperties(config.asProperties())
                .withLabel(getLabel(device, "IRCC")).build();
    }

    @Override
    public @Nullable ThingUID getThingUID(RemoteDevice device) {
        Objects.requireNonNull(device, "device cannot be null");

        if (isSonyDevice(device)) {
            // if (isScalarThingType(device)) {
            //     logger.debug("Found a SCALAR thing type for this IRCC thing - ignoring IRCC");
            //     return null;
            // }

            final String modelName = getModelName(device);
            if (modelName == null || StringUtils.isEmpty(modelName)) {
                logger.debug("Found Sony device but it has no model name - ignoring");
                return null;
            }

            final RemoteService irccService = device.findService(
                    new ServiceId(SonyBindingConstants.SONY_SERVICESCHEMA, SonyBindingConstants.SONY_IRCCSERVICENAME));
            if (irccService != null) {
                final RemoteDeviceIdentity identity = device.getIdentity();
                if (identity != null) {
                    final UDN udn = device.getIdentity().getUdn();
                    logger.debug("Found Sony IRCC service: {}", udn);
                    final ThingTypeUID modelUID = getThingTypeUID(modelName);
                    return UidUtils.createThingUID(modelUID == null ? IrccConstants.THING_TYPE_IRCC : modelUID, udn);
                } else {
                    logger.debug("Found Sony IRCC service but it had no identity!");
                }
            } else {
                logger.debug("Could not find the IRCC service for device: {}", device);
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
