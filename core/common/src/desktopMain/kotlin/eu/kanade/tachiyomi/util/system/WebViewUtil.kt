package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import app.katari.core.common.web.webViewSafeHeaders
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Headers
import kotlin.coroutines.resume

/**
 * Android's web view helpers, kept for extensions that call them. Desktop has no web view yet: [supportsWebView] says
 * so, as on an Android device without one, and the helpers that drive a [WebView] are Android's, which reach the
 * stand-in's "Stub!".
 */
object WebViewUtil {
    private const val CHROME_PACKAGE = "com.android.chrome"

    const val MINIMUM_WEBVIEW_VERSION = 118

    /** What Android answers when no web view package is installed. */
    fun getVersion(context: Context): String = "how did you get here?"

    fun supportsWebView(context: Context): Boolean = false

    /** The package Android names first, as desktop has no installed packages to choose among. */
    fun spoofedPackageName(context: Context): String = CHROME_PACKAGE
}

fun WebView.isOutdated(): Boolean {
    return getWebViewMajorVersion() < WebViewUtil.MINIMUM_WEBVIEW_VERSION
}

suspend fun WebView.getHtml(): String = suspendCancellableCoroutine {
    evaluateJavascript("document.documentElement.outerHTML") { html -> it.resume(html) }
}

fun WebView.setDefaultSettings() {
    with(settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        cacheMode = WebSettings.LOAD_DEFAULT

        // Handle popups properly
        setSupportMultipleWindows(true)

        // Allow zooming
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
    }

    CookieManager.getInstance().acceptThirdPartyCookies(this)
}

/** Android also sets the matching client hints through AndroidX WebKit, which desktop doesn't have. */
fun WebView.setUserAgent(userAgent: String) {
    settings.userAgentString = userAgent
}

fun Headers.toWebViewHeaders(): Map<String, String> = webViewSafeHeaders()

fun Map<String, String>.toWebViewHeaders(): Map<String, String> = webViewSafeHeaders()

private fun WebView.getWebViewMajorVersion(): Int {
    val uaRegexMatch = """.*Chrome/(\d+)\..*""".toRegex().matchEntire(getDefaultUserAgentString())
    return if (uaRegexMatch != null && uaRegexMatch.groupValues.size > 1) {
        uaRegexMatch.groupValues[1].toInt()
    } else {
        0
    }
}

// Based on https://stackoverflow.com/a/29218966
private fun WebView.getDefaultUserAgentString(): String {
    val originalUA: String = settings.userAgentString

    // Next call to getUserAgentString() will get us the default
    settings.userAgentString = null
    val defaultUserAgentString = settings.userAgentString

    // Revert to original UA string
    settings.userAgentString = originalUA

    return defaultUserAgentString
}
