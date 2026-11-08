package app.katari.webview.jcef.page.extension

import app.katari.core.common.web.ExtensionPageEvents
import app.katari.core.common.web.ExtensionPageEvents.ConsoleLevel
import app.katari.core.common.web.ExtensionPageRequest
import app.katari.core.common.web.ExtensionPageResponse
import app.katari.webview.jcef.cookie.ChromiumCookieMirror
import app.katari.webview.jcef.page.HiddenBrowser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import org.cef.CefClient
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefFileDialogCallback
import org.cef.handler.CefDialogHandler
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefRequestHandler
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceHandler
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import org.cef.network.CefURLRequest
import tachiyomi.core.common.util.system.logcat
import java.util.Vector

/**
 * What Chromium tells of an extension page's browser, on threads of its own, passed on to [page]'s events while it is
 * open: the main frame's loads, a finished one once the cookies it set are in the app's store, every frame's failed
 * loads, the console with the bridges' calls taken out ([ExtensionBridges]), and the renderer's end. Nothing is told of
 * the blank page the browser starts on. Cookies a response or another site's frame set are read back as it completes.
 *
 * Every request of the page goes to the extension first ([ExtensionPageEvents.interceptRequest]), after the document a
 * load shows at its base URL and the headers a load sends ([PendingLoads]); images are refused after that where the
 * page blocks them. Nothing of the page opens outside it: new windows and file choosers are refused, as an Android web
 * view refuses windows its page opens without a person's tap.
 */
internal class ExtensionClient(
    private val page: ExtensionBrowser,
    private val events: ExtensionPageEvents,
    private val bridges: ExtensionBridges,
    private val loads: PendingLoads,
    private val cookies: ChromiumCookieMirror,
    private val scope: CoroutineScope,
    private val created: CompletableDeferred<CefBrowser>,
    private val onClosed: () -> Unit,
) {
    fun attachTo(client: CefClient) {
        client.addLifeSpanHandler(lifeSpan)
        client.addLoadHandler(load)
        client.addDisplayHandler(display)
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
        override fun onLoadStart(browser: CefBrowser, frame: CefFrame, transitionType: CefRequest.TransitionType) {
            val url = frame.url ?: return
            if (frame.isMain && page.tells(url)) events.onPageStarted(url)
        }

        override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
            // A frame of another site may have set cookies too, such as a challenge's
            if (!frame.isMain) {
                cookies.readBack()
                return
            }
            val url = frame.url ?: return
            if (!page.tells(url)) return
            scope.launch {
                cookies.awaitReadBack()
                if (page.isOpen) events.onPageFinished(url)
            }
        }

        override fun onLoadError(
            browser: CefBrowser,
            frame: CefFrame,
            errorCode: CefLoadHandler.ErrorCode,
            errorText: String?,
            failedUrl: String?,
        ) {
            // A load that another replaced, or that the extension stopped; Android's web view tells of neither
            if (errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED) return
            val url = failedUrl ?: return
            if (!page.tells(url)) return
            events.onLoadFailed(url, frame.isMain, errorCode.code, errorText.orEmpty())
        }
    }

    private val display = object : CefDisplayHandlerAdapter() {
        override fun onTitleChange(browser: CefBrowser, title: String?) {
            if (title != null && page.tells(browser.url.orEmpty())) events.onTitleChanged(title)
        }

        override fun onConsoleMessage(
            browser: CefBrowser,
            level: CefSettings.LogSeverity,
            message: String,
            source: String?,
            line: Int,
        ): Boolean {
            val bridged = bridges.read(message) { call ->
                if (page.isOpen) events.onBridgeCall(call.name, call.method, call.arguments)
            }
            if (!bridged && page.isOpen) {
                events.onConsoleMessage(level.toConsoleLevel(), message, source.orEmpty(), line)
            }
            return true
        }
    }

    private val resources = object : CefResourceRequestHandlerAdapter() {
        override fun onBeforeResourceLoad(browser: CefBrowser, frame: CefFrame, request: CefRequest): Boolean {
            if (request.resourceType == CefRequest.ResourceType.RT_MAIN_FRAME) {
                loads.takeHeaders(request.url)?.forEach { (name, value) -> request.setHeaderByName(name, value, true) }
            }
            return false
        }

        override fun getResourceHandler(
            browser: CefBrowser,
            frame: CefFrame,
            request: CefRequest,
        ): CefResourceHandler? {
            if (!page.isOpen) return null
            val isMainFrame = request.resourceType == CefRequest.ResourceType.RT_MAIN_FRAME
            if (isMainFrame) loads.takeContent(request.url)?.let { return InterceptedResponse(it) }
            intercept(request, isMainFrame)?.let { return InterceptedResponse(it) }
            if (page.blocksImages && request.resourceType == CefRequest.ResourceType.RT_IMAGE && request.isNetwork()) {
                return RefusedRequest()
            }
            return null
        }

        // JBR's out-of-process JCEF asks no cookie access filter, so a response's own header tells it set cookies
        override fun onResourceLoadComplete(
            browser: CefBrowser,
            frame: CefFrame,
            request: CefRequest,
            response: CefResponse,
            status: CefURLRequest.Status,
            receivedContentLength: Long,
        ) {
            if (!response.getHeaderByName(SET_COOKIE).isNullOrEmpty()) cookies.readBack()
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
        ): CefResourceRequestHandler? = resources.takeIf { request.url != HiddenBrowser.BLANK_PAGE }

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
            logcat(LogPriority.ERROR) { "An extension page's renderer ended: $status" }
            val crashed = status != CefRequestHandler.TerminationStatus.TS_PROCESS_WAS_KILLED
            if (page.isOpen) events.onPageGone(crashed)
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

    private fun intercept(request: CefRequest, isMainFrame: Boolean): ExtensionPageResponse? {
        val headers = HashMap<String, String>().also(request::getHeaderMap)
        val pageRequest = ExtensionPageRequest(request.url, request.method ?: "GET", headers, isMainFrame)
        return try {
            events.interceptRequest(pageRequest)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "An extension failed answering its page's request for ${request.url}" }
            null
        }
    }

    private fun CefRequest.isNetwork(): Boolean = url.startsWith("http://") || url.startsWith("https://")

    private fun CefSettings.LogSeverity.toConsoleLevel(): ConsoleLevel = when (this) {
        // Chromium logs console.debug as verbose
        CefSettings.LogSeverity.LOGSEVERITY_VERBOSE -> ConsoleLevel.DEBUG
        CefSettings.LogSeverity.LOGSEVERITY_WARNING -> ConsoleLevel.WARNING
        CefSettings.LogSeverity.LOGSEVERITY_ERROR, CefSettings.LogSeverity.LOGSEVERITY_FATAL -> ConsoleLevel.ERROR
        else -> ConsoleLevel.LOG
    }

    private companion object {
        const val SET_COOKIE = "Set-Cookie"
    }
}
