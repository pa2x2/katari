package eu.kanade.tachiyomi.ui.webview

import eu.kanade.presentation.util.Screen
import mihon.core.common.platform.PlatformFacility
import mihon.entry.interactions.source.EntryWebViewDestination

/**
 * A source's site in the app's web view, opened from shared screens such as the library update report, which push
 * [screen], and from readers and players, which [open] it.
 */
@PlatformFacility
interface WebViewDestination : EntryWebViewDestination {
    /** Only called while [isAvailable]. */
    fun screen(url: String, title: String?, sourceId: Long?, headers: Map<String, String>): Screen
}
