package eu.kanade.tachiyomi.ui.browse

import eu.kanade.presentation.util.Screen

/** Where shared screens send the user to find entries outside the library: the sources and their global search. */
interface BrowseDestination {
    /** False where sources can't be browsed yet; what would open them is then hidden. */
    val isAvailable: Boolean

    /** Switches the home screen to the list of sources. Only called while [isAvailable]. */
    suspend fun openSources()

    /** Only called while [isAvailable]. */
    fun globalSearch(query: String): Screen
}
