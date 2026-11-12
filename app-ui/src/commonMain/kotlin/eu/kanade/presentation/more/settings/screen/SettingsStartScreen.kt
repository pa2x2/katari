package eu.kanade.presentation.more.settings.screen

import app.katari.sync.ui.page.SyncScreen
import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen

internal fun resolveSettingsStartScreen(
    destination: SettingsScreen.Destination?,
    twoPane: Boolean,
    viewerSettingsScreen: Screen? = null,
): Screen {
    val requestedScreen = when (destination) {
        SettingsScreen.Destination.About -> AboutScreen()
        SettingsScreen.Destination.DataAndStorage -> SettingsDataScreen()
        SettingsScreen.Destination.Translation -> SettingsTranslationScreen()
        SettingsScreen.Destination.TextRecognition -> SettingsTextRecognitionScreen()
        SettingsScreen.Destination.Readers -> viewerSettingsScreen ?: SettingsReaderScreen()
        SettingsScreen.Destination.LibraryUpdates -> SettingsLibraryUpdatesScreen()
        SettingsScreen.Destination.Profiles -> SettingsProfilesScreen()
        SettingsScreen.Destination.Sync -> SyncScreen()
        null -> if (twoPane) SettingsGeneralScreen() else SettingsMainScreen()
    }

    return requestedScreen
}
