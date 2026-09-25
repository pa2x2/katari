package mihon.entry.interactions.manga.reader.settings

import mihon.entry.interactions.reader.settings.MangaReaderEInkDefinitions
import mihon.entry.interactions.reader.settings.MangaReaderSettings

internal fun mangaReaderEInkDefinitions(preferences: MangaReaderPreferences): MangaReaderEInkDefinitions {
    return MangaReaderEInkDefinitions(
        flashOnPageChange = entryBoolean("flash_on_page_change", preferences.flashOnPageChange),
        flashDurationMillis = entryInt(
            "flash_duration_millis",
            preferences.flashDurationMillis,
            validate = { it >= 0 },
        ),
        flashPageInterval = entryInt(
            "flash_page_interval",
            preferences.flashPageInterval,
            validate = { it > 0 },
        ),
        flashColor = entryValue(
            "flash_color",
            preferences.flashColor,
            enumCodec(MangaReaderSettings.FlashColor.entries),
        ),
    )
}
