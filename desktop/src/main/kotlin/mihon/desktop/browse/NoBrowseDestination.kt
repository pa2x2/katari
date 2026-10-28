package mihon.desktop.browse

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.browse.BrowseDestination

/**
 * Desktop's window has no home screen with a Browse tab yet, so the empty library doesn't offer to open the sources.
 * Stands in until desktop hosts the home screen.
 */
@Inject
class NoBrowseDestination : BrowseDestination {
    override val isAvailable: Boolean = false

    override suspend fun openSources() = error("The home screen's sources are not available on desktop")
}
