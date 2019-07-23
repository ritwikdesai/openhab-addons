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
package org.openhab.binding.sony.internal.transports;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.sony.internal.net.Header;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebEvent;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractSonyTransport implements SonyTransport {

    private final URL baseUrl;

    private final List<SonyTransportListener> listeners = new CopyOnWriteArrayList<>();

    private final List<TransportOption> options = new CopyOnWriteArrayList<>();

    protected AbstractSonyTransport(URL baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public URL getBaseUrl() {
        return baseUrl;
    }

    @Override
    public void addListener(SonyTransportListener listener) {
        listeners.add(listener);
    }

    @Override
    public boolean removeListener(SonyTransportListener listener) {
        return listeners.remove(listener);
    }

    protected void fireOnError(Throwable t) {
        for (SonyTransportListener listener : listeners) {
            listener.onError(t);
        }
    }

    protected void fireEvent(ScalarWebEvent event) {
        for (SonyTransportListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    @Override
    public void setOption(TransportOption option) {
        Objects.requireNonNull(option, "option cannot be null");

        if (option instanceof TransportOptionHeader) {
            final String headerName = ((TransportOptionHeader) option).getHeader().getName();
            options.removeIf(e -> e instanceof TransportOptionHeader
                    && StringUtils.equalsIgnoreCase(headerName, ((TransportOptionHeader) e).getHeader().getName()));
        } else {
            final Class<?> optionClass = option.getClass();
            options.removeIf(e -> optionClass.equals(e.getClass()));
        }
        options.add(option);
    }

    @Override
    public void removeOption(TransportOption option) {
        Objects.requireNonNull(option, "option cannot be null");
        options.remove(option);
    }

    protected List<TransportOption> getOptions() {
        return options;
    }

    @SuppressWarnings("unchecked")
    protected <O> List<O> getOptions(Class<O> clazz, TransportOption... options) {
        Objects.requireNonNull(clazz, "clazz cannot be null");
        return Stream.concat(Arrays.stream(options), this.options.stream())
                .filter(obj -> obj.getClass().isAssignableFrom(clazz)).map(obj -> (O) obj).collect(Collectors.toList());
    }

    protected <O> boolean hasOption(Class<O> clazz, TransportOption... options) {
        return !getOptions(clazz, options).isEmpty();
    }

    protected Header[] getHeaders(TransportOption... options) {
        return getOptions(TransportOptionHeader.class, options).stream().map(h -> h.getHeader())
                .collect(Collectors.toList()).toArray(new Header[0]);
    }
}
