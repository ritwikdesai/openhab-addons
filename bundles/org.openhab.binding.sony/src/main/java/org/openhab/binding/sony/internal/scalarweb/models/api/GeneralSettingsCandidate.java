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
package org.openhab.binding.sony.internal.scalarweb.models.api;

import org.apache.commons.lang.BooleanUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class GeneralSettingsCandidate {
    private final @Nullable Boolean isAvailable;
    private final @Nullable Double max;
    private final @Nullable Double min;
    private final @Nullable Double step;
    private final @Nullable String title;
    private final @Nullable String titleTextID;
    private final @Nullable String value;

    public GeneralSettingsCandidate(@Nullable Boolean isAvailable, @Nullable Double max, @Nullable Double min,
            @Nullable Double step, @Nullable String title, @Nullable String titleTextID, @Nullable String value) {
        this.isAvailable = isAvailable;
        this.max = max;
        this.min = min;
        this.step = step;
        this.title = title;
        this.titleTextID = titleTextID;
        this.value = value;
    }

    public boolean isAvailable() {
        return isAvailable == null || BooleanUtils.isTrue(isAvailable);
    }

    public @Nullable Double getMax() {
        return max;
    }

    public @Nullable Double getMin() {
        return min;
    }

    public @Nullable Double getStep() {
        return step;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public @Nullable String getTitleTextID() {
        return titleTextID;
    }

    public @Nullable String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "GeneralSettingsCandidate [isAvailable=" + isAvailable + ", max=" + max + ", min=" + min + ", step="
                + step + ", title=" + title + ", titleTextID=" + titleTextID + ", value=" + value + "]";
    }
}
