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

// Modified for Katari's desktop extension host from the Android Open Source Project's WebView: the SDK's own members
// without their documentation, which AOSP's has. What extensions use runs on a hidden page of the host's
// (mihon.extension.compat.webkit.WebViewPage, which says how calls and callbacks are threaded); the members the host
// doesn't offer throw as the SDK's stub does. The view itself is never drawn, so the members it inherits as a view
// work as on any view in no window; a Dialog whose content it is shows its page in the app's window.

package android.webkit;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.print.PrintDocumentAdapter;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.textclassifier.TextClassifier;
import android.widget.AbsoluteLayout;

import mihon.core.common.web.ExtensionPage;
import mihon.extension.compat.webkit.WebViewPage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class WebView extends AbsoluteLayout
        implements ViewTreeObserver.OnGlobalFocusChangeListener,
        ViewGroup.OnHierarchyChangeListener {

    public static final int RENDERER_PRIORITY_WAIVED = 0;
    public static final int RENDERER_PRIORITY_BOUND = 1;
    public static final int RENDERER_PRIORITY_IMPORTANT = 2;

    public static final String SCHEME_TEL = "tel:";
    public static final String SCHEME_MAILTO = "mailto:";
    public static final String SCHEME_GEO = "geo:0,0?q=";

    public class WebViewTransport {
        private WebView mWebview;

        public synchronized void setWebView(@Nullable WebView webview) {
            mWebview = webview;
        }

        @Nullable
        public synchronized WebView getWebView() {
            return mWebview;
        }
    }

    public interface FindListener {
        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches,
                boolean isDoneCounting);
    }

    public static abstract class VisualStateCallback {
        public abstract void onComplete(long requestId);
    }

    @Deprecated
    public interface PictureListener {
        @Deprecated
        void onNewPicture(WebView view, @Nullable Picture picture);
    }

    public static class HitTestResult {
        public static final int UNKNOWN_TYPE = 0;
        @Deprecated
        public static final int ANCHOR_TYPE = 1;
        public static final int PHONE_TYPE = 2;
        public static final int GEO_TYPE = 3;
        public static final int EMAIL_TYPE = 4;
        public static final int IMAGE_TYPE = 5;
        @Deprecated
        public static final int IMAGE_ANCHOR_TYPE = 6;
        public static final int SRC_ANCHOR_TYPE = 7;
        public static final int SRC_IMAGE_ANCHOR_TYPE = 8;
        public static final int EDIT_TEXT_TYPE = 9;

        private int mType;
        private String mExtra;

        @SystemApi
        public HitTestResult() {
            mType = UNKNOWN_TYPE;
        }

        @SystemApi
        public void setType(int type) {
            mType = type;
        }

        @SystemApi
        public void setExtra(String extra) {
            mExtra = extra;
        }

        public int getType() {
            return mType;
        }

        @Nullable
        public String getExtra() {
            return mExtra;
        }
    }

    private final WebViewPage mPage;

    public WebView(@NonNull Context context) {
        this(context, null);
    }

    public WebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (context == null) {
            throw new IllegalArgumentException("Invalid context argument");
        }
        mPage = WebViewPage.open(this);
    }

    @Deprecated
    public WebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            boolean privateBrowsing) {
        this(context, attrs, defStyleAttr, 0);
    }

    // Supported

    @NonNull
    public WebSettings getSettings() {
        return mPage.getSettings();
    }

    public void setWebViewClient(@NonNull WebViewClient client) {
        mPage.setWebViewClient(client);
    }

    @NonNull
    public WebViewClient getWebViewClient() {
        return mPage.getWebViewClient();
    }

    public void setWebChromeClient(@Nullable WebChromeClient client) {
        mPage.setWebChromeClient(client);
    }

    @Nullable
    public WebChromeClient getWebChromeClient() {
        return mPage.getWebChromeClient();
    }

    public void loadUrl(@NonNull String url, @NonNull Map<String, String> additionalHttpHeaders) {
        mPage.loadUrl(url, additionalHttpHeaders);
    }

    public void loadUrl(@NonNull String url) {
        mPage.loadUrl(url, null);
    }

    public void loadData(@NonNull String data, @Nullable String mimeType, @Nullable String encoding) {
        mPage.loadData(data, mimeType, encoding);
    }

    public void loadDataWithBaseURL(@Nullable String baseUrl, @NonNull String data,
            @Nullable String mimeType, @Nullable String encoding, @Nullable String historyUrl) {
        mPage.loadDataWithBaseUrl(baseUrl, data, mimeType, encoding, historyUrl);
    }

    public void evaluateJavascript(@NonNull String script,
            @Nullable ValueCallback<String> resultCallback) {
        mPage.evaluateJavascript(script, resultCallback);
    }

    public void addJavascriptInterface(@NonNull Object object, @NonNull String name) {
        mPage.addJavascriptInterface(object, name);
    }

    public void removeJavascriptInterface(@NonNull String name) {
        mPage.removeJavascriptInterface(name);
    }

    public void stopLoading() {
        mPage.stopLoading();
    }

    public void reload() {
        mPage.reload();
    }

    public void destroy() {
        mPage.destroy();
    }

    @Nullable
    public String getUrl() {
        return mPage.getUrl();
    }

    @Nullable
    public String getOriginalUrl() {
        return mPage.getOriginalUrl();
    }

    @Nullable
    public String getTitle() {
        return mPage.getTitle();
    }

    public int getProgress() {
        return mPage.getProgress();
    }

    @NonNull
    public Looper getWebViewLooper() {
        return Looper.getMainLooper();
    }

    // Where a dialog shows the page, it is drawn on the web's canvas as every page the app shows, whatever the colour
    @Override
    public void setBackgroundColor(int color) {
    }

    /** Katari's: the host's page behind the view, which android.app.Dialog shows where the view is its content. */
    @NonNull
    public ExtensionPage getHostPage() {
        return mPage.getHostPage();
    }

    @NonNull
    public static ClassLoader getWebViewClassLoader() {
        // The host's web view has no implementation of its own for the WebView support library to reach
        return WebView.class.getClassLoader();
    }

    @Deprecated
    @Override
    public void onChildViewAdded(View parent, View child) {
    }

    @Deprecated
    @Override
    public void onChildViewRemoved(View p, View child) {
    }

    @Deprecated
    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
    }

    // Not offered

    @Deprecated
    public void setHorizontalScrollbarOverlay(boolean overlay) {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public void setVerticalScrollbarOverlay(boolean overlay) {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public boolean overlayHorizontalScrollbar() {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public boolean overlayVerticalScrollbar() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public SslCertificate getCertificate() {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public void setCertificate(SslCertificate certificate) {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public void savePassword(String host, String username, String password) {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public void setHttpAuthUsernamePassword(String host, String realm, String username,
            String password) {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    @Nullable
    public String[] getHttpAuthUsernamePassword(String host, String realm) {
        throw new RuntimeException("Stub!");
    }

    public void setNetworkAvailable(boolean networkUp) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public WebBackForwardList saveState(@NonNull Bundle outState) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public WebBackForwardList restoreState(@NonNull Bundle inState) {
        throw new RuntimeException("Stub!");
    }

    public void postUrl(@NonNull String url, @NonNull byte[] postData) {
        throw new RuntimeException("Stub!");
    }

    public void saveWebArchive(@NonNull String filename) {
        throw new RuntimeException("Stub!");
    }

    public void saveWebArchive(@NonNull String basename, boolean autoname,
            @Nullable ValueCallback<String> callback) {
        throw new RuntimeException("Stub!");
    }

    public boolean canGoBack() {
        throw new RuntimeException("Stub!");
    }

    public void goBack() {
        throw new RuntimeException("Stub!");
    }

    public boolean canGoForward() {
        throw new RuntimeException("Stub!");
    }

    public void goForward() {
        throw new RuntimeException("Stub!");
    }

    public boolean canGoBackOrForward(int steps) {
        throw new RuntimeException("Stub!");
    }

    public void goBackOrForward(int steps) {
        throw new RuntimeException("Stub!");
    }

    public boolean isPrivateBrowsingEnabled() {
        throw new RuntimeException("Stub!");
    }

    public boolean pageUp(boolean top) {
        throw new RuntimeException("Stub!");
    }

    public boolean pageDown(boolean bottom) {
        throw new RuntimeException("Stub!");
    }

    public void postVisualStateCallback(long requestId, @NonNull VisualStateCallback callback) {
        mPage.postVisualStateCallback(requestId, callback);
    }

    @Deprecated
    public void clearView() {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public Picture capturePicture() {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public PrintDocumentAdapter createPrintDocumentAdapter() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public PrintDocumentAdapter createPrintDocumentAdapter(@NonNull String documentName) {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public float getScale() {
        throw new RuntimeException("Stub!");
    }

    public void setInitialScale(int scaleInPercent) {
        throw new RuntimeException("Stub!");
    }

    public void invokeZoomPicker() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public HitTestResult getHitTestResult() {
        throw new RuntimeException("Stub!");
    }

    public void requestFocusNodeHref(@Nullable Message hrefMsg) {
        throw new RuntimeException("Stub!");
    }

    public void requestImageRef(@NonNull Message msg) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public Bitmap getFavicon() {
        throw new RuntimeException("Stub!");
    }

    public int getContentHeight() {
        throw new RuntimeException("Stub!");
    }

    public void pauseTimers() {
        throw new RuntimeException("Stub!");
    }

    public void resumeTimers() {
        throw new RuntimeException("Stub!");
    }

    public void onPause() {
        throw new RuntimeException("Stub!");
    }

    public void onResume() {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public void freeMemory() {
        throw new RuntimeException("Stub!");
    }

    public void clearCache(boolean includeDiskFiles) {
        throw new RuntimeException("Stub!");
    }

    public void clearFormData() {
        throw new RuntimeException("Stub!");
    }

    public void clearHistory() {
        throw new RuntimeException("Stub!");
    }

    public void clearSslPreferences() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public WebBackForwardList copyBackForwardList() {
        throw new RuntimeException("Stub!");
    }

    public void setFindListener(@Nullable FindListener listener) {
        throw new RuntimeException("Stub!");
    }

    public void findNext(boolean forward) {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public int findAll(String find) {
        throw new RuntimeException("Stub!");
    }

    public void findAllAsync(@NonNull String find) {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public boolean showFindDialog(@Nullable String text, boolean showIme) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    @Deprecated
    public static String findAddress(String addr) {
        throw new RuntimeException("Stub!");
    }

    public static void enableSlowWholeDocumentDraw() {
        throw new RuntimeException("Stub!");
    }

    public void clearMatches() {
        throw new RuntimeException("Stub!");
    }

    public void documentHasImages(@NonNull Message response) {
        throw new RuntimeException("Stub!");
    }

    public void setDownloadListener(@Nullable DownloadListener listener) {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public void setPictureListener(PictureListener listener) {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public WebMessagePort[] createWebMessageChannel() {
        throw new RuntimeException("Stub!");
    }

    public void postWebMessage(@NonNull WebMessage message, @NonNull Uri targetOrigin) {
        throw new RuntimeException("Stub!");
    }

    public static void setWebContentsDebuggingEnabled(boolean enabled) {
        throw new RuntimeException("Stub!");
    }

    public static void setDataDirectorySuffix(@NonNull String suffix) {
        throw new RuntimeException("Stub!");
    }

    public static void disableWebView() {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public void setMapTrackballToArrowKeys(boolean setMap) {
        throw new RuntimeException("Stub!");
    }

    public void flingScroll(int vx, int vy) {
        throw new RuntimeException("Stub!");
    }

    public boolean canZoomIn() {
        throw new RuntimeException("Stub!");
    }

    public boolean canZoomOut() {
        throw new RuntimeException("Stub!");
    }

    public void zoomBy(float zoomFactor) {
        throw new RuntimeException("Stub!");
    }

    public boolean zoomIn() {
        throw new RuntimeException("Stub!");
    }

    public boolean zoomOut() {
        throw new RuntimeException("Stub!");
    }

    public void setRendererPriorityPolicy(int rendererRequestedPriority,
            boolean waivedWhenNotVisible) {
        throw new RuntimeException("Stub!");
    }

    public int getRendererRequestedPriority() {
        throw new RuntimeException("Stub!");
    }

    public boolean getRendererPriorityWaivedWhenNotVisible() {
        throw new RuntimeException("Stub!");
    }

    public void setTextClassifier(@Nullable TextClassifier textClassifier) {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public TextClassifier getTextClassifier() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public WebViewRenderProcess getWebViewRenderProcess() {
        throw new RuntimeException("Stub!");
    }

    public void setWebViewRenderProcessClient(@NonNull Executor executor,
            @NonNull WebViewRenderProcessClient webViewRenderProcessClient) {
        throw new RuntimeException("Stub!");
    }

    public void setWebViewRenderProcessClient(
            @Nullable WebViewRenderProcessClient webViewRenderProcessClient) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public WebViewRenderProcessClient getWebViewRenderProcessClient() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public static PackageInfo getCurrentWebViewPackage() {
        throw new RuntimeException("Stub!");
    }

    public static void startSafeBrowsing(@NonNull Context context,
            @Nullable ValueCallback<Boolean> callback) {
        throw new RuntimeException("Stub!");
    }

    public static void setSafeBrowsingWhitelist(@NonNull List<String> hosts,
            @Nullable ValueCallback<Boolean> callback) {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public static Uri getSafeBrowsingPrivacyPolicyUrl() {
        throw new RuntimeException("Stub!");
    }

    public static void clearClientCertPreferences(@Nullable Runnable onCleared) {
        throw new RuntimeException("Stub!");
    }
}
