package mihon.entry.interactions.reader.window

/** Where the desktop book reader leaves for: back to the screen it was opened from, its settings, the web view. */
interface BookReaderNavigation {
    /** False where the app has no web view; the chapter's menu then leaves it out. */
    val webViewAvailable: Boolean

    fun close()

    fun openReaderSettings()

    /** Only called while [webViewAvailable]. */
    fun openWebView(url: String, title: String?, sourceId: Long?)
}
