package mihon.entry.interactions.manga.navigation

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.manga.runtime.requireManga
import mihon.entry.interactions.manga.state.lastReadAt
import mihon.entry.interactions.manga.state.pageIndex
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryOpenOptions
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.sortedForReading

internal class MangaContinueProcessor(
    private val getEntryWithChapters: GetEntryWithChapters,
    private val entryProgressRepository: EntryProgressRepository,
    private val openProcessor: MangaOpenProcessor,
) : EntryContinueProcessor {
    override val type: EntryType = EntryType.MANGA

    override suspend fun findNext(entry: Entry): EntryChapter? {
        entry.requireManga()
        val chapters = getEntryWithChapters.awaitChapters(entry)
        val progressStates = chapters
            .map { it.entryId }
            .distinct()
            .flatMap { entryId -> entryProgressRepository.getByEntryId(entryId) }
        return findNext(entry, chapters, progressStates)
    }

    override suspend fun findNext(
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
