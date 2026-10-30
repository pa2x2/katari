package eu.kanade.tachiyomi.ui.webview.browser

/** A page [BrowserView] opened. Its properties are Compose state, so what reads them follows the page. */
interface BrowserPage {
    /** The title the loaded document gives itself; null until it has given one. */
    val title: String?

    val loading: BrowserLoading

    val canGoBack: Boolean

    val canGoForward: Boolean

    fun goBack()

    fun goForward()

    fun reload()

    /**
     * Runs [script] in the page's main frame and returns its value as JSON, as `JSON.stringify` writes it, or null
     * when the page couldn't run it.
     */
    suspend fun evaluateJavascript(script: String): String?

    /** Frees the browser the page has: at once if the page isn't shown, otherwise once it leaves the screen. */
    fun close()
}

sealed interface BrowserLoading {
    /** Nothing has loaded yet. */
    data object Starting : BrowserLoading

    /** [progress] is 0..1. */
    data class Loading(val progress: Float) : BrowserLoading

    data object Finished : BrowserLoading
}
