package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.input.key.Key
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import mihon.presentation.kit.shortcut.KeyboardShortcut
import mihon.presentation.kit.shortcut.KeyboardShortcutHandler
import mihon.presentation.kit.shortcut.ShortcutGroup
import mihon.presentation.kit.shortcut.ShortcutKeys
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Ctrl+1 to Ctrl+5 open the home destinations in the order the dock shows them, its menu's after its own, and Ctrl+,
 * opens the settings, from whichever screen is on top of [navigator].
 */
@Composable
internal fun HomeShortcuts(navigator: Navigator, state: HomeNavigationState) {
    val destinations = state.configuration?.enabledTabs.orEmpty().take(DestinationKeys.size)
    destinations.forEachIndexed { index, tab ->
        key(index) {
            val (key, keyName) = DestinationKeys[index]
            KeyboardShortcutHandler(
                KeyboardShortcut(
                    id = "navigation.destination.$index",
                    label = homeNavigationTitle(tab),
                    keys = listOf(ShortcutKeys.Chord(key, keyName, ctrl = true)),
                    group = ShortcutGroup.Navigation,
                ),
            ) {
                // The dock's tap on the open tab acts on it, such as More opening the settings; a key only goes there
                val alreadyOpen = navigator.lastItem is HomeScreen && state.selectedTab == tab
                if (!alreadyOpen) state.open(tab, navigator)
            }
        }
    }
    KeyboardShortcutHandler(
        KeyboardShortcut(
            id = "navigation.settings",
            label = stringResource(MR.strings.label_settings),
            keys = listOf(ShortcutKeys.Chord(Key.Comma, ",", ctrl = true)),
            group = ShortcutGroup.Navigation,
        ),
    ) {
        if (navigator.lastItem !is SettingsScreen) navigator.push(SettingsScreen())
    }
}

private val DestinationKeys = listOf(
    Key.One to "1",
    Key.Two to "2",
    Key.Three to "3",
    Key.Four to "4",
    Key.Five to "5",
)
