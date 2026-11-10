package app.katari.webpages.host

import app.katari.core.common.web.ExtensionPages
import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import eu.kanade.tachiyomi.source.entry.webpage.WebPageException

/**
 * Sources' pages in the app's Chromium, each one of the hidden pages extensions' own web views run on. Where Chromium
 * isn't set up, asking for a page tells the set-up so, as any other need of a web view without a screen does.
 */
class DesktopHostPages(
    private val pages: ExtensionPages,
    private val messages: WebViewInterceptorMessages,
) : HostPages {
    override suspend fun open(settings: HostPageSettings, events: HostPageEvents): HostPage {
        if (!pages.isOffered) throw WebPageException(messages.webViewRequired())
        return DesktopHostPage(pages, settings, events)
    }
}
