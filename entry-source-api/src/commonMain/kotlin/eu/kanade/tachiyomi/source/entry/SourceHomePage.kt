package eu.kanade.tachiyomi.source.entry

/**
 * Optional capability for sources that expose a browser home page.
 */
interface SourceHomePage : UnifiedSource {

    /**
     * Returns the source home page URL, or null when it should not be opened directly.
     */
    fun getHomeUrl(): String?
}
