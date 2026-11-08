package mihon.entry.interactions.manga.reader.settings

import mihon.entry.interactions.reader.settings.MangaReaderPageTextDefinitions

internal fun mangaReaderPageTextDefinitions(preferences: MangaReaderPreferences): MangaReaderPageTextDefinitions {
    return MangaReaderPageTextDefinitions(
        translationOverlay = entryBoolean("page_text_translation_overlay", preferences.pageTextTranslationOverlay),
        processAheadOnlyOnUnmeteredNetwork = profileBoolean(
            "page_text_process_ahead_unmetered_only",
            preferences.pageTextProcessAheadOnlyOnUnmeteredNetwork,
        ),
        processAheadOnlyWhileCharging = profileBoolean(
            "page_text_process_ahead_charging_only",
            preferences.pageTextProcessAheadOnlyWhileCharging,
        ),
    )
}
