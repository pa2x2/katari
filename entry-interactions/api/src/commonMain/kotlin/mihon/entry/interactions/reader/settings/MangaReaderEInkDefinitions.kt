package mihon.entry.interactions.reader.settings

import mihon.entry.viewer.settings.ViewerSettingDefinition

/**
 * E-ink refresh settings owned by the manga reader surface.
 */
class MangaReaderEInkDefinitions(
    val flashOnPageChange: ViewerSettingDefinition<Boolean>,
    val flashDurationMillis: ViewerSettingDefinition<Int>,
    val flashPageInterval: ViewerSettingDefinition<Int>,
    val flashColor: ViewerSettingDefinition<MangaReaderSettings.FlashColor>,
) {
    val all: List<ViewerSettingDefinition<*>> = listOf(
        flashOnPageChange,
        flashDurationMillis,
        flashPageInterval,
        flashColor,
    )
}
