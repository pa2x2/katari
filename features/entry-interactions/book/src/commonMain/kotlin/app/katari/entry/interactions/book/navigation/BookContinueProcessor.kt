package app.katari.entry.interactions.book.navigation

import app.katari.entry.interactions.book.runtime.requireBook
import app.katari.entry.interactions.navigation.EntryContinueProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.service.sortedForReading

internal object BookContinueProcessor : EntryContinueProcessor {
    override fun findNext(
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
}
