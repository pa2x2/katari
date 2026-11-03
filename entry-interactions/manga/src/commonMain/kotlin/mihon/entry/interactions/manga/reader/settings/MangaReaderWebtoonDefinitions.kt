package mihon.entry.interactions.manga.reader.settings

import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.MangaReaderWebtoonDefinitions

internal fun mangaReaderWebtoonDefinitions(preferences: MangaReaderPreferences): MangaReaderWebtoonDefinitions {
    return MangaReaderWebtoonDefinitions(
        navigationMode = entryInt(
            "navigation_mode_webtoon",
            preferences.navigationModeWebtoon,
            validate = { it in MangaReaderSettings.TapZones.indices },
        ),
        navigationInverted = entryValue(
            "webtoon_navigation_inverted",
            preferences.webtoonNavInverted,
            enumCodec(MangaReaderSettings.TappingInvertMode.entries),
        ),
        sidePadding = entryInt(
            "webtoon_side_padding",
            preferences.webtoonSidePadding,
            validate = { it in MangaReaderSettings.WEBTOON_PADDING_MIN..MangaReaderSettings.WEBTOON_PADDING_MAX },
        ),
        hideThreshold = entryValue(
            "reader_hide_threshold",
            preferences.readerHideThreshold,
            enumCodec(MangaReaderSettings.ReaderHideThreshold.entries),
        ),
        cropBorders = entryBoolean("crop_borders_webtoon", preferences.cropBordersWebtoon),
        dualPageSplit = entryBoolean("dual_page_split_webtoon", preferences.dualPageSplitWebtoon),
        dualPageInvert = entryBoolean("dual_page_invert_webtoon", preferences.dualPageInvertWebtoon),
        dualPageRotateToFit = entryBoolean("dual_page_rotate_to_fit_webtoon", preferences.dualPageRotateToFitWebtoon),
        dualPageRotateToFitInvert = entryBoolean(
            "dual_page_rotate_to_fit_invert_webtoon",
            preferences.dualPageRotateToFitInvertWebtoon,
        ),
        doubleTapZoom = entryBoolean("webtoon_double_tap_zoom", preferences.webtoonDoubleTapZoomEnabled),
        disableZoomOut = entryBoolean("webtoon_disable_zoom_out", preferences.webtoonDisableZoomOut),
    )
}
