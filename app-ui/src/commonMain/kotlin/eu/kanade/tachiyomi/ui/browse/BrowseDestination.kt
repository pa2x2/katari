package eu.kanade.tachiyomi.ui.browse

/** Where shared screens send the user to find entries outside the library: the home screen's list of sources. */
interface BrowseDestination {
    /** False where the home screen can't show the sources yet; what would open them is then hidden. */
    val isAvailable: Boolean

    /** Switches the home screen to the list of sources. Only called while [isAvailable]. */
    suspend fun openSources()
}
