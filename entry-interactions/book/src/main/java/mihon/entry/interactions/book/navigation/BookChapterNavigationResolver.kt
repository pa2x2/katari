package mihon.entry.interactions.book.navigation

import mihon.entry.interactions.book.runtime.requireBook
import mihon.entry.interactions.viewer.EntryChildWindow
import mihon.entry.interactions.viewer.entryChildWindow
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.sortedForReading

internal class BookChapterNavigationResolver(
    private val getEntryWithChapters: GetEntryWithChapters,
) {
    suspend fun resolve(entry: Entry, current: EntryChapter): EntryChildWindow<EntryChapter>? {
        return resolveAll(entry, current.id).entryChildWindow(current.id, EntryChapter::id)
    }

    /**
     * [entry]'s chapters in reading order without those of excluded scanlators, as the manga reader reads them. The
     * chapter [currentChapterId] stays, so one opened directly, such as from a notification, still opens.
     */
    suspend fun resolveAll(entry: Entry, currentChapterId: Long): List<EntryChapter> {
        entry.requireBook()
        val shown = getEntryWithChapters.awaitChapters(entry, applyScanlatorFilter = true)
        val chapters = if (shown.any { it.id == currentChapterId }) {
            shown
        } else {
            shown + getEntryWithChapters.awaitChapters(entry).filter { it.id == currentChapterId }
        }
        return chapters.sortedForReading(entry)
    }
}
