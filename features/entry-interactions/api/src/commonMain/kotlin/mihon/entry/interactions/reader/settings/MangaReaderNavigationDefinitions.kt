package mihon.entry.interactions.reader.settings

import mihon.entry.viewer.settings.ViewerSettingDefinition

/**
 * Chrome navigation and auto-scroll settings owned by the manga reader surface.
 */
class MangaReaderNavigationDefinitions(
    val verticalNavigator: ViewerSettingDefinition<Set<ReadingMode>>,
    val verticalNavigatorOnLeft: ViewerSettingDefinition<Boolean>,
    val verticalNavigatorHeight: ViewerSettingDefinition<Int>,
    val volumeKeys: ViewerSettingDefinition<Boolean>,
    val volumeKeysInverted: ViewerSettingDefinition<Boolean>,
    val autoScrollEnabled: ViewerSettingDefinition<Boolean>,
    val autoScrollSpeed: ViewerSettingDefinition<Int>,
) {
    val all: List<ViewerSettingDefinition<*>> = listOf(
        verticalNavigator,
        verticalNavigatorOnLeft,
        verticalNavigatorHeight,
        volumeKeys,
        volumeKeysInverted,
        autoScrollEnabled,
        autoScrollSpeed,
    )
}
