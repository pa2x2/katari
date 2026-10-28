package mihon.desktop.browse

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.BrowseDestination

/**
 * Desktop can't browse sources yet, so the empty library doesn't offer them, and neither library search nor an entry's
 * title and author offer to search them. Stands in until desktop hosts the Browse tab.
 */
@Inject
class NoBrowseDestination : BrowseDestination {
    override val isAvailable: Boolean = false

    override suspend fun openSources() = error("Browsing sources is not available on desktop")

    override fun globalSearch(query: String): Screen = error("Global search is not available on desktop")
}
