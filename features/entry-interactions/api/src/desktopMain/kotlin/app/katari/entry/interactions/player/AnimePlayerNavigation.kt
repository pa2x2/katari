package app.katari.entry.interactions.player

/**
 * Where the desktop player leaves for: back to the screen it was opened from, the player's settings, the screens that
 * fix why an episode can't play, the episode's page in the web view, and between the main window and its
 * picture-in-picture window.
 */
interface AnimePlayerNavigation {
    /** False where the app has no web view; the episode's menu then leaves it out. */
    val webViewAvailable: Boolean

    /** Ends the player, from the main window or from picture-in-picture. */
    fun close()

    fun openPlayerSettings()

    fun openExtensions()

    fun openEntry(entryId: Long)

    fun openSourceSettings(sourceId: Long)

    /** Only called while [webViewAvailable]. */
    fun openWebView(url: String, title: String?, sourceId: Long?)

    /** Moves the player into its picture-in-picture window; the main window goes back to the screen under it. */
    fun enterPictureInPicture()

    /**
     * Brings the player back from picture-in-picture to fill the main window, over whatever that shows now, and the
     * main window forward. Called while the player fills the window, it only brings the window forward.
     */
    fun bringToMainWindow()
}
