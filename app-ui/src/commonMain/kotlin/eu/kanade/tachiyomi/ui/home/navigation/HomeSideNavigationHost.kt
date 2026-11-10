package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import app.katari.core.common.HomeScreenTabs
import app.katari.presentation.kit.navigation.DockPlacement
import app.katari.presentation.kit.navigation.DockedScreen
import app.katari.presentation.kit.navigation.FloatingDockHost
import app.katari.presentation.kit.sheet.popoverAnchor
import app.katari.presentation.kit.window.currentWindowLayout
import app.katari.profiles.ui.switcher.LocalProfileSwitcher
import app.katari.ui.telemetry.LocalShownEntryTypes
import app.katari.ui.telemetry.ShownEntryTypes
import app.katari.ui.telemetry.ShownScreenReport
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.util.DefaultNavigatorScreenTransition
import eu.kanade.tachiyomi.ui.home.HomeScreen

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
    val profileSwitcher = LocalProfileSwitcher.current
    val shownEntryTypes = remember { ShownEntryTypes() }
    CompositionLocalProvider(LocalHomeNavigation provides state, LocalShownEntryTypes provides shownEntryTypes) {
        HomeShortcuts(navigator = navigator, state = state)
        ShownScreenReport(navigator = navigator, entryTypes = shownEntryTypes, homeTab = { state.selectedTab })
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
                    itemModifier = { tab ->
                        // The switcher opened from this destination opens beside it
                        val switcher = profileSwitcher
                        if (tab == HomeScreenTabs.Profiles && switcher != null) {
                            Modifier.popoverAnchor(switcher.dockAnchor)
                        } else {
                            Modifier
                        }
                    },
                    actions = listOfNotNull(rememberDockSyncAction(navigator)),
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
