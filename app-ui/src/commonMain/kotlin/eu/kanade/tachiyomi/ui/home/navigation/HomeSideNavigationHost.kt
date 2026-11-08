package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.util.DefaultNavigatorScreenTransition
import eu.kanade.tachiyomi.ui.home.HomeScreen
import mihon.presentation.kit.navigation.DockPlacement
import mihon.presentation.kit.navigation.DockedScreen
import mihon.presentation.kit.navigation.FloatingDockHost
import mihon.presentation.kit.window.currentWindowLayout

/**
 * Shows the screens of [navigator] with the home destinations beside every one of them in a wide window, not only beside
 * the home tabs, so any screen is one tap from them, and gives every window their keyboard shortcuts (see
 * [HomeShortcuts]). Each screen keeps the dock's room for itself through a transition, see [DockedScreen].
 */
@Composable
fun HomeSideNavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val tabSettings = rememberUpdatedState(rememberHomeTabSettings())
    val state = remember(tabSettings) { HomeNavigationState(tabSettings) }
    val usesSideNavigation = currentWindowLayout().usesSideNavigation
    val besideDock = { screen: Screen ->
        usesSideNavigation && screen !is FullWindowScreen && !(screen is HomeScreen && state.hiddenByTab)
    }
    val configuration = state.configuration
    val selectedTab = state.markedTab(navigator)
    CompositionLocalProvider(LocalHomeNavigation provides state) {
        HomeShortcuts(navigator = navigator, state = state)
        FloatingDockHost(
            dockVisible = besideDock(navigator.lastItem),
            placement = DockPlacement.Side,
            dock = {
                HomeNavigationBar(
                    primaryTabs = configuration.primaryTabs,
                    overflowTabs = configuration.overflowTabs,
                    selectedTab = selectedTab,
                    onClick = { tab -> state.open(tab, navigator) },
                    placement = DockPlacement.Side,
                )
            },
            modifier = modifier,
        ) {
            DefaultNavigatorScreenTransition(navigator = navigator) { screen ->
                DockedScreen(besideDock = besideDock(screen)) { screen.Content() }
            }
        }
    }
}
