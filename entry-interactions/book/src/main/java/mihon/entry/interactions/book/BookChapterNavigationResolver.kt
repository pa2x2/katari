package mihon.entry.interactions.book

import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.sortedForReading

internal class BookChapterNavigationResolver(
    private val getEntryWithChapters: GetEntryWithChapters,
) {
    suspend fun resolve(entry: Entry, current: EntryChapter): BookChapterNavigation {
        entry.requireBook()
        val chapters = getEntryWithChapters.awaitChapters(entry.id).sortedForReading(entry)
        val index = chapters.indexOfFirst { it.id == current.id }
        if (index < 0) return BookChapterNavigation()
        return BookChapterNavigation(
            previous = chapters.getOrNull(index - 1),
            next = chapters.getOrNull(index + 1),
        )
    }
}

internal data class BookChapterNavigation(
    val previous: EntryChapter? = null,
    val next: EntryChapter? = null,
)
