package mihon.entry.interactions.reader.settings

import mihon.entry.viewer.settings.ViewerSettingDefinition

/**
 * Brightness and color filter settings owned by the manga reader surface.
 */
class MangaReaderColorFilterDefinitions(
    val customBrightness: ViewerSettingDefinition<Boolean>,
    val customBrightnessValue: ViewerSettingDefinition<Int>,
    val colorFilter: ViewerSettingDefinition<Boolean>,
    val colorFilterValue: ViewerSettingDefinition<Int>,
    val colorFilterMode: ViewerSettingDefinition<Int>,
    val grayscale: ViewerSettingDefinition<Boolean>,
    val invertedColors: ViewerSettingDefinition<Boolean>,
) {
    val all: List<ViewerSettingDefinition<*>> = listOf(
        customBrightness,
        customBrightnessValue,
        colorFilter,
        colorFilterValue,
        colorFilterMode,
        grayscale,
        invertedColors,
    )
}
