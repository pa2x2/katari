package mihon.entry.interactions.manga.navigation

import android.content.Context
import mihon.entry.interactions.manga.runtime.requireManga
import mihon.entry.interactions.manga.state.lastReadAt
import mihon.entry.interactions.manga.state.pageIndex
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryOpenOptions
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.service.sortedForReading

internal class MangaContinueProcessor(
    private val openProcessor: MangaOpenProcessor,
) : EntryContinueProcessor {

    override fun findNext(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryChapter? {
        entry.requireManga()
        val chapterById = chapters.associateBy { it.id }
        return progressStates
            .asSequence()
            .filter { !it.completed && it.pageIndex > 0L }
            .sortedByDescending { it.lastReadAt }
            .mapNotNull { it.chapterId?.let(chapterById::get) }
            .firstOrNull()
            ?: chapters.sortedForReading(entry).firstOrNull { !it.read }
    }

    override fun open(context: Context, entry: Entry, chapter: EntryChapter) {
        entry.requireManga()
        openProcessor.open(context, entry, chapter, EntryOpenOptions())
    }
}
