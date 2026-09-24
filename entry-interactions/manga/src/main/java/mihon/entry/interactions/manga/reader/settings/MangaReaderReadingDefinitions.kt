package mihon.entry.interactions.manga.reader.settings

import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.MangaReaderReadingDefinitions
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.settings.shared.StandardReaderSharedSettingIds

internal fun mangaReaderReadingDefinitions(
    preferences: MangaReaderPreferences,
    chapterPreparationPreferences: ReaderChapterPreparationPreferences,
): MangaReaderReadingDefinitions {
    return MangaReaderReadingDefinitions(
        readingMode = entryInt(
            key = MangaReaderSettings.READING_MODE_KEY,
            preference = preferences.defaultReadingMode,
            validate = { value -> ReadingMode.entries.any { it.flagValue == value } },
        ),
        orientation = entryInt(
            key = MangaReaderSettings.ORIENTATION_KEY,
            preference = preferences.defaultOrientationType,
            validate = { value -> ReaderOrientation.entries.any { it.flagValue == value } },
        ),
        chapterTransition = entryValue(
            key = MangaReaderSettings.CHAPTER_TRANSITION_PREFERENCE_KEY,
            preference = preferences.chapterTransitionMode,
            codec = enumCodec(ChapterTransitionMode.entries),
        ),
        prepareNextChapter = entryBoolean(
            key = StandardReaderSharedSettingIds.NextChapterPreparation.value,
            preference = chapterPreparationPreferences.prepareNextChapter(MangaReaderSettings.PROVIDER_ID),
        ),
        pageTransitions = entryBoolean("page_transitions", preferences.pageTransitions),
        readWithLongTap = entryBoolean("read_with_long_tap", preferences.readWithLongTap),
        skipRead = profileBoolean("skip_read", preferences.skipRead),
        skipFiltered = profileBoolean("skip_filtered", preferences.skipFiltered),
        skipDuplicate = profileBoolean("skip_duplicate", preferences.skipDupe),
        folderPerManga = profileBoolean("folder_per_manga", preferences.folderPerManga),
    )
}
