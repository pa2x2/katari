package eu.kanade.tachiyomi.ui.webview

import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.entry.interactions.source.EntryWebViewFeature

interface WebViewGraph {
    val entryWebViewFeature: EntryWebViewFeature
    val networkHelper: NetworkHelper
}
