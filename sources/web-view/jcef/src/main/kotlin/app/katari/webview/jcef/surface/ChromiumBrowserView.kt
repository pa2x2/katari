package app.katari.webview.jcef.surface

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.core.platform.clipboard.TextClipboard
import app.katari.core.platform.link.LinkOpener
import app.katari.webview.jcef.cookie.ChromiumCookieMirror
import app.katari.webview.jcef.data.SiteOrigins
import app.katari.webview.jcef.runtime.JcefRuntime
import eu.kanade.tachiyomi.ui.webview.browser.BrowserEvents
import eu.kanade.tachiyomi.ui.webview.browser.BrowserPage
import eu.kanade.tachiyomi.ui.webview.browser.BrowserRequest
import eu.kanade.tachiyomi.ui.webview.browser.BrowserView
import kotlinx.coroutines.CoroutineScope

/**
 * The web view screen's browser on Chromium: each page is an off-screen browser of its own that Compose draws (see
 * [ChromiumSurface]), so the kit's menus, dialogs and sheets draw over it as over any screen. Chromium starts as the
 * first page is shown. Pages open only where [runtime] has JCEF; the app binds this view on every runtime and offers
 * the web view only there, so this class names no JCEF type itself.
 */
class ChromiumBrowserView(
    private val runtime: JcefRuntime,
    private val cookies: ChromiumCookieMirror,
    private val origins: SiteOrigins,
    private val linkOpener: LinkOpener,
    private val clipboard: TextClipboard,
) : BrowserView {
    override fun openPage(
        request: BrowserRequest,
        userAgent: String,
        scope: CoroutineScope,
        events: BrowserEvents,
    ): BrowserPage = ChromiumPage(
        firstUrl = request.url,
        siteHeaders = SiteHeaders.of(request),
        userAgent = userAgent,
        scope = scope,
        events = events,
        runtime = runtime,
        cookies = cookies,
        origins = origins,
    )

    @Composable
    override fun Page(page: BrowserPage, modifier: Modifier) {
        require(page is ChromiumPage) { "A page this browser didn't open" }
        ChromiumSurface(page, linkOpener, clipboard, modifier)
    }
}
