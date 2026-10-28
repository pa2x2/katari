package mihon.desktop.browse

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.browse.BrowseDestination
import eu.kanade.tachiyomi.ui.browse.BrowseTab

/**
 * Desktop's window has no home screen with a Browse tab yet, so the empty library doesn't offer to open the sources
 * and a finished migration doesn't return to Browse. Stands in until desktop hosts the home screen.
 */
@Inject
class NoBrowseDestination : BrowseDestination {
    override val isAvailable: Boolean = false

    override suspend fun open(page: BrowseTab.Page?) = error("The home screen's Browse tab is not available on desktop")
}
