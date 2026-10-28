package eu.kanade.presentation.more.settings.screen.navigation

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen

@Inject
class HomeNavigationEditorScreenDestination : HomeNavigationEditorDestination {
    override val isAvailable: Boolean = true

    override fun screen(): Screen = HomeNavigationEditorScreen()
}
