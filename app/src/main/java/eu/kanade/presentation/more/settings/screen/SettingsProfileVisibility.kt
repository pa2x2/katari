package eu.kanade.presentation.more.settings.screen

import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen

internal fun isSettingsScreenVisible(
    screen: Screen,
): Boolean = true

internal fun resolveSettingsStartScreen(
    destination: SettingsScreen.Destination?,
    twoPane: Boolean,
): Screen {
    val requestedScreen = when (destination) {
        SettingsScreen.Destination.About -> AboutScreen
        SettingsScreen.Destination.DataAndStorage -> SettingsDataScreen
        SettingsScreen.Destination.Tracking -> SettingsTrackingScreen
        null -> if (twoPane) SettingsAppearanceScreen else SettingsMainScreen
    }

    return requestedScreen
}
