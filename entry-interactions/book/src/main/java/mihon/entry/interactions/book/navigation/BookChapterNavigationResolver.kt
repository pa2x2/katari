package mihon.entry.interactions.book

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
        return resolveAll(entry).entryChildWindow(current.id, EntryChapter::id)
    }

    suspend fun resolveAll(entry: Entry): List<EntryChapter> {
        entry.requireBook()
        return getEntryWithChapters.awaitChapters(entry).sortedForReading(entry)
    }
}
