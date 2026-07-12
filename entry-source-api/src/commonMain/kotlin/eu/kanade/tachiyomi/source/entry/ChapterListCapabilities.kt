package eu.kanade.tachiyomi.source.entry

/**
 * Source capability for chapter lists that may legitimately be empty.
 *
 * Sources without this capability are treated as failed when they return an empty list so a
 * transient parser failure cannot delete every stored chapter.
 */
interface EmptyChapterListSource

/**
 * Source capability for chapter refreshes that need the currently stored chapter list.
 */
interface IncrementalChapterSource {
    suspend fun getChapterList(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
    ): List<SEntryChapter>
}

/** Source capability requesting host-side chapter-number recognition for unknown numbers. */
interface ChapterNumberRecognitionSource
