/*
 * Copyright (C) 2006 The Android Open Source Project
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

// Modified for Katari's desktop extension host from the Android Open Source Project's DisplayMetrics: the fields and
// densities, set to what AOSP's setToDefaults leaves when the device reports no density.

package android.util;

public class DisplayMetrics {
    public static final int DENSITY_LOW = 120;
    public static final int DENSITY_140 = 140;
    public static final int DENSITY_MEDIUM = 160;
    public static final int DENSITY_180 = 180;
    public static final int DENSITY_200 = 200;
    public static final int DENSITY_TV = 213;
    public static final int DENSITY_220 = 220;
    public static final int DENSITY_HIGH = 240;
    public static final int DENSITY_260 = 260;
    public static final int DENSITY_280 = 280;
    public static final int DENSITY_300 = 300;
    public static final int DENSITY_XHIGH = 320;
    public static final int DENSITY_340 = 340;
    public static final int DENSITY_360 = 360;
    public static final int DENSITY_400 = 400;
    public static final int DENSITY_420 = 420;
    public static final int DENSITY_440 = 440;
    public static final int DENSITY_450 = 450;
    public static final int DENSITY_XXHIGH = 480;
    public static final int DENSITY_520 = 520;
    public static final int DENSITY_560 = 560;
    public static final int DENSITY_600 = 600;
    public static final int DENSITY_XXXHIGH = 640;
    public static final int DENSITY_DEFAULT = DENSITY_MEDIUM;
    public static final int DENSITY_DEVICE_STABLE = DENSITY_DEFAULT;

    public int widthPixels;
    public int heightPixels;
    public float density;
    public int densityDpi;
    /** @deprecated */
    @Deprecated
    public float scaledDensity;
    public float xdpi;
    public float ydpi;

    public DisplayMetrics() {
    }

    public void setTo(DisplayMetrics o) {
        if (this == o) {
            return;
        }
        widthPixels = o.widthPixels;
        heightPixels = o.heightPixels;
        density = o.density;
        densityDpi = o.densityDpi;
        scaledDensity = o.scaledDensity;
        xdpi = o.xdpi;
        ydpi = o.ydpi;
    }

    public void setToDefaults() {
        widthPixels = 0;
        heightPixels = 0;
        density = DENSITY_DEFAULT / (float) DENSITY_DEFAULT;
        densityDpi = DENSITY_DEFAULT;
        scaledDensity = density;
        xdpi = DENSITY_DEFAULT;
        ydpi = DENSITY_DEFAULT;
    }

    public boolean equals(Object o) {
        return o instanceof DisplayMetrics && equals((DisplayMetrics) o);
    }

    public boolean equals(DisplayMetrics other) {
        return other != null
                && widthPixels == other.widthPixels
                && heightPixels == other.heightPixels
                && density == other.density
                && densityDpi == other.densityDpi
                && scaledDensity == other.scaledDensity
                && xdpi == other.xdpi
                && ydpi == other.ydpi;
    }

    @Override
    public int hashCode() {
        return widthPixels * heightPixels * densityDpi;
    }

    @Override
    public String toString() {
        return "DisplayMetrics{density=" + density + ", width=" + widthPixels + ", height=" + heightPixels
                + ", scaledDensity=" + scaledDensity + ", xdpi=" + xdpi + ", ydpi=" + ydpi + "}";
    }
}
