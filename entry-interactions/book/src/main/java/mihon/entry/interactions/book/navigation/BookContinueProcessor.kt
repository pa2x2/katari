package mihon.entry.interactions.book

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryContinueProcessor
import mihon.entry.interactions.EntryOpenOptions
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
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
        val chapters = getEntryWithChapters.awaitChapters(entry).sortedForReading(entry)
        val chapterById = chapters.associateBy(EntryChapter::id)
        return chapters
            .map(EntryChapter::entryId)
            .distinct()
            .flatMap { entryProgressRepository.getByEntryId(it) }
            .asSequence()
            .filter { !it.completed && !it.locator.isEmpty }
            .sortedByDescending { it.locatorUpdatedAt }
            .mapNotNull { it.chapterId?.let(chapterById::get) }
            .firstOrNull()
            ?: chapters.firstOrNull { !it.read }
    }

    override fun open(context: Context, entry: Entry, chapter: EntryChapter) {
        entry.requireBook()
        openProcessor.open(context, entry, chapter, EntryOpenOptions())
    }
}
