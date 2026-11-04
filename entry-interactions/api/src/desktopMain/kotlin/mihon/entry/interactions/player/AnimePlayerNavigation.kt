package mihon.entry.interactions.player

/**
 * Where the desktop player leaves for, each a screen of the window's navigation: back to the screen it was opened
 * from, the player's settings, the screens that fix why an episode can't play, and the episode's page in the web view.
 */
interface AnimePlayerNavigation {
    /** False where the app has no web view; the episode's menu then leaves it out. */
    val webViewAvailable: Boolean

    fun close()

    fun openPlayerSettings()

    fun openExtensions()

    fun openEntry(entryId: Long)

    fun openSourceSettings(sourceId: Long)

    /** Only called while [webViewAvailable]. */
    fun openWebView(url: String, title: String?, sourceId: Long?)
}
