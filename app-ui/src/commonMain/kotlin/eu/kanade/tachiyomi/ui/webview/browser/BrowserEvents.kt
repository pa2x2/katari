package eu.kanade.tachiyomi.ui.webview.browser

/** What the browser tells the screen about its pages, on the main thread. */
interface BrowserEvents {
    /**
     * [page]'s address changed to [url]: a load started, or its history did, as when a site changes the address itself.
     */
    fun onUrlChange(page: BrowserPage, url: String)

    /** [page] finished loading [url], null when the browser didn't say which. */
    fun onPageFinished(page: BrowserPage, url: String?)

    /**
     * A page's renderer ended, by a crash when [crashed] rather than by the system reclaiming its memory. The browser
     * has already freed the page, which shows nothing from then on.
     */
    fun onRenderProcessGone(crashed: Boolean)

    /**
     * A page asked for [popup] in a new window, on a tap or click of the user's when [isUserGesture]. Returns whether
     * the screen shows it; a refused popup is never shown and needs no closing.
     */
    fun onNewWindow(popup: BrowserPage, isUserGesture: Boolean): Boolean

    /** [page] waits for [dialog] to be answered, which has to happen exactly once. */
    fun onJsDialog(page: BrowserPage, dialog: BrowserJsDialog)
}
