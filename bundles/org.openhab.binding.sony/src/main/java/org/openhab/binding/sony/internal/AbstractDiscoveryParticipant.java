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

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.openhab.binding.sony.internal.net.NetUtil;
import org.openhab.binding.sony.internal.providers.SonyDefinitionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class provides all the base functionality for discovery participants
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractDiscoveryParticipant {

    /** The logger */
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The sony definition provider - must be SET by super classes since it's an OSGI service */
    protected @NonNullByDefault({}) SonyDefinitionProvider sonyDefinitionProvider;

    /** The service this discovery participant is for */
    private final String service;

    /**
     * Construct the participant from the given service
     *
     * @param service a non-null, non-empty service
     */
    protected AbstractDiscoveryParticipant(String service) {
        Validate.notEmpty(service, "service cannot be empty");
        this.service = service;
    }

    /**
     * Returns a list of thing type uids supported by this discovery implementation
     *
     * @return a non-null, never empty list of supported thing type uids
     */
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        final Set<ThingTypeUID> uids = new HashSet<ThingTypeUID>();

        // Add the generic one
        uids.add(new ThingTypeUID(SonyBindingConstants.BINDING_ID, service));

        // Add any specific ones
        final SonyDefinitionProvider localSonyDefinitionProvider = sonyDefinitionProvider;
        if (localSonyDefinitionProvider != null) {
            for (ThingType tt : localSonyDefinitionProvider.getThingTypes(null)) {
                uids.add(tt.getUID());
            }
        }
        return uids;
    }

    /**
     * Determines if the remote device is a sony device (based on it's manufacturer)
     *
     * @param device a non-null device
     * @return true if it's a sony device, false otherwise
     */
    protected boolean isSonyDevice(RemoteDevice device) {
        Objects.requireNonNull(device, "device cannot be null");

        final DeviceDetails details = device.getDetails();
        final String manufacturer = details == null || details.getManufacturerDetails() == null ? null
                : details.getManufacturerDetails().getManufacturer();
        return details != null
                && StringUtils.containsIgnoreCase(manufacturer, SonyBindingConstants.SONY_REMOTEDEVICE_ID);
        // return false;
    }

    /**
     * Get model name for the remove device
     *
     * @param device a non-null device
     * @return the model name or null if none found
     */
    protected @Nullable String getModelName(RemoteDevice device) {
        Objects.requireNonNull(device, "device cannot be null");

        final DeviceDetails details = device.getDetails();
        return details == null || details.getModelDetails() == null ? null : details.getModelDetails().getModelName();
    }

    /**
     * Get model description for the remove device
     *
     * @param device a non-null device
     * @return the model description or null if none found
     */
    protected @Nullable String getModelDescription(RemoteDevice device) {
        Objects.requireNonNull(device, "device cannot be null");

        final DeviceDetails details = device.getDetails();
        return details == null || details.getModelDetails() == null ? null
                : details.getModelDetails().getModelDescription();
    }

    /**
     * Get's the mac address of the remote device (or if the UID is a mac address)
     *
     * @param identity a non-null identity
     * @param uid      a non-null tyhing UID
     * @return a valid mac address if found, null if not
     */
    protected static @Nullable String getMacAddress(RemoteDeviceIdentity identity, ThingUID uid) {
        final String wolMacAddress = NetUtil.getMacAddress(identity.getWakeOnLANBytes());
        if (NetUtil.isMacAddress(wolMacAddress)) {
            return wolMacAddress;
        } else if (NetUtil.isMacAddress(uid.getId())) {
            return uid.getId();
        }
        return null;
    }

    /**
     * Gets the label from the remote device
     *
     * @param device a non-null, non-empty device
     * @param suffix a non-null, non-empty suffix
     * @return the label for the device
     */
    protected static String getLabel(RemoteDevice device, String suffix) {
        Objects.requireNonNull(device, "device cannot be null");
        Validate.notEmpty(suffix, "suffix cannot be empty");
        
        return (StringUtils.isEmpty(device.getDetails().getFriendlyName()) ? device.getDisplayString()
                : device.getDetails().getFriendlyName()) + " " + suffix;
    }

    /**
     * Determines if there is a scalar thing type defined for the device
     *
     * @param device the non-null device
     * @return true if found, false otherwise
     */
    protected boolean isScalarThingType(RemoteDevice device) {
        Objects.requireNonNull(device, "device cannot be null");
        return getThingTypeUID(SonyBindingConstants.SCALAR_THING_TYPE_PREFIX, getModelName(device)) != null;
    }

    /**
     * Returns the thing type related to the model name. This will search the registry for a thing type that is
     * specific to the model. If found, that thing type will be used. If not found, the generic scalar thing type will
     * be used
     *
     * @param modelName a possibly null, possibly empty model name
     * @return a ThingTypeUID if found, null if not
     */
    protected @Nullable ThingTypeUID getThingTypeUID(@Nullable String modelName) {
        return getThingTypeUID(service, modelName);
    }

    /**
     * Returns the thing type related to the service/model name. This will search the registry for a thing type that is
     * specific to the model. If found, that thing type will be used. If not found, the generic scalar thing type will
     * be used
     *
     * @param service   a non-null, non-empty service
     * @param modelName a possibly null, possibly empty model name
     * @return a ThingTypeUID if found, null if not
     */
    private @Nullable ThingTypeUID getThingTypeUID(String service, @Nullable String modelName) {
        Validate.notEmpty(service, "service cannot be empty");

        if (modelName == null || StringUtils.isEmpty(modelName)) {
            logger.debug("Emtpy model name!");
            return null;
        }

        final SonyDefinitionProvider provider = sonyDefinitionProvider;
        if (provider == null) {
            logger.debug("No definition provider!");
            return null;
        }

        final String lowerModelName = modelName.toLowerCase();
        for (ThingType tt : provider.getThingTypes(Locale.getDefault())) {
            final String typeId = tt.getUID().getId();
            // If it's not the generic one...
            if (!typeId.equals(service) && typeId.startsWith(service)) {
                final String model = typeId.substring(service.length() + 1);
                final String modelPattern = model.replaceAll("x", ".*").toLowerCase();
                if (lowerModelName.matches(modelPattern)) {
                    logger.debug("Using specific thing type for {}: {}", modelName, typeId);
                    return tt.getUID();
                }
            }
        }
        logger.debug("No specific thing type found for {}", modelName);
        return null;
    }
}
