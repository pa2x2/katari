package mihon.entry.interactions.reader.settings

import mihon.entry.viewer.settings.ViewerSettingDefinition

/**
 * Reading mode, chapter flow, and library-facing reading settings owned by the manga reader surface.
 */
class MangaReaderReadingDefinitions(
    val readingMode: ViewerSettingDefinition<Int>,
    val orientation: ViewerSettingDefinition<Int>,
    val chapterTransition: ViewerSettingDefinition<ChapterTransitionMode>,
    val prepareNextChapter: ViewerSettingDefinition<Boolean>,
    val pageTransitions: ViewerSettingDefinition<Boolean>,
    val readWithLongTap: ViewerSettingDefinition<Boolean>,
    val skipRead: ViewerSettingDefinition<Boolean>,
    val skipFiltered: ViewerSettingDefinition<Boolean>,
    val skipDuplicate: ViewerSettingDefinition<Boolean>,
    val folderPerManga: ViewerSettingDefinition<Boolean>,
) {
    val all: List<ViewerSettingDefinition<*>> = listOf(
        readingMode,
        orientation,
        chapterTransition,
        prepareNextChapter,
        pageTransitions,
        readWithLongTap,
        skipRead,
        skipFiltered,
        skipDuplicate,
        folderPerManga,
    )
}
