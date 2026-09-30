package mihon.entry.interactions.reader.settings

import mihon.entry.viewer.settings.ViewerSettingDefinition

/**
 * Display and chrome settings owned by the manga reader surface.
 */
class MangaReaderDisplayDefinitions(
    val readerTheme: ViewerSettingDefinition<Int>,
    val showPageNumber: ViewerSettingDefinition<Boolean>,
    val showStatusBar: ViewerSettingDefinition<Boolean>,
    val showNavigationBar: ViewerSettingDefinition<Boolean>,
    val drawUnderCutout: ViewerSettingDefinition<Boolean>,
    val keepScreenOn: ViewerSettingDefinition<Boolean>,
    val showReadingMode: ViewerSettingDefinition<Boolean>,
    val showNavigationOverlayOnStart: ViewerSettingDefinition<Boolean>,
    val showNavigationOverlayNewUser: ViewerSettingDefinition<Boolean>,
    val doubleTapAnimSpeed: ViewerSettingDefinition<Int>,
) {
    val all: List<ViewerSettingDefinition<*>> = listOf(
        readerTheme,
        showPageNumber,
        showStatusBar,
        showNavigationBar,
        drawUnderCutout,
        keepScreenOn,
        showReadingMode,
        showNavigationOverlayOnStart,
        showNavigationOverlayNewUser,
        doubleTapAnimSpeed,
    )
}
