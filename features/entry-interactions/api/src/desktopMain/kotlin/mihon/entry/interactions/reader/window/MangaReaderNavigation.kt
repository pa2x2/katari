package mihon.entry.interactions.reader.window

/** Where the desktop reader leaves for: back to the screen it was opened from, its settings, the entry, the web view. */
interface MangaReaderNavigation {
    /** False where the app has no web view; the chapter's menu then leaves it out. */
    val webViewAvailable: Boolean

    fun close()

    fun openReaderSettings()

    fun openEntry(entryId: Long)

    /** Only called while [webViewAvailable]. */
    fun openWebView(url: String, title: String?, sourceId: Long?)
}
