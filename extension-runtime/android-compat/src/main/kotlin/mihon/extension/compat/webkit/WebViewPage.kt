package mihon.extension.compat.webkit

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import logcat.LogPriority
import mihon.core.common.web.ExtensionPage
import mihon.core.common.web.ExtensionPageEvents
import mihon.core.common.web.ExtensionPageEvents.ConsoleLevel
import mihon.core.common.web.ExtensionPageRequest
import mihon.core.common.web.ExtensionPageResponse
import mihon.core.common.web.ExtensionPages
import mihon.extension.compat.AndroidCompatEnvironment
import tachiyomi.core.common.util.system.logcat
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * What an extension's [WebView] is on desktop: one of the host's hidden pages ([AndroidCompatEnvironment.webPages]),
 * which nothing draws, sending the view's settings' user agent from all its frames and sharing the app's cookies.
 *
 * Threads are Android's: every [WebViewClient] and [WebChromeClient] callback and every script's value comes on the
 * main looper's thread, the one Android's web views belong to, except [WebViewClient.shouldInterceptRequest], which
 * Chromium asks on a thread of its own as Android's web view asks on one of its, and the bridges' methods, which run
 * on their own thread ([JavascriptBridges]). Calls are taken from any thread and reach the page in the order made, as
 * the host's `Dispatchers.Main`, which Android extensions take for the main looper, is the desktop's event thread.
 *
 * Once destroyed, or once its renderer is gone, the view does nothing more and tells nothing more, as Android's;
 * destroy returns once the cookies its page set are in the app's store, where Android's web view keeps them all along.
 * A view its extension forgets without destroying it closes its page when collected ([ForgottenPages]).
 */
class WebViewPage private constructor(private val view: WebView, pages: ExtensionPages) {
    private val settings = PageWebSettings(pages.defaultUserAgent) { settings -> page.apply(settings) }
    private val bridges = JavascriptBridges()
    private val closed = AtomicBoolean(false)
    private val page: ExtensionPage = pages.open(settings.toPageSettings(), Events(WeakReference(this)))
    private val activity = ForgottenPages.track(view, page)

    @Volatile
    private var viewClient: WebViewClient = WebViewClient()

    @Volatile
    private var chromeClient: WebChromeClient? = null

    @Volatile
    private var url: String? = null

    @Volatile
    private var title: String? = null

    @Volatile
    private var progress = 0

    fun getSettings(): WebSettings = settings

    fun setWebViewClient(client: WebViewClient) {
        viewClient = client
    }

    fun getWebViewClient(): WebViewClient = viewClient

    fun setWebChromeClient(client: WebChromeClient?) {
        chromeClient = client
    }

    fun getWebChromeClient(): WebChromeClient? = chromeClient

    fun getUrl(): String? = url

    // Chromium tells of no redirect, so the page's address is the one it was asked for
    fun getOriginalUrl(): String? = url

    fun getTitle(): String? = title

    fun getProgress(): Int = progress

    fun loadUrl(url: String, headers: Map<String, String>?) {
        if (isUsable("loadUrl")) page.load(url, headers.orEmpty())
    }

    /** A document of no origin, [data] percent-encoded or, where [encoding] says `base64`, Base64-encoded. */
    fun loadData(data: String, mimeType: String?, encoding: String?) {
        if (!isUsable("loadData")) return
        val base64 = if (encoding.equals(BASE64, ignoreCase = true)) ";$BASE64" else ""
        page.load("data:${mimeType ?: DEFAULT_MIME_TYPE}$base64,$data", emptyMap())
    }

    // A base URL of the data scheme loads the data as one of no origin, as on Android
    fun loadDataWithBaseUrl(baseUrl: String?, data: String, mimeType: String?, encoding: String?, historyUrl: String?) {
        if (baseUrl != null && baseUrl.startsWith("data:", ignoreCase = true)) return loadData(data, mimeType, encoding)
        if (!isUsable("loadDataWithBaseURL")) return
        page.loadContent(baseUrl, data, mimeType ?: DEFAULT_MIME_TYPE, encoding ?: DEFAULT_ENCODING)
    }

    fun evaluateJavascript(script: String, callback: ValueCallback<String>?) {
        if (!isUsable("evaluateJavascript")) return
        val events = WeakReference(this)
        page.evaluate(script, callback?.let { { value -> onMainLooper(events) { callback.onReceiveValue(value) } } })
    }

    /**
     * Nothing draws the page, so the state the page's calls so far made is as drawn as it gets once the page has taken
     * them: [callback] comes after a script queued behind them has run.
     */
    fun postVisualStateCallback(requestId: Long, callback: WebView.VisualStateCallback) {
        if (!isUsable("postVisualStateCallback")) return
        val events = WeakReference(this)
        page.evaluate(NO_SCRIPT) { onMainLooper(events) { callback.onComplete(requestId) } }
    }

    fun addJavascriptInterface(target: Any, name: String) {
        if (isUsable("addJavascriptInterface")) page.addBridge(name, bridges.add(name, target))
    }

    fun removeJavascriptInterface(name: String) {
        if (!isUsable("removeJavascriptInterface")) return
        bridges.remove(name)
        page.removeBridge(name)
    }

