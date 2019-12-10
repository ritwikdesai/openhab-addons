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
package org.openhab.binding.sony.internal.providers.models;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.types.StateDescription;
import org.openhab.binding.sony.internal.SonyMatcher;
import org.openhab.binding.sony.internal.SonyUtil;

/**
 * Defines a thing state definition. This class will be used to serialize any state description from the underlying
 * thing
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class SonyThingStateDefinition implements SonyMatcher {
    /** The stepping */
    private @Nullable BigDecimal step;

    /** The minimum */
    private @Nullable BigDecimal minimum;

    /** The maximum */
    private @Nullable BigDecimal maximum;

    /** Any pattern to apply */
    private @Nullable String pattern;

    /** Whether it is readonly or not */
    private @Nullable Boolean readonly;

    /** The channel options */
    private @Nullable Map<@Nullable String, @Nullable String> options;

    /**
     * Constructs the thing state definition from the state description
     *
     * @param desc a possibly null state description
     */
    public SonyThingStateDefinition(@Nullable StateDescription desc) {
        this.maximum = desc == null ? null : desc.getMaximum();
        this.minimum = desc == null ? null : desc.getMinimum();
        this.step = desc == null ? null : desc.getStep();
        this.pattern = desc == null ? null : desc.getPattern();
        this.readonly = desc == null ? null : desc.isReadOnly();
        this.options = desc == null ? null
                : desc.getOptions().stream().collect(Collectors.toMap(k -> k.getValue(), v -> v.getLabel()));
    }

    @Override
    public boolean matches(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof SonyThingStateDefinition)) {
            return false;
        }

        final SonyThingStateDefinition other = (SonyThingStateDefinition) obj;

        return ObjectUtils.equals(maximum, other.maximum) && ObjectUtils.equals(minimum, other.minimum)
                && ObjectUtils.equals(step, other.step) && ObjectUtils.equals(readonly, other.readonly)
                && SonyUtil.equalsIgnoreCase(options, other.options)
                && StringUtils.equalsIgnoreCase(pattern, other.pattern);
    }

    @Override
    public String toString() {
        return "SonyThingStateDefinition [maximum=" + maximum + ", minimum=" + minimum + ", step=" + step + ", pattern="
                + pattern + ", readonly=" + readonly + ", options=" + options + "]";
    }
}
