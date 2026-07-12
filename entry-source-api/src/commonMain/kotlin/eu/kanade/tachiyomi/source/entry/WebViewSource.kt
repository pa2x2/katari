package eu.kanade.tachiyomi.source.entry

/**
 * Optional capability for sources that can expose a canonical details page URL.
 */
interface WebViewSource : UnifiedSource {

    /**
     * Returns the canonical URL for the entry's details page.
     */
    fun getContentUrl(entry: SEntry): String

    /**
     * Returns headers to use when opening the WebView for this source.
     */
    fun getWebViewHeaders(): Map<String, String> = emptyMap()
}

/**
 * Optional capability for sources that can expose canonical chapter page URLs.
 */
interface ChapterWebViewSource : WebViewSource {

    /**
     * Returns the canonical URL for the chapter's details/reader page.
     */
    fun getChapterUrl(chapter: SEntryChapter): String
}
