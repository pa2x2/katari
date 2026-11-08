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

// Modified for Katari's desktop extension host from the Android Open Source Project's Resources: the configuration
// and display metrics extensions read, with no resource tables, so every lookup by id fails as for an id the app
// doesn't have. The system's and the application's resources are the same, the host's. Themes have no styles: what
// they would resolve throws "Stub!", as the SDK stub's members do, so diagnostics name it.

package android.content.res;

import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import app.katari.extension.compat.AndroidCompatEnvironment;

public class Resources {
    private static final Object sSync = new Object();
    private static Resources mSystem;

    private final AssetManager mAssets;
    private final DisplayMetrics mMetrics = new DisplayMetrics();
    // Null for the host's, which follows the desktop's light or dark preference
    private final Configuration mConfiguration;

    /** @deprecated */
    @Deprecated
    public Resources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
        mAssets = assets;
        if (metrics != null) {
            mMetrics.setTo(metrics);
        } else {
            mMetrics.setToDefaults();
        }
        mConfiguration = new Configuration();
        if (config != null) {
            mConfiguration.setTo(config);
        } else {
            mConfiguration.setToDefaults();
        }
    }

    private Resources() {
        mAssets = null;
        mMetrics.setToDefaults();
        mConfiguration = null;
    }

    public static Resources getSystem() {
        synchronized (sSync) {
            if (mSystem == null) {
                mSystem = new Resources();
            }
            return mSystem;
        }
    }

    public Configuration getConfiguration() {
        if (mConfiguration != null) {
            return mConfiguration;
        }
        Configuration configuration = new Configuration();
        configuration.setToDefaults();
        configuration.densityDpi = mMetrics.densityDpi;
        boolean night = AndroidCompatEnvironment.current().isNightMode().invoke();
        configuration.uiMode = Configuration.UI_MODE_TYPE_NORMAL
                | (night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return configuration;
    }

    public DisplayMetrics getDisplayMetrics() {
        return mMetrics;
    }

    public final AssetManager getAssets() {
        return mAssets;
    }

    public String getString(int id) throws NotFoundException {
        throw new NotFoundException("String resource ID #0x" + Integer.toHexString(id));
    }

    public CharSequence getText(int id) throws NotFoundException {
        throw new NotFoundException("String resource ID #0x" + Integer.toHexString(id));
    }

    public String getResourceName(int resid) throws NotFoundException {
        throw new NotFoundException("Unable to find resource ID #0x" + Integer.toHexString(resid));
    }

    public CharSequence[] getTextArray(int id) throws NotFoundException {
        throw new NotFoundException("Text array resource ID #0x" + Integer.toHexString(id));
    }

    public int getColor(int id, Theme theme) throws NotFoundException {
        throw new NotFoundException("Color resource ID #0x" + Integer.toHexString(id));
    }

    public ColorStateList getColorStateList(int id, Theme theme) throws NotFoundException {
        throw new NotFoundException("Color state list resource ID #0x" + Integer.toHexString(id));
    }

    public Drawable getDrawable(int id, Theme theme) throws NotFoundException {
        throw new NotFoundException("Drawable resource ID #0x" + Integer.toHexString(id));
    }

    public int getIdentifier(String name, String defType, String defPackage) {
        return 0;
    }

    public final Theme newTheme() {
        return new Theme();
    }

    public final class Theme {
        Theme() {
        }

        public Resources getResources() {
            return Resources.this;
        }

        public TypedArray obtainStyledAttributes(int[] attrs) {
            throw new RuntimeException("Stub!");
        }

        public TypedArray obtainStyledAttributes(int resId, int[] attrs) throws NotFoundException {
            throw new RuntimeException("Stub!");
        }

        public TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
            throw new RuntimeException("Stub!");
        }
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }

        public NotFoundException(String name, Exception cause) {
            super(name, cause);
        }
    }
}
