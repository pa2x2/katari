package eu.kanade.tachiyomi.ui.browse

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.home.HomeScreen

@Inject
class BrowseScreenDestination : BrowseDestination {
    override val isAvailable: Boolean = true

    override suspend fun openSources() {
        HomeScreen.openTab(HomeScreen.Tab.Browse(BrowseTab.Page.Sources))
    }
}
