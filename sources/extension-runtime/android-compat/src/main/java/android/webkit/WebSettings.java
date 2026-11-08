/*
 * Copyright (C) 2007 The Android Open Source Project
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

// Modified for Katari's desktop extension host from the Android Open Source Project's WebSettings: the SDK's members
// without their documentation and deprecation marks, which are for apps and which AOSP's has, and without hidden ones.
// Each web view's settings are app.katari.extension.compat.webkit.PageWebSettings, which keeps the deprecated ones too, and
// the default user agent is the one the host's pages send where an extension sets none.

package android.webkit;

import android.content.Context;

import app.katari.extension.compat.webkit.PageWebSettings;

public abstract class WebSettings {
    public enum LayoutAlgorithm {
        NORMAL,
        SINGLE_COLUMN,
        NARROW_COLUMNS,
        TEXT_AUTOSIZING
    }
    public enum TextSize {
        SMALLEST(50),
        SMALLER(75),
        NORMAL(100),
        LARGER(150),
        LARGEST(200);

        TextSize(int size) {
            value = size;
        }

        int value;
    }

    public enum ZoomDensity {
        FAR(150),
        MEDIUM(100),
        CLOSE(75);

        ZoomDensity(int size) {
            value = size;
        }

        public int getValue() {
            return value;
        }

        int value;
    }

    public static final int LOAD_DEFAULT = -1;
    public static final int LOAD_NORMAL = 0;
    public static final int LOAD_CACHE_ELSE_NETWORK = 1;
    public static final int LOAD_NO_CACHE = 2;
    public static final int LOAD_CACHE_ONLY = 3;

    public enum RenderPriority {
        NORMAL,
        HIGH,
        LOW
    }
    public enum PluginState {
        ON,
        ON_DEMAND,
        OFF
    }

    public static final int MIXED_CONTENT_ALWAYS_ALLOW = 0;
    public static final int MIXED_CONTENT_NEVER_ALLOW = 1;
    public static final int MIXED_CONTENT_COMPATIBILITY_MODE = 2;
    public static final int FORCE_DARK_OFF = 0;
    public static final int FORCE_DARK_AUTO = 1;
    public static final int FORCE_DARK_ON = 2;

    public static final int MENU_ITEM_NONE = 0;
    public static final int MENU_ITEM_SHARE = 1 << 0;
    public static final int MENU_ITEM_WEB_SEARCH = 1 << 1;
    public static final int MENU_ITEM_PROCESS_TEXT = 1 << 2;

    public WebSettings() {
    }
    public synchronized void setTextSize(TextSize t) {
        setTextZoom(t.value);
    }
    public synchronized TextSize getTextSize() {
        TextSize closestSize = null;
        int smallestDelta = Integer.MAX_VALUE;
        int textSize = getTextZoom();
        for (TextSize size : TextSize.values()) {
            int delta = Math.abs(textSize - size.value);
            if (delta == 0) {
                return size;
            }
            if (delta < smallestDelta) {
                smallestDelta = delta;
                closestSize = size;
            }
        }
        return closestSize != null ? closestSize : TextSize.NORMAL;
    }

    public static String getDefaultUserAgent(Context context) {
        return PageWebSettings.defaultUserAgent();
    }
    public void setForceDark(int forceDark) {
    }
    public int getForceDark() {
        return FORCE_DARK_AUTO;
    }

    public void setAlgorithmicDarkeningAllowed(boolean allow) {
    }

    public boolean isAlgorithmicDarkeningAllowed() {
        return false;
    }

    public abstract boolean enableSmoothTransition();
    public abstract boolean getAllowContentAccess();
    public abstract boolean getAllowFileAccess();
    public abstract boolean getAllowFileAccessFromFileURLs();
    public abstract boolean getAllowUniversalAccessFromFileURLs();
    public abstract boolean getBlockNetworkImage();
    public abstract boolean getBlockNetworkLoads();
    public abstract boolean getBuiltInZoomControls();
    public abstract int getCacheMode();
    public abstract String getCursiveFontFamily();
    public abstract boolean getDatabaseEnabled();
    public abstract String getDatabasePath();
    public abstract int getDefaultFixedFontSize();
    public abstract int getDefaultFontSize();
    public abstract String getDefaultTextEncodingName();
    public abstract ZoomDensity getDefaultZoom();
    public abstract int getDisabledActionModeMenuItems();
    public abstract boolean getDisplayZoomControls();
    public abstract boolean getDomStorageEnabled();
    public abstract String getFantasyFontFamily();
    public abstract String getFixedFontFamily();
    public abstract boolean getJavaScriptCanOpenWindowsAutomatically();
    public abstract boolean getJavaScriptEnabled();
    public abstract LayoutAlgorithm getLayoutAlgorithm();
    public abstract boolean getLightTouchEnabled();
    public abstract boolean getLoadWithOverviewMode();
    public abstract boolean getLoadsImagesAutomatically();
    public abstract boolean getMediaPlaybackRequiresUserGesture();
    public abstract int getMinimumFontSize();
    public abstract int getMinimumLogicalFontSize();
    public abstract int getMixedContentMode();
    public abstract boolean getOffscreenPreRaster();
    public abstract PluginState getPluginState();
    public abstract boolean getSafeBrowsingEnabled();
    public abstract String getSansSerifFontFamily();
    public abstract boolean getSaveFormData();
    public abstract boolean getSavePassword();
    public abstract String getSerifFontFamily();
    public abstract String getStandardFontFamily();
    public abstract int getTextZoom();
    public abstract boolean getUseWideViewPort();
    public abstract String getUserAgentString();
    public abstract void setAllowContentAccess(boolean flag);
    public abstract void setAllowFileAccess(boolean flag);
    public abstract void setAllowFileAccessFromFileURLs(boolean flag);
    public abstract void setAllowUniversalAccessFromFileURLs(boolean flag);
    public abstract void setBlockNetworkImage(boolean flag);
    public abstract void setBlockNetworkLoads(boolean flag);
    public abstract void setBuiltInZoomControls(boolean flag);
    public abstract void setCacheMode(int value);
    public abstract void setCursiveFontFamily(String value);
    public abstract void setDatabaseEnabled(boolean flag);
    public abstract void setDatabasePath(String value);
    public abstract void setDefaultFixedFontSize(int value);
    public abstract void setDefaultFontSize(int value);
    public abstract void setDefaultTextEncodingName(String value);
    public abstract void setDefaultZoom(ZoomDensity value);
    public abstract void setDisabledActionModeMenuItems(int value);
    public abstract void setDisplayZoomControls(boolean flag);
    public abstract void setDomStorageEnabled(boolean flag);
    public abstract void setEnableSmoothTransition(boolean flag);
    public abstract void setFantasyFontFamily(String value);
    public abstract void setFixedFontFamily(String value);
    public abstract void setGeolocationDatabasePath(String value);
    public abstract void setGeolocationEnabled(boolean flag);
    public abstract void setJavaScriptCanOpenWindowsAutomatically(boolean flag);
    public abstract void setJavaScriptEnabled(boolean flag);
    public abstract void setLayoutAlgorithm(LayoutAlgorithm value);
    public abstract void setLightTouchEnabled(boolean flag);
    public abstract void setLoadWithOverviewMode(boolean flag);
    public abstract void setLoadsImagesAutomatically(boolean flag);
    public abstract void setMediaPlaybackRequiresUserGesture(boolean flag);
    public abstract void setMinimumFontSize(int value);
    public abstract void setMinimumLogicalFontSize(int value);
    public abstract void setMixedContentMode(int value);
    public abstract void setNeedInitialFocus(boolean flag);
    public abstract void setOffscreenPreRaster(boolean flag);
    public abstract void setPluginState(PluginState value);
    public abstract void setRenderPriority(RenderPriority value);
    public abstract void setSafeBrowsingEnabled(boolean flag);
    public abstract void setSansSerifFontFamily(String value);
    public abstract void setSaveFormData(boolean flag);
    public abstract void setSavePassword(boolean flag);
    public abstract void setSerifFontFamily(String value);
    public abstract void setStandardFontFamily(String value);
    public abstract void setSupportMultipleWindows(boolean flag);
    public abstract void setSupportZoom(boolean flag);
    public abstract void setTextZoom(int value);
    public abstract void setUseWideViewPort(boolean flag);
    public abstract void setUserAgentString(String value);
    public abstract boolean supportMultipleWindows();
    public abstract boolean supportZoom();
}
