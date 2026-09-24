package mihon.entry.interactions.reader.settings

import mihon.entry.viewer.settings.ViewerSettingDefinition

/**
 * Paged viewer settings owned by the manga reader surface.
 */
class MangaReaderPagerDefinitions(
    val navigationMode: ViewerSettingDefinition<Int>,
    val navigationInverted: ViewerSettingDefinition<MangaReaderSettings.TappingInvertMode>,
    val imageScaleType: ViewerSettingDefinition<Int>,
    val zoomStart: ViewerSettingDefinition<Int>,
    val cropBorders: ViewerSettingDefinition<Boolean>,
    val landscapeZoom: ViewerSettingDefinition<Boolean>,
    val navigateToPan: ViewerSettingDefinition<Boolean>,
    val dualPageSplit: ViewerSettingDefinition<Boolean>,
    val dualPageInvert: ViewerSettingDefinition<Boolean>,
    val dualPageRotateToFit: ViewerSettingDefinition<Boolean>,
    val dualPageRotateToFitInvert: ViewerSettingDefinition<Boolean>,
) {
    val all: List<ViewerSettingDefinition<*>> = listOf(
        navigationMode,
        navigationInverted,
        imageScaleType,
        zoomStart,
        cropBorders,
        landscapeZoom,
        navigateToPan,
        dualPageSplit,
        dualPageInvert,
        dualPageRotateToFit,
        dualPageRotateToFitInvert,
    )
}
