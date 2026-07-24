package eu.kanade.presentation.more.settings.screen

import mihon.entry.interactions.EntryViewerSettingsScreenProjectionResolver

/** The application composition root for genuine Viewer Settings screen implementations. */
internal fun productionEntryViewerSettingsScreenProjectionResolver(): EntryViewerSettingsScreenProjectionResolver {
    return EntryViewerSettingsScreenProjectionResolver { _ ->
        listOf(
            SettingsMangaReaderScreen,
            SettingsAnimePlayerScreen,
            SettingsReadiumEpubReaderScreen,
            SettingsHtmlProseReaderScreen,
        )
    }
}
