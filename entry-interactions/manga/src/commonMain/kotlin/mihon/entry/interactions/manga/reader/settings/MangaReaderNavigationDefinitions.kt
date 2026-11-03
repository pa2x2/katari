package mihon.entry.interactions.manga.reader.settings

import mihon.entry.interactions.reader.settings.MangaReaderNavigationDefinitions
import mihon.entry.interactions.reader.settings.MangaReaderSettings

internal fun mangaReaderNavigationDefinitions(preferences: MangaReaderPreferences): MangaReaderNavigationDefinitions {
    return MangaReaderNavigationDefinitions(
        verticalNavigator = entryValue(
            "vertical_navigator_modes",
            preferences.verticalNavigator,
            readingModeSetCodec(),
        ),
        verticalNavigatorOnLeft = entryBoolean("vertical_navigator_on_left", preferences.verticalNavigatorOnLeft),
        verticalNavigatorHeight = entryInt(
            "vertical_navigator_height",
            preferences.verticalNavigatorHeight,
            validate = { it in 0..100 },
        ),
        volumeKeys = profileBoolean("read_with_volume_keys", preferences.readWithVolumeKeys),
        volumeKeysInverted = profileBoolean(
            "read_with_volume_keys_inverted",
            preferences.readWithVolumeKeysInverted,
        ),
        autoScrollEnabled = profileBoolean("auto_scroll_enabled", preferences.autoScrollEnabled),
        autoScrollSpeed = profileInt(
            "auto_scroll_speed",
            preferences.autoScrollSpeed,
            validate = { it in MangaReaderSettings.AUTO_SCROLL_SPEED_RANGE },
        ),
    )
}
