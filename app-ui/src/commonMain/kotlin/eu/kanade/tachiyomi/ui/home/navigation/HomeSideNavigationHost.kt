package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.home.HomeScreen
import mihon.presentation.kit.navigation.DockPlacement
import mihon.presentation.kit.navigation.FloatingDockHost
import mihon.presentation.kit.window.currentWindowLayout

/**
 * Lays the home destinations beside every screen of [navigator] in a wide window, not only beside the home tabs, so
 * any screen is one tap from them, and gives every window their keyboard shortcuts (see [HomeShortcuts]).
 */
@Composable
fun HomeSideNavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tabSettings = rememberUpdatedState(rememberHomeTabSettings())
    val state = remember(tabSettings) { HomeNavigationState(tabSettings) }
    val screen = navigator.lastItem
    val onHome = screen is HomeScreen
    val configuration = state.configuration
    val selectedTab = state.markedTab(navigator)
    CompositionLocalProvider(LocalHomeNavigation provides state) {
        HomeShortcuts(navigator = navigator, state = state)
        FloatingDockHost(
            dockVisible = currentWindowLayout().usesSideNavigation &&
                screen !is FullWindowScreen &&
                !(onHome && state.hiddenByTab),
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
            content = content,
        )
    }
}
