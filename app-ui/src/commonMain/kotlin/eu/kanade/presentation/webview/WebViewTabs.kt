package eu.kanade.presentation.webview

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.webview.browser.BrowserPage

/**
 * The web view screen's tabs: the page it opened, and the popups opened over it, the last one shown. Pages keep loading
 * in the background, so what a page reports goes to its own tab, and the shown tab's bar and actions never take another
 * page's address.
 */
@Stable
internal class WebViewTabs {
    private val tabs = mutableStateListOf<WebViewTab>()

    /** Only read once a tab is open. */
    val shown: WebViewTab
        get() = tabs.last()

    val count: Int
        get() = tabs.size

    val pages: List<BrowserPage>
        get() = tabs.map(WebViewTab::page)

    /** Shows [page] over the tab shown so far; [url] is the address it loads, null where it isn't known yet. */
    fun open(page: BrowserPage, url: String?) {
        tabs += WebViewTab(page, url)
    }

    /** Closes the shown tab's page and shows the tab beneath it. */
    fun closeShown() {
        tabs.removeAt(tabs.lastIndex).page.close()
    }

    fun onUrlChange(page: BrowserPage, url: String) {
        tabOf(page)?.url = url
    }

    fun onCloudflareCheck(page: BrowserPage, showsCheck: Boolean) {
        tabOf(page)?.showsCloudflareHelp = showsCheck
    }

    // A closed page may still report what it was doing as it closed
    private fun tabOf(page: BrowserPage) = tabs.firstOrNull { it.page === page }
}

@Stable
internal class WebViewTab(val page: BrowserPage, url: String?) {
    /** The address [page] last reported; null for a popup until it reports its first. */
    var url by mutableStateOf(url)

    var showsCloudflareHelp by mutableStateOf(false)
}
