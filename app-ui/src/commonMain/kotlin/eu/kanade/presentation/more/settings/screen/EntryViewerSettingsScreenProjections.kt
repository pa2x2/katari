package eu.kanade.presentation.more.settings.screen

import app.katari.entry.interactions.media.EntryViewerSettingsScreenProjectionResolver

/** The application composition root for genuine Viewer Settings screen implementations. */
fun productionEntryViewerSettingsScreenProjectionResolver(): EntryViewerSettingsScreenProjectionResolver {
    return EntryViewerSettingsScreenProjectionResolver { surfaceIds ->
        listOf(
            SettingsMangaReaderScreen(),
            SettingsBookDocumentReaderScreen(),
            SettingsAnimePlayerScreen(),
        ).filter { screen -> screen.surfaceId in surfaceIds }
    }
}
