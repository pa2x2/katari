package mihon.entry.interactions.manga.reader.settings

import mihon.entry.interactions.reader.settings.MangaReaderColorFilterDefinitions
import mihon.entry.interactions.reader.settings.MangaReaderSettings

internal fun mangaReaderColorFilterDefinitions(preferences: MangaReaderPreferences): MangaReaderColorFilterDefinitions {
    return MangaReaderColorFilterDefinitions(
        customBrightness = entryBoolean("custom_brightness", preferences.customBrightness),
        customBrightnessValue = entryInt("custom_brightness_value", preferences.customBrightnessValue),
        colorFilter = entryBoolean("color_filter", preferences.colorFilter),
        colorFilterValue = entryInt("color_filter_value", preferences.colorFilterValue),
        colorFilterMode = entryInt(
            "color_filter_mode",
            preferences.colorFilterMode,
            validate = { it in MangaReaderSettings.ColorFilterMode.indices },
        ),
        grayscale = entryBoolean("grayscale", preferences.grayscale),
        invertedColors = entryBoolean("inverted_colors", preferences.invertedColors),
    )
}
