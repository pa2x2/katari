package mihon.entry.interactions.reader.settings

import mihon.entry.viewer.settings.ViewerSettingDefinition

/**
 * Webtoon viewer settings owned by the manga reader surface.
 */
class MangaReaderWebtoonDefinitions(
    val navigationMode: ViewerSettingDefinition<Int>,
    val navigationInverted: ViewerSettingDefinition<MangaReaderSettings.TappingInvertMode>,
    val sidePadding: ViewerSettingDefinition<Int>,
    val hideThreshold: ViewerSettingDefinition<MangaReaderSettings.ReaderHideThreshold>,
    val cropBorders: ViewerSettingDefinition<Boolean>,
    val dualPageSplit: ViewerSettingDefinition<Boolean>,
    val dualPageInvert: ViewerSettingDefinition<Boolean>,
    val dualPageRotateToFit: ViewerSettingDefinition<Boolean>,
    val dualPageRotateToFitInvert: ViewerSettingDefinition<Boolean>,
    val doubleTapZoom: ViewerSettingDefinition<Boolean>,
    val disableZoomOut: ViewerSettingDefinition<Boolean>,
) {
    val all: List<ViewerSettingDefinition<*>> = listOf(
        navigationMode,
        navigationInverted,
        sidePadding,
        hideThreshold,
        cropBorders,
        dualPageSplit,
        dualPageInvert,
        dualPageRotateToFit,
        dualPageRotateToFitInvert,
        doubleTapZoom,
        disableZoomOut,
    )
}
