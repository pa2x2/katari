package eu.kanade.tachiyomi.ui.browse

/**
 * The home screen's Browse tab, where shared screens send the user to find entries outside the library, and where a
 * migration started from a catalogue returns to.
 */
interface BrowseDestination {
    /** False where the home screen can't show Browse yet; what would open it is then hidden. */
    val isAvailable: Boolean

    /**
     * Switches the home screen to Browse, on [page], or on the page it last showed when null. Only called while
     * [isAvailable].
     */
    suspend fun open(page: BrowseTab.Page?)
}
