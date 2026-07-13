package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryLibraryFilterProcessor
import tachiyomi.domain.entry.model.Entry

internal class BookLibraryFilterProcessor : EntryLibraryFilterProcessor {
    override val type = EntryType.BOOK

    override fun supportsOutsideReleasePeriodFilter(entry: Entry): Boolean {
        entry.requireBook()
        return true
    }
}
