package eu.kanade.tachiyomi.ui.webview.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.core.common.platform.PlatformFacility
import kotlinx.coroutines.CoroutineScope

/**
 * The platform's browser, as the web view screen shows it. The screen owns the pages it opens: it keeps the open ones as
 * tabs, shows one of them at a time with [Page] and closes each when it is done with it.
 */
@PlatformFacility
interface BrowserView {
    /**
     * A page that loads [request] once it is first shown, in a browser that sends [userAgent]. The browser reports what
     * happens on the page to [events] and does its work in [scope], so the page stops with it.
     */
    fun openPage(
        request: BrowserRequest,
        userAgent: String,
        scope: CoroutineScope,
        events: BrowserEvents,
    ): BrowserPage

    /**
     * Shows [page] as it was when it was last shown, history included. The page keeps loading while another one is
     * shown instead.
     */
    @Composable
    fun Page(page: BrowserPage, modifier: Modifier)
}

/**
 * [url] with [headers] added to its request. The browser also adds them when the user follows a link to [url]'s host,
 * and to no other site, so a source's headers reach only the source.
 */
class BrowserRequest(val url: String, val headers: Map<String, String>)
