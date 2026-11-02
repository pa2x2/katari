package mihon.webview.jcef.page

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.core.common.web.ChallengePageEvents
import mihon.webview.jcef.cookie.ChromiumCookieMirror
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefFileDialogCallback
import org.cef.handler.CefDialogHandler
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefRequestHandler
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import tachiyomi.core.common.util.system.logcat
import java.util.Vector
import java.util.concurrent.atomic.AtomicBoolean

/**
 * What Chromium tells of a challenge page's browser, on threads of its own, passed on to [events]: a finished load of
 * the main frame once the cookies it set are in the app's store, the main frame's HTTP errors, and the renderer's end.
 * The source's [headers] go with the first page the browser loads, as Android's WebView sends them with the URL it is
 * told to load. Nothing of the page opens outside it: new windows and file choosers are refused.
 */
internal class ChallengeClient(
    private val headers: Map<String, String>,
    private val events: ChallengePageEvents,
    private val cookies: ChromiumCookieMirror,
    private val scope: CoroutineScope,
    private val created: CompletableDeferred<CefBrowser>,
    private val onClosed: () -> Unit,
) {
    private val headersSent = AtomicBoolean(false)

    fun attachTo(client: CefClient) {
        client.addLifeSpanHandler(lifeSpan)
        client.addLoadHandler(load)
        client.addRequestHandler(requests)
        client.addDialogHandler(fileDialogs)
    }

    private val lifeSpan = object : CefLifeSpanHandlerAdapter() {
        override fun onAfterCreated(browser: CefBrowser) {
            created.complete(browser)
        }

        override fun onBeforePopup(
            browser: CefBrowser,
            frame: CefFrame?,
            targetUrl: String?,
            targetFrameName: String?,
        ): Boolean = true

        override fun onBeforeClose(browser: CefBrowser) {
            onClosed()
        }
    }

    private val load = object : CefLoadHandlerAdapter() {
        override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
            if (!frame.isMain) return
            val loaded = frame.url
            if (loaded == ChallengeBrowser.BLANK_PAGE) return
            scope.launch {
                cookies.awaitReadBack()
                this@ChallengeClient.logcat(LogPriority.DEBUG) {
                    "A challenge page finished loading $loaded ($httpStatusCode)"
                }
                events.onPageFinished(loaded)
            }
        }
    }

    private val mainFrame = object : CefResourceRequestHandlerAdapter() {
        override fun onBeforeResourceLoad(browser: CefBrowser, frame: CefFrame, request: CefRequest): Boolean {
            if (headersSent.compareAndSet(false, true)) {
                headers.forEach { (name, value) -> request.setHeaderByName(name, value, true) }
            }
            return false
        }

        override fun onResourceResponse(
            browser: CefBrowser,
            frame: CefFrame,
            request: CefRequest,
            response: CefResponse,
        ): Boolean {
            if (response.status >= FIRST_ERROR_STATUS) {
                val responseHeaders = HashMap<String, String>().also(response::getHeaderMap)
                logcat(LogPriority.DEBUG) { "A challenge page got HTTP ${response.status} for ${request.url}" }
                events.onHttpError(response.status, responseHeaders)
            }
            return false
        }
    }

    private val requests = object : CefRequestHandlerAdapter() {
        override fun getResourceRequestHandler(
            browser: CefBrowser,
            frame: CefFrame?,
            request: CefRequest,
            isNavigation: Boolean,
            isDownload: Boolean,
            requestInitiator: String?,
            disableDefaultHandling: BoolRef,
        ): CefResourceRequestHandler? = mainFrame.takeIf {
            request.resourceType == CefRequest.ResourceType.RT_MAIN_FRAME && request.url != ChallengeBrowser.BLANK_PAGE
        }

        override fun onOpenURLFromTab(
            browser: CefBrowser,
            frame: CefFrame?,
            targetUrl: String?,
            userGesture: Boolean,
        ): Boolean = true

        override fun onRenderProcessTerminated(
            browser: CefBrowser,
            status: CefRequestHandler.TerminationStatus,
            errorCode: Int,
            errorString: String?,
        ) {
            logcat(LogPriority.ERROR) { "A challenge page's renderer ended: $status" }
            events.onPageGone()
        }
    }

    private val fileDialogs = object : CefDialogHandler {
        override fun onFileDialog(
            browser: CefBrowser,
            mode: CefDialogHandler.FileDialogMode,
            title: String?,
            defaultFilePath: String?,
            acceptFilters: Vector<String>?,
            acceptExtensions: Vector<String>?,
            acceptDescriptions: Vector<String>?,
            callback: CefFileDialogCallback,
        ): Boolean {
            callback.Cancel()
            return true
        }
    }

    private companion object {
        const val FIRST_ERROR_STATUS = 400
    }
}
