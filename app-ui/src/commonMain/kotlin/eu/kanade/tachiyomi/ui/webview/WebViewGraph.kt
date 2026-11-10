package eu.kanade.tachiyomi.ui.webview

import eu.kanade.tachiyomi.ui.webview.browser.BrowserView
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetup

interface WebViewGraph {
    val browserView: BrowserView
    val webViewSetup: WebViewSetup
}
