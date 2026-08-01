package eu.kanade.presentation.more.settings.screen

import mihon.entry.interactions.media.EntryViewerSettingsScreenProjectionResolver

/** The application composition root for genuine Viewer Settings screen implementations. */
internal fun productionEntryViewerSettingsScreenProjectionResolver(): EntryViewerSettingsScreenProjectionResolver {
    return EntryViewerSettingsScreenProjectionResolver { _ ->
        listOf(
            SettingsMangaReaderScreen,
            SettingsBookDocumentReaderScreen,
            SettingsAnimePlayerScreen,
        )
    }
}
