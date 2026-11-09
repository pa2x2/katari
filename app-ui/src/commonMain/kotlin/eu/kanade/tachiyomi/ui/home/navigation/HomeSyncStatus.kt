package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Composable
import app.katari.core.common.HomeScreenTabs
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.window.currentWindowLayout
import app.katari.sync.ui.status.rememberSyncStatusAction
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.setting.SettingsScreen

/**
 * Sync's status icon in the app bar of [destination], or null where it doesn't show there. It shows once per window:
 * a window with the side dock has it at the dock's foot, beside every screen ([rememberDockSyncAction]); a narrower one
 * in the bars of the destinations sync changes most, and on the tab the home opens on when the home shows none of
 * them, so it is never more than a tap on the dock away. It opens the Sync page over the screen.
 */
@Composable
fun rememberBarSyncAction(destination: HomeScreenTabs, navigator: Navigator): AppBar.Action? {
    if (currentWindowLayout().usesSideNavigation) return null
    val tabSettings = LocalHomeNavigation.current.tabSettings
    val shows = destination in BarDestinations ||
        (destination == tabSettings.launchTab && tabSettings.contentTabs.none { it in BarDestinations })
    return if (shows) {
        rememberSyncStatusAction {
            navigator.push(SettingsScreen(SettingsScreen.Destination.Sync))
        }
    } else {
        null
    }
}

/** Sync's status icon at the foot of the side dock. */
@Composable
internal fun rememberDockSyncAction(navigator: Navigator): AppBar.Action? =
    rememberSyncStatusAction { openSyncFromRoot(navigator) }

/** Opens the Sync page from the home's root, as the dock's destinations open, for what shows beside every screen. */
internal fun openSyncFromRoot(navigator: Navigator) {
    navigator.popUntilRoot()
    navigator.push(SettingsScreen(SettingsScreen.Destination.Sync))
}

private val BarDestinations = setOf(
    HomeScreenTabs.Library,
    HomeScreenTabs.Updates,
    HomeScreenTabs.History,
    HomeScreenTabs.Statistics,
)
