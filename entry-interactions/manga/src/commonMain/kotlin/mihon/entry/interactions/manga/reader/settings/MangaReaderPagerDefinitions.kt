package mihon.entry.interactions.manga.reader.settings

import mihon.entry.interactions.reader.settings.MangaReaderPagerDefinitions
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReaderPageLayout

internal fun mangaReaderPagerDefinitions(preferences: MangaReaderPreferences): MangaReaderPagerDefinitions {
    return MangaReaderPagerDefinitions(
        navigationMode = entryInt(
            "navigation_mode_pager",
            preferences.navigationModePager,
            validate = { it in MangaReaderSettings.TapZones.indices },
        ),
        navigationInverted = entryValue(
            "pager_navigation_inverted",
            preferences.pagerNavInverted,
            enumCodec(MangaReaderSettings.TappingInvertMode.entries),
        ),
        imageScaleType = entryInt(
            "image_scale_type",
            preferences.imageScaleType,
            validate = { it in MangaReaderSettings.ImageScaleType.indices },
        ),
        zoomStart = entryInt(
            "zoom_start",
            preferences.zoomStart,
            validate = { it in MangaReaderSettings.ZoomStart.indices },
        ),
        cropBorders = entryBoolean("crop_borders", preferences.cropBorders),
        landscapeZoom = entryBoolean("landscape_zoom", preferences.landscapeZoom),
        navigateToPan = entryBoolean("navigate_to_pan", preferences.navigateToPan),
        pageLayout = entryValue(
            "page_layout_paged",
            preferences.pagerPageLayout,
            enumCodec(ReaderPageLayout.entries),
        ),
        dualPageSplit = entryBoolean("dual_page_split_paged", preferences.dualPageSplitPaged),
        dualPageInvert = entryBoolean("dual_page_invert_paged", preferences.dualPageInvertPaged),
        dualPageRotateToFit = entryBoolean("dual_page_rotate_to_fit", preferences.dualPageRotateToFit),
        dualPageRotateToFitInvert = entryBoolean(
            "dual_page_rotate_to_fit_invert",
            preferences.dualPageRotateToFitInvert,
        ),
    )
}
