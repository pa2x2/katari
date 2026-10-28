package eu.kanade.tachiyomi.ui.browse

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.home.HomeScreen

@Inject
class BrowseScreenDestination : BrowseDestination {
    override val isAvailable: Boolean = true

    override suspend fun open(page: BrowseTab.Page?) {
        HomeScreen.openTab(HomeScreen.Tab.Browse(page))
    }
}
