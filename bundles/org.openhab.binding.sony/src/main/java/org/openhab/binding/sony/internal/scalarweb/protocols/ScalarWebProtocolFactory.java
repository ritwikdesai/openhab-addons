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
/*
 *
 */
package org.openhab.binding.sony.internal.scalarweb.protocols;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.ThingCallback;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebChannelDescriptor;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebClient;
import org.openhab.binding.sony.internal.scalarweb.ScalarWebContext;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for creating ScalarWebProtocol objects.
 *
 * @author Tim Roberts - Initial contribution
 * @param <T> the generic type of callback
 */
@NonNullByDefault
public class ScalarWebProtocolFactory<T extends ThingCallback<String>> implements AutoCloseable {

    /** The logger */
    private Logger logger = LoggerFactory.getLogger(ScalarWebProtocolFactory.class);

    /** The protocols by service name */
    private final Map<String, ScalarWebProtocol<T>> protocols = new HashMap<>();

    /**
     * Instantiates a new scalar web protocol factory.
     *
     * @param context  the non-null context
     * @param client   the non-null state
     * @param callback the non-null callback
     */
    public ScalarWebProtocolFactory(ScalarWebContext context, ScalarWebClient client, T callback) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");

        for (ScalarWebService service : client.getDevice().getServices()) {
            final String serviceName = service.getServiceName();
            switch (serviceName) {
                case ScalarWebService.APPCONTROL:
                    protocols.put(ScalarWebService.APPCONTROL,
                            new ScalarWebAppControlProtocol<T>(this, context, service, callback));
                    break;
                case ScalarWebService.AUDIO:
                    protocols.put(ScalarWebService.AUDIO,
                            new ScalarWebAudioProtocol<T>(this, context, service, callback));
                    break;

                case ScalarWebService.AVCONTENT:
                    protocols.put(ScalarWebService.AVCONTENT,
                            new ScalarWebAvContentProtocol<T>(this, context, service, callback));
                    break;

                case ScalarWebService.BROWSER:
                    protocols.put(ScalarWebService.BROWSER,
                            new ScalarWebBrowserProtocol<T>(this, context, service, callback));
                    break;

                case ScalarWebService.CEC:
                    protocols.put(ScalarWebService.CEC, new ScalarWebCecProtocol<T>(this, context, service, callback));
                    break;

                case ScalarWebService.SYSTEM:
                    protocols.put(ScalarWebService.SYSTEM, new ScalarWebSystemProtocol<T>(this, context, service,
                            callback, context.getConfig().getIrccUrl()));
                    break;

                case ScalarWebService.VIDEOSCREEN:
                    protocols.put(ScalarWebService.VIDEOSCREEN,
                            new ScalarWebVideoScreenProtocol<T>(this, context, service, callback));
                    break;

                default:
                    logger.debug("No protocol found for service {}", serviceName);
                    break;
            }
        }
    }

    /**
     * Gets the protocol for the given name
     *
     * @param name the service name
     * @return the protocol or null if not found
     */
    public @Nullable ScalarWebProtocol<T> getProtocol(@Nullable String name) {
        if (name == null || StringUtils.isEmpty(name)) {
            return null;
        }
        return protocols.get(name);
    }

    /**
     * Gets the channel descriptors for all protocols
     *
     * @return the non-null, possibly empty channel descriptors
     */
    public Collection<ScalarWebChannelDescriptor> getChannelDescriptors() {
        return getChannelDescriptors(null);
    }

    /**
     * Gets the channel descriptors for all or a specific protocol
     *
     * @param service a possibly null, possibly empty service name (null/empty service means all services)
     * @return the non-null, possibly empty channel descriptors
     */
    public Collection<ScalarWebChannelDescriptor> getChannelDescriptors(@Nullable String service) {
        final List<ScalarWebChannelDescriptor> descriptors = new ArrayList<ScalarWebChannelDescriptor>();

        if (service == null || StringUtils.isEmpty(service)) {
            for (ScalarWebProtocol<T> protocol : protocols.values()) {
                logger.debug("Getting channel descriptors for {}", protocol.getClass().getName());
                descriptors.addAll(protocol.getChannelDescriptors());
            }
        } else {
            final ScalarWebProtocol<T> protocol = protocols.get(service);
            if (protocol == null) {
                logger.debug("Unknown service/protocol: {}", service);
            } else {
                descriptors.addAll(protocol.getChannelDescriptors());
            }

        }

        return descriptors;
    }

    /**
     * Refresh all state in all services
     *
     * @param scheduler the non-null scheduler to schedule refresh on each service
     */
    public void refreshAllState(ScheduledExecutorService scheduler) {
        Objects.requireNonNull(scheduler, "scheduler cannot be null");
        for (final ScalarWebProtocol<T> protocol : protocols.values()) {
            scheduler.execute(() -> {
                protocol.refreshState();
            });
        }

    }

    @Override
    public void close() {
        for (ScalarWebProtocol<T> protocol : protocols.values()) {
            protocol.close();
        }
    }
}
