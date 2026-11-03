/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modified for Katari's desktop extension host from the Android Open Source Project's Configuration: the fields and
// constants of the display and UI mode, with no locales, resources qualifiers, parceling or diffing. The host's
// configuration is a normal UI mode, light or dark as the desktop prefers, at the default density.

package android.content.res;

import java.util.Locale;

public final class Configuration implements Comparable<Configuration> {
    public static final int UI_MODE_TYPE_MASK = 0x0f;
    public static final int UI_MODE_TYPE_UNDEFINED = 0x00;
    public static final int UI_MODE_TYPE_NORMAL = 0x01;
    public static final int UI_MODE_TYPE_DESK = 0x02;
    public static final int UI_MODE_TYPE_CAR = 0x03;
    public static final int UI_MODE_TYPE_TELEVISION = 0x04;
    public static final int UI_MODE_TYPE_APPLIANCE = 0x05;
    public static final int UI_MODE_TYPE_WATCH = 0x06;
    public static final int UI_MODE_TYPE_VR_HEADSET = 0x07;
    public static final int UI_MODE_NIGHT_MASK = 0x30;
    public static final int UI_MODE_NIGHT_UNDEFINED = 0x00;
    public static final int UI_MODE_NIGHT_NO = 0x10;
    public static final int UI_MODE_NIGHT_YES = 0x20;

    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;
    /** @deprecated */
    @Deprecated
    public static final int ORIENTATION_SQUARE = 3;

    public static final int DENSITY_DPI_UNDEFINED = 0;
    public static final int SCREEN_WIDTH_DP_UNDEFINED = 0;
    public static final int SCREEN_HEIGHT_DP_UNDEFINED = 0;
    public static final int SMALLEST_SCREEN_WIDTH_DP_UNDEFINED = 0;

    public float fontScale;
    /** @deprecated */
    @Deprecated
    public Locale locale;
    public int orientation;
    public int uiMode;
    public int screenWidthDp;
    public int screenHeightDp;
    public int smallestScreenWidthDp;
    public int densityDpi;

    public Configuration() {
        unset();
    }

    public Configuration(Configuration o) {
        setTo(o);
    }

    public void setTo(Configuration o) {
        fontScale = o.fontScale;
        locale = o.locale;
        orientation = o.orientation;
        uiMode = o.uiMode;
        screenWidthDp = o.screenWidthDp;
        screenHeightDp = o.screenHeightDp;
        smallestScreenWidthDp = o.smallestScreenWidthDp;
        densityDpi = o.densityDpi;
    }

    public void setToDefaults() {
        fontScale = 1;
        locale = null;
        orientation = ORIENTATION_UNDEFINED;
        uiMode = UI_MODE_TYPE_UNDEFINED;
        screenWidthDp = SCREEN_WIDTH_DP_UNDEFINED;
        screenHeightDp = SCREEN_HEIGHT_DP_UNDEFINED;
        smallestScreenWidthDp = SMALLEST_SCREEN_WIDTH_DP_UNDEFINED;
        densityDpi = DENSITY_DPI_UNDEFINED;
    }

    public void unset() {
        setToDefaults();
        fontScale = 0;
    }

    public void setLocale(Locale loc) {
        locale = loc;
    }

    public boolean isNightModeActive() {
        return (uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES;
    }

    @Override
    public int compareTo(Configuration that) {
        int n = Float.compare(fontScale, that.fontScale);
        if (n != 0) return n;
        n = orientation - that.orientation;
        if (n != 0) return n;
        n = uiMode - that.uiMode;
        if (n != 0) return n;
        n = screenWidthDp - that.screenWidthDp;
        if (n != 0) return n;
        n = screenHeightDp - that.screenHeightDp;
        if (n != 0) return n;
        n = smallestScreenWidthDp - that.smallestScreenWidthDp;
        if (n != 0) return n;
        return densityDpi - that.densityDpi;
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Configuration && compareTo((Configuration) that) == 0
                && java.util.Objects.equals(locale, ((Configuration) that).locale);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + Float.floatToIntBits(fontScale);
        result = 31 * result + orientation;
        result = 31 * result + uiMode;
        result = 31 * result + screenWidthDp;
        result = 31 * result + screenHeightDp;
        result = 31 * result + smallestScreenWidthDp;
        result = 31 * result + densityDpi;
        return result;
    }

    @Override
    public String toString() {
        return "{" + fontScale + " " + locale + " orientation=" + orientation + " uiMode=" + uiMode
                + " w" + screenWidthDp + "dp h" + screenHeightDp + "dp sw" + smallestScreenWidthDp + "dp "
                + densityDpi + "dpi}";
    }
}
