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
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.transform.TransformationHelper;
import org.eclipse.smarthome.core.transform.TransformationService;
import org.eclipse.smarthome.io.net.http.WebSocketFactory;
import org.openhab.binding.sony.internal.dial.DialConstants;
import org.openhab.binding.sony.internal.dial.DialHandler;
import org.openhab.binding.sony.internal.ircc.IrccConstants;
import org.openhab.binding.sony.internal.ircc.IrccHandler;
import org.openhab.binding.sony.internal.providers.SonyDefinitionProvider;
import org.openhab.binding.sony.internal.providers.SonyDynamicStateProvider;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebHandler;
import org.openhab.binding.sony.internal.simpleip.SimpleIpConstants;
import org.openhab.binding.sony.internal.simpleip.SimpleIpHandler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link SonyHandlerFactory} is responsible for creating all things sony!
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = ThingHandlerFactory.class)
public class SonyHandlerFactory extends BaseThingHandlerFactory {
    /** websocket client used for scalar operations */
    private @NonNullByDefault({}) WebSocketClient webSocketClient;

    /** The sony thing type provider */
    private @NonNullByDefault({}) SonyDefinitionProvider sonyDefinitionProvider;

    /** The sony thing type provider */
    private @NonNullByDefault({}) SonyDynamicStateProvider sonyDynamicStateProvider;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        Objects.requireNonNull(thingTypeUID, "thingTypeUID cannot be null");
        return StringUtils.equalsIgnoreCase(SonyBindingConstants.BINDING_ID, thingTypeUID.getBindingId());
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        Objects.requireNonNull(thing, "thing cannot be null");

        final ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(SimpleIpConstants.THING_TYPE_SIMPLEIP)) {
            final TransformationService transformationService = TransformationHelper
                    .getTransformationService(getBundleContext(), "MAP");
            return new SimpleIpHandler(thing, transformationService);
        } else if (thingTypeUID.equals(IrccConstants.THING_TYPE_IRCC)) {
            final TransformationService transformationService = TransformationHelper
                    .getTransformationService(getBundleContext(), "MAP");
            return new IrccHandler(thing, transformationService);
        } else if (thingTypeUID.equals(DialConstants.THING_TYPE_DIAL)) {
            return new DialHandler(thing);
        } else if (thingTypeUID.getId().startsWith(SonyBindingConstants.SCALAR_THING_TYPE_PREFIX)) {
            final TransformationService transformationService = TransformationHelper
                    .getTransformationService(getBundleContext(), "MAP");

            final WebSocketClient localWebSocketClient = webSocketClient;
            if (localWebSocketClient == null) {
                throw new IllegalStateException("No websocket client found");
            }

            final SonyDefinitionProvider localSonyDefinitionProvider = sonyDefinitionProvider;
            if (localSonyDefinitionProvider == null) {
                throw new IllegalStateException("No SonyDefinitionProvider found");
            }
            final SonyDynamicStateProvider localSonyDynamicStateProvider = sonyDynamicStateProvider;
            if (localSonyDynamicStateProvider == null) {
                throw new IllegalStateException("No localSonyDynamicStateProvider found");
            }

            return new ScalarWebHandler(thing, transformationService, localWebSocketClient, localSonyDefinitionProvider,
                    localSonyDynamicStateProvider);
        }

        return null;
    }

    @Reference
    protected void setWebSocketFactory(WebSocketFactory webSocketFactory) {
        this.webSocketClient = webSocketFactory.getCommonWebSocketClient();
    }

    protected void unsetWebSocketFactory(WebSocketFactory webSocketFactory) {
        this.webSocketClient = null;
    }

    @Reference
    protected void setSonyDefinitionProvider(SonyDefinitionProvider sonyDefinitionProvider) {
        this.sonyDefinitionProvider = sonyDefinitionProvider;
    }

    protected void unsetSonyDefinitionProvider(SonyDefinitionProvider sonyDefinitionProvider) {
        this.sonyDefinitionProvider = null;
    }

    @Reference
    protected void setSonyDynamicStateProvider(SonyDynamicStateProvider sonyDynamicStateProvider) {
        this.sonyDynamicStateProvider = sonyDynamicStateProvider;
    }

    protected void unsetSonyDynamicStateProvider(SonyDynamicStateProvider sonyDynamicStateProvider) {
        this.sonyDynamicStateProvider = null;
    }
}
