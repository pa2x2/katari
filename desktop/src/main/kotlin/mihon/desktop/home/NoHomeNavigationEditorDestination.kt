package mihon.desktop.home

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.navigation.HomeNavigationEditorDestination
import eu.kanade.presentation.util.Screen

/**
 * Desktop can't show the editor of the home screen's tabs yet, so the appearance settings leave it out. Stands in until
 * desktop hosts the home navigation editor.
 */
@Inject
class NoHomeNavigationEditorDestination : HomeNavigationEditorDestination {
    override val isAvailable: Boolean = false

    override fun screen(): Screen = error("The home navigation editor is not available on desktop")
}
