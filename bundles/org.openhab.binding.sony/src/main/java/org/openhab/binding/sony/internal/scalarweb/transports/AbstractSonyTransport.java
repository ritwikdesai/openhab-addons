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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebEvent;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractSonyTransport<T> implements SonyTransport<T> {

    private final List<SonyTransportListener> listeners = new CopyOnWriteArrayList<>();

    private final ConcurrentMap<String, @Nullable List<Object>> options = new ConcurrentHashMap<>();

    @Override
    public void addOption(String optionName, Object option) {
        Validate.notEmpty(optionName, "optionName cannot be empty");
        Objects.requireNonNull(option, "option cannot be null");

        options.compute(optionName, (k, v) -> {
            List<Object> val = v;
            if (val == null) {
                val = new CopyOnWriteArrayList<>();
            }
            val.add(option);
            return val;
        });
    }

    @Override
    public void removeOption(String optionName, Object option) {
        Validate.notEmpty(optionName, "optionName cannot be empty");
        Objects.requireNonNull(option, "option cannot be null");
        options.computeIfPresent(optionName, (k, v) -> {
            if (v != null) {
                v.remove(option);
            }
            return v;
        });
    }

    protected <O> List<O> getOption(String optionName, Class<O> clazz) {
        Validate.notEmpty(optionName, "optionName cannot be empty");
        Objects.requireNonNull(clazz, "clazz cannot be null");

        final List<Object> objs = options.get(optionName);
        final List<O> rtn = new ArrayList<>();
        if (objs != null) {
            for (Object obj : objs) {
                if (obj.getClass().isAssignableFrom(clazz)) {
                    rtn.add((O) obj);
                }
            }
        }
        return rtn;
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
}
