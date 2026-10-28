package eu.kanade.tachiyomi.ui.browse

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen

@Inject
class BrowseScreenDestination : BrowseDestination {
    override val isAvailable: Boolean = true

    override suspend fun openSources() {
        HomeScreen.openTab(HomeScreen.Tab.Browse(BrowseTab.Page.Sources))
    }

    override fun globalSearch(query: String): Screen = GlobalSearchScreen(query)
}
