package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import mihon.core.common.HomeScreenTabs
import mihon.core.common.navigation.HomeNavigationConfiguration

/**
 * Connects the home destinations shown beside every screen of a wide window to the home tabs, which aren't composed
 * while another screen is on top. Home publishes what it shows here, and a tap on the side dock waits in
 * [requestedTab] until home is back on top to carry it out.
 */
@Stable
class HomeNavigationState {
    /** Null until home is first shown. */
    var configuration by mutableStateOf<HomeNavigationConfiguration?>(null)
    var selectedTab by mutableStateOf<HomeScreenTabs?>(null)

    /** Set by a home tab that takes the window for itself, such as while items are selected. */
    var hiddenByTab by mutableStateOf(false)

    var requestedTab by mutableStateOf<HomeScreenTabs?>(null)

    /** The destination the dock marks as open over [navigator]'s top screen; null until home is first shown. */
    fun markedTab(navigator: Navigator): HomeScreenTabs? =
        if (navigator.lastItem.isStatistics()) HomeScreenTabs.Statistics else selectedTab

    /**
     * Opens [tab] as a tap on the dock does. From another screen it returns to the home tabs first, so asking for the
     * tab that was open under it only goes back to it.
     */
    fun open(tab: HomeScreenTabs, navigator: Navigator) {
        val screen = navigator.lastItem
        if (screen is HomeScreen) {
            requestedTab = tab
        } else if (!(screen.isStatistics() && tab == HomeScreenTabs.Statistics)) {
            val homeTab = selectedTab
            navigator.popUntilRoot()
            if (tab != homeTab) requestedTab = tab
        }
    }

    // Statistics is a screen of its own rather than a home tab, so the dock marks it while it's open
    private fun Screen.isStatistics() =
        this is StatsScreen && configuration?.enabledTabs?.contains(HomeScreenTabs.Statistics) == true
}

val LocalHomeNavigation = staticCompositionLocalOf<HomeNavigationState> {
    error("HomeNavigationState is provided by HomeSideNavigationHost")
}
