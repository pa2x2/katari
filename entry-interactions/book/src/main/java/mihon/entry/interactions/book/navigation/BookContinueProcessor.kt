package mihon.entry.interactions.book.navigation

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.book.runtime.requireBook
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryOpenOptions
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.sortedForReading

internal class BookContinueProcessor(
    private val getEntryWithChapters: GetEntryWithChapters,
    private val entryProgressRepository: EntryProgressRepository,
    private val openProcessor: BookOpenProcessor,
) : EntryContinueProcessor {
    override val type = EntryType.BOOK

    override suspend fun findNext(entry: Entry): EntryChapter? {
        entry.requireBook()
        val chapters = getEntryWithChapters.awaitChapters(entry)
        val progressStates = chapters
            .map(EntryChapter::entryId)
            .distinct()
            .flatMap { entryProgressRepository.getByEntryId(it) }
        return findNext(entry, chapters, progressStates)
    }

    override suspend fun findNext(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryChapter? {
        entry.requireBook()
        val sortedChapters = chapters.sortedForReading(entry)
        val chapterById = sortedChapters.associateBy(EntryChapter::id)
        return progressStates
            .asSequence()
            .filter { !it.completed && !it.locator.isEmpty }
            .sortedByDescending { it.locatorUpdatedAt }
            .mapNotNull { it.chapterId?.let(chapterById::get) }
            .firstOrNull()
            ?: sortedChapters.firstOrNull { !it.read }
    }

    override fun open(context: Context, entry: Entry, chapter: EntryChapter) {
        entry.requireBook()
        openProcessor.open(context, entry, chapter, EntryOpenOptions())
    }
}
