package eu.kanade.tachiyomi.ui.webview.browser

import android.os.Message
import android.webkit.WebView
import com.kevinnzou.web.LoadingState
import com.kevinnzou.web.WebContent
import com.kevinnzou.web.WebViewNavigator
import com.kevinnzou.web.WebViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A page in an Android [WebView], kept by compose-webview's [state] and [navigator]. The [WebView] outlives the page's
 * time on screen, so a tab shown again keeps its history; [popupMessage] is the transport a popup's [WebView] is handed
 * to its opener through.
 */
internal class AndroidBrowserPage(
    content: WebContent,
    val popupMessage: Message?,
    val siteHeaders: SiteHeaders,
    val userAgent: String,
    private val scope: CoroutineScope,
    val events: BrowserEvents,
) : BrowserPage {
    val state = WebViewState(content)
    val navigator = WebViewNavigator(scope)
    val client = AndroidBrowserPageClient(this)
    val chromeClient = AndroidBrowserPageChromeClient(this)

    var webView: WebView? = null
    private var shown = false
    private var closed = false

    override val title: String?
        get() = state.pageTitle

    override val loading: BrowserLoading
        get() = when (val loading = state.loadingState) {
            LoadingState.Initializing -> BrowserLoading.Starting
            is LoadingState.Loading -> BrowserLoading.Loading(loading.progress)
            LoadingState.Finished -> BrowserLoading.Finished
        }

    override val canGoBack: Boolean
        get() = navigator.canGoBack

    override val canGoForward: Boolean
        get() = navigator.canGoForward

    override fun goBack() = navigator.navigateBack()

    override fun goForward() = navigator.navigateForward()

    override fun reload() = navigator.reload()

    override suspend fun evaluateJavascript(script: String): String? {
        val view = webView ?: return null
        return suspendCancellableCoroutine { continuation ->
            view.evaluateJavascript(script) { result -> continuation.resume(result) }
        }
    }

    override fun close() {
        closed = true
        if (!shown) release()
    }

    fun popup(message: Message) =
        AndroidBrowserPage(WebContent.NavigatorOnly, message, siteHeaders, userAgent, scope, events)

    fun onShown(view: WebView) {
        webView = view
        shown = true
    }

    /** The page's [WebView] left the screen; a closed page frees it, an open one keeps it to be shown again. */
    fun onHidden(view: WebView) {
        shown = false
        if (closed || webView !== view) {
            view.destroy()
            if (webView === view) webView = null
        } else {
            // The page resumes where it was when it is shown again, rather than loading its first address anew
            state.content = WebContent.NavigatorOnly
        }
    }

    private fun release() {
        webView?.destroy()
        webView = null
    }
}

/** A source's headers, which go with links to the source's own [host] only. */
internal class SiteHeaders(val host: String?, val headers: Map<String, String>)
