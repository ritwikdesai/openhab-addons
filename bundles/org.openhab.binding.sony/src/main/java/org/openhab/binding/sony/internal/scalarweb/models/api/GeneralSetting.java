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

import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public class GeneralSetting {
    public static final String BOOLEANTARGET = "booleanTarget";
    public static final String DOUBLETARGET = "doubleNumberTarget";
    public static final String ENUMTARGET = "enumTarget";
    public static final String INTEGERTARGET = "integerTarget";
    public static final String STRINGTARGET = "stringTarget";

    public static final String ON = "on";
    public static final String OFF = "off";

    public static final String SLIDER = "slider";
    public static final String PICKER = "picker";

    private final @Nullable Boolean isAvailable;
    private final @Nullable String currentValue;
    private final @Nullable String target;
    private final @Nullable String title;
    private final @Nullable String titleTextID;
    private final @Nullable String type;
    private final @Nullable String deviceUIInfo;
    private final @Nullable List<@Nullable GeneralSettingsCandidate> candidate;

    public GeneralSetting(@Nullable Boolean isAvailable, @Nullable String currentValue, @Nullable String target,
            @Nullable String title, @Nullable String titleTextID, @Nullable String type, @Nullable String deviceUIInfo,
            @Nullable List<@Nullable GeneralSettingsCandidate> candidate) {
        this.isAvailable = isAvailable;
        this.currentValue = currentValue;
        this.target = target;
        this.title = title;
        this.titleTextID = titleTextID;
        this.type = type;
        this.deviceUIInfo = deviceUIInfo;
        this.candidate = candidate;
    }

    public boolean isAvailable() {
        return isAvailable == null || BooleanUtils.isTrue(isAvailable);
    }

    public @Nullable String getCurrentValue() {
        return currentValue;
    }

    public @Nullable String getTarget() {
        return target;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public @Nullable String getTitleTextID() {
        return titleTextID;
    }

    public @Nullable String getType() {
        return type;
    }

    public @Nullable String getDeviceUIInfo() {
        return deviceUIInfo;
    }

    public boolean isUiSlider() {
        return StringUtils.contains(deviceUIInfo, SLIDER);
    }

    public boolean isUiPicker() {
        return StringUtils.contains(deviceUIInfo, PICKER);
    }

    public @Nullable List<@Nullable GeneralSettingsCandidate> getCandidate() {
        return candidate;
    }

    @Override
    public String toString() {
        return "SoundSetting [isAvailable=" + isAvailable + ", currentValue=" + currentValue + ", target=" + target
                + ", title=" + title + ", titleTextID=" + titleTextID + ", type=" + type + ", deviceUIInfo="
                + deviceUIInfo + ", candidate=" + candidate + "]";
    }

}
