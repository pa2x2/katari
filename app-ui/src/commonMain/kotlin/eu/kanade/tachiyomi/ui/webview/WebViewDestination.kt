package eu.kanade.tachiyomi.ui.webview

import eu.kanade.presentation.util.Screen

/** A source's site in the app's web view, opened from shared screens such as the library update report. */
interface WebViewDestination {
    /** False where the app has no web view; what would open one is then hidden. */
    val isAvailable: Boolean

    /** Only called while [isAvailable]. */
    fun screen(url: String, title: String?, sourceId: Long?, headers: Map<String, String>): Screen
}
