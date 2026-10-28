package mihon.desktop.browse

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.browse.longpress.BrowseLongPressActionsDestination
import eu.kanade.presentation.util.Screen

/**
 * Desktop browses no sources yet, so a long press on a browsed entry has nothing to act on and its setting is hidden.
 * Stands in until desktop hosts browsing with its source rows.
 */
@Inject
class NoBrowseLongPressActionsDestination : BrowseLongPressActionsDestination {
    override val isAvailable: Boolean = false

    override fun screen(): Screen = error("Browse long-press actions are not available on desktop")
}
