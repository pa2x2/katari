package eu.kanade.presentation.more.settings.screen

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.browse.longpress.BrowseLongPressActionsDestination
import eu.kanade.presentation.util.Screen

@Inject
class BrowseLongPressActionsScreenDestination : BrowseLongPressActionsDestination {
    override val isAvailable: Boolean = true

    override fun screen(): Screen = BrowseLongPressActionsScreen()
}
