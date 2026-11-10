package eu.kanade.presentation.more.settings.screen

import app.katari.ui.di.AppUiGraph

/**
 * Every Settings screen this platform offers, the main list's and the viewers' own: what settings search finds, and
 * where the settings of a sync area are listed from, so a setting shows wherever its screen does.
 */
fun settingsScreens(graph: AppUiGraph): List<SearchableSettings> =
    (mainSettingsScreens() + viewerProviderSettingsScreens(graph.entryViewerSettingsFeature))
        .filter { it.isOffered(graph) }

private fun mainSettingsScreens() = listOf(
    SettingsAppearanceScreen(),
    SettingsLibraryScreen(),
    SettingsLibraryUpdatesScreen(),
    SettingsReaderScreen(),
    SettingsPlayerScreen(),
    SettingsDownloadScreen(),
    SettingsTranslationScreen(),
    SettingsTtsScreen(),
    SettingsTextRecognitionScreen(),
    SettingsBrowseScreen(),
    SettingsDataScreen(),
    SettingsSecurityScreen(),
    SettingsNotificationsScreen(),
    SettingsProfilesScreen(),
    SettingsAdvancedScreen(),
)
