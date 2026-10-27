package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import mihon.core.common.HomeScreenTabs
import mihon.presentation.kit.navigation.DockPlacement
import mihon.presentation.kit.navigation.FloatingDockHost
import mihon.presentation.kit.window.currentWindowLayout

/**
 * Lays the home destinations beside every screen of [navigator] in a wide window, not only beside the home tabs, so
 * any screen is one tap from them. A tap on another screen returns to the home tabs first, so tapping the tab that
 * was open under it only goes back to it.
 */
@Composable
fun HomeSideNavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = remember { HomeNavigationState() }
    val screen = navigator.lastItem
    val onHome = screen is HomeScreen
    val configuration = state.configuration
    val homeTab = state.selectedTab
    // Statistics is a screen of its own rather than a home tab, so the dock marks it while it's open
    val onStatistics = screen is StatsScreen && configuration?.enabledTabs?.contains(HomeScreenTabs.Statistics) == true
    val selectedTab = if (onStatistics) HomeScreenTabs.Statistics else homeTab
    CompositionLocalProvider(LocalHomeNavigation provides state) {
        FloatingDockHost(
            dockVisible = currentWindowLayout().usesSideNavigation &&
                configuration != null &&
                selectedTab != null &&
                screen !is FullWindowScreen &&
                !(onHome && state.hiddenByTab),
            placement = DockPlacement.Side,
            dock = {
                if (configuration != null && selectedTab != null) {
                    HomeNavigationBar(
                        primaryTabs = configuration.primaryTabs,
                        overflowTabs = configuration.overflowTabs,
                        selectedTab = selectedTab,
                        onClick = { tab ->
                            if (onHome) {
                                state.requestedTab = tab
                            } else if (!(onStatistics && tab == HomeScreenTabs.Statistics)) {
                                navigator.popUntilRoot()
                                if (tab != homeTab) state.requestedTab = tab
                            }
                        },
                        placement = DockPlacement.Side,
                    )
                }
            },
            modifier = modifier,
            content = content,
        )
    }
}
