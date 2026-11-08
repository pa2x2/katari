package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import app.katari.core.common.HomeScreenTabs
import app.katari.core.common.navigation.HomeNavigationConfiguration
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen

/**
 * Connects the home destinations shown beside every screen of a wide window to the home tabs, which aren't composed
 * while another screen is on top. Home publishes the tab it shows here, and a tap on the side dock waits in
 * [requestedTab] until home is back on top to carry it out.
 */
@Stable
class HomeNavigationState internal constructor(tabSettings: State<HomeTabSettings>) {
    internal val tabSettings by tabSettings

    val configuration: HomeNavigationConfiguration
        get() = tabSettings.configuration

    /** Null until home is first shown. */
    var selectedTab by mutableStateOf<HomeScreenTabs?>(null)

    /** Set by a home tab that takes the window for itself, such as while items are selected. */
    var hiddenByTab by mutableStateOf(false)

    var requestedTab by mutableStateOf<HomeScreenTabs?>(null)

    // Before home first shows, the tab it will open on is the one under the screen on top
    private val homeTab: HomeScreenTabs
        get() = selectedTab ?: tabSettings.openingTab()

    /** The destination the dock marks as open over [navigator]'s top screen. */
    fun markedTab(navigator: Navigator): HomeScreenTabs =
        if (navigator.lastItem.isStatistics()) HomeScreenTabs.Statistics else homeTab

    /**
     * Opens [tab] as a tap on the dock does. From another screen it returns to the home tabs first, so asking for the
     * tab that was open under it only goes back to it.
     */
    fun open(tab: HomeScreenTabs, navigator: Navigator) {
        val screen = navigator.lastItem
        if (screen is HomeScreen) {
            requestedTab = tab
        } else if (!(screen.isStatistics() && tab == HomeScreenTabs.Statistics)) {
            val tabUnder = homeTab
            navigator.popUntilRoot()
            if (tab != tabUnder) requestedTab = tab
        }
    }

    // Statistics is a screen of its own rather than a home tab, so the dock marks it while it's open
    private fun Screen.isStatistics() =
        this is StatsScreen && HomeScreenTabs.Statistics in configuration.enabledTabs
}

val LocalHomeNavigation = staticCompositionLocalOf<HomeNavigationState> {
    error("HomeNavigationState is provided by HomeSideNavigationHost")
}
