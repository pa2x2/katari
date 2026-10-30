package mihon.webview.jcef.surface

import eu.kanade.tachiyomi.ui.webview.browser.BrowserRequest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.network.CefRequest

/**
 * A source's headers, which go with every page the browser loads from the source's [host], the first one included, and
 * with nothing it loads from another site, as Android's web view sends them with the links it follows there.
 */
internal class SiteHeaders private constructor(private val host: String?, private val headers: Map<String, String>) {
    private val handler = object : CefResourceRequestHandlerAdapter() {
        override fun onBeforeResourceLoad(browser: CefBrowser, frame: CefFrame, request: CefRequest): Boolean {
            headers.forEach { (name, value) -> request.setHeaderByName(name, value, true) }
            return false
        }
    }

    /** What adds the headers to [request], where it is a page of the source's site; called on Chromium's IO thread. */
    fun handlerFor(request: CefRequest): CefResourceRequestHandler? = handler.takeIf {
        headers.isNotEmpty() &&
            request.resourceType == CefRequest.ResourceType.RT_MAIN_FRAME &&
            request.url.toHttpUrlOrNull()?.host == host
    }

    companion object {
        fun of(request: BrowserRequest) = SiteHeaders(request.url.toHttpUrlOrNull()?.host, request.headers)
    }
}
