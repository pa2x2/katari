package mihon.entry.interactions.manga.reader.settings

import mihon.entry.interactions.reader.settings.MangaReaderDisplayDefinitions

internal fun mangaReaderDisplayDefinitions(preferences: MangaReaderPreferences): MangaReaderDisplayDefinitions {
    return MangaReaderDisplayDefinitions(
        readerTheme = entryInt("reader_theme", preferences.readerTheme),
        showPageNumber = entryBoolean("show_page_number", preferences.showPageNumber),
        fullscreen = entryBoolean("fullscreen", preferences.fullscreen),
        drawUnderCutout = entryBoolean("draw_under_cutout", preferences.drawUnderCutout),
        keepScreenOn = entryBoolean("keep_screen_on", preferences.keepScreenOn),
        showReadingMode = profileBoolean("show_reading_mode", preferences.showReadingMode),
        showNavigationOverlayOnStart = profileBoolean(
            "show_navigation_overlay_on_start",
            preferences.showNavigationOverlayOnStart,
        ),
        showNavigationOverlayNewUser = profileBoolean(
            "show_navigation_overlay_new_user",
            preferences.showNavigationOverlayNewUser,
        ),
        doubleTapAnimSpeed = profileInt(
            "double_tap_animation_millis",
            preferences.doubleTapAnimSpeed,
            validate = { it >= 0 },
        ),
    )
}