    /** Hands the page the touches [event] makes; false where it makes none, or once the view is destroyed. */
    fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (!isUsable("dispatchTouchEvent")) return false
        val touches = pageTouches(event, view.resources.displayMetrics.density)
        touches.forEach(page::touch)
        return touches.isNotEmpty()
    }

    fun stopLoading() {
        if (isUsable("stopLoading")) page.stopLoading()
    }

    fun reload() {
        if (isUsable("reload")) page.reload()
    }

    fun destroy() {
        close()
    }

    private fun close() {
        if (!closed.compareAndSet(false, true)) return
        ForgottenPages.untrack(page)
        page.close()
    }

    private fun isUsable(call: String): Boolean {
        activity.used()
        if (closed.get()) logcat(LogPriority.WARN) { "An extension called $call on a destroyed web view" }
        return !closed.get()
    }

    private fun onPageStarted(url: String) {
        this.url = url
        progress = STARTED_PROGRESS
        viewClient.onPageStarted(view, url, null)
        chromeClient?.onProgressChanged(view, STARTED_PROGRESS)
    }

    private fun onPageFinished(url: String) {
        this.url = url
        progress = FINISHED_PROGRESS
        chromeClient?.onProgressChanged(view, FINISHED_PROGRESS)
        viewClient.onPageFinished(view, url)
    }

    private fun onTitleChanged(title: String) {
        this.title = title
        chromeClient?.onReceivedTitle(view, title)
    }

    private fun onLoadFailed(request: ExtensionPageRequest, errorCode: Int, description: String) {
        viewClient.onReceivedError(
            view,
            PageResourceRequest(request),
            PageResourceError(webViewErrorCode(errorCode), description),
        )
    }

    private fun onConsoleMessage(level: ConsoleLevel, message: String, sourceId: String, lineNumber: Int) {
        val messageLevel = when (level) {
            ConsoleLevel.DEBUG -> ConsoleMessage.MessageLevel.DEBUG
            ConsoleLevel.LOG -> ConsoleMessage.MessageLevel.LOG
            ConsoleLevel.WARNING -> ConsoleMessage.MessageLevel.WARNING
            ConsoleLevel.ERROR -> ConsoleMessage.MessageLevel.ERROR
        }
        val handled = chromeClient?.onConsoleMessage(ConsoleMessage(message, sourceId, lineNumber, messageLevel))
        // As Android's web view logs what its client doesn't handle
        if (handled != true) logcat(LogPriority.DEBUG) { "Web view console: $message ($sourceId:$lineNumber)" }
    }

    private fun onPageGone(crashed: Boolean) {
        close()
        val handled = viewClient.onRenderProcessGone(view, PageGoneDetail(crashed))
        // Android ends the app where the client doesn't handle it; the host keeps running without the page
        if (!handled) logcat(LogPriority.ERROR) { "A web view's page ended and its extension didn't handle it" }
    }

    // On Chromium's thread
    private fun interceptRequest(request: ExtensionPageRequest): ExtensionPageResponse? {
        if (closed.get()) return null
        val response: WebResourceResponse? = try {
            viewClient.shouldInterceptRequest(view, PageResourceRequest(request))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "A web view's client failed answering ${request.url}" }
            null
        }
        return response?.toPageResponse()
    }

    /**
     * The page's news, passed on to its view's page on the threads Android's web view calls on while it is open. It
     * refers to the view's page weakly, as a page refers to its events until it closes, which only a destroyed or
     * collected view closes.
     */
    private class Events(private val owner: WeakReference<WebViewPage>) : ExtensionPageEvents {
        override fun onPageStarted(url: String) = onMainLooper(owner) { onPageStarted(url) }

        override fun onPageFinished(url: String) = onMainLooper(owner) { onPageFinished(url) }

        override fun onTitleChanged(title: String) = onMainLooper(owner) { onTitleChanged(title) }

        override fun onLoadFailed(url: String, isForMainFrame: Boolean, errorCode: Int, description: String) {
            // Chromium tells of no method or headers for a load that failed
            val request = ExtensionPageRequest(url, "GET", emptyMap(), isForMainFrame)
            onMainLooper(owner) { onLoadFailed(request, errorCode, description) }
        }

        override fun interceptRequest(request: ExtensionPageRequest): ExtensionPageResponse? =
            owner.get()?.interceptRequest(request)

        override fun onConsoleMessage(level: ConsoleLevel, message: String, sourceId: String, lineNumber: Int) =
            onMainLooper(owner) { onConsoleMessage(level, message, sourceId, lineNumber) }

        override fun onBridgeCall(name: String, method: String, arguments: String) {
            val page = owner.get()?.takeUnless { it.closed.get() } ?: return
            page.activity.used()
            page.bridges.call(name, method, arguments)
        }

        override fun onPageGone(crashed: Boolean) = onMainLooper(owner) { onPageGone(crashed) }
    }

    companion object {
        private const val BASE64 = "base64"
        private const val DEFAULT_MIME_TYPE = "text/html"
        private const val DEFAULT_ENCODING = "UTF-8"
        private const val NO_SCRIPT = "undefined"

        // Chromium tells JCEF no progress between a load's start and its end
        private const val STARTED_PROGRESS = 10
        private const val FINISHED_PROGRESS = 100

        private val mainLooper by lazy { Handler(Looper.getMainLooper()) }

        /**
         * The page behind a new [view]. Where the host has no pages, the view fails as the SDK's stub of it does, which
         * the host reports as a source needing a web view.
         */
        @JvmStatic
        fun open(view: WebView): WebViewPage {
            val pages = AndroidCompatEnvironment.current().webPages
            if (!pages.isOffered) throw RuntimeException("Stub!")
            return WebViewPage(view, pages)
        }

        private fun onMainLooper(owner: WeakReference<WebViewPage>, block: WebViewPage.() -> Unit) {
            mainLooper.post {
                val page = owner.get()?.takeUnless { it.closed.get() } ?: return@post
                page.activity.used()
                page.block()
            }
        }
    }
}
