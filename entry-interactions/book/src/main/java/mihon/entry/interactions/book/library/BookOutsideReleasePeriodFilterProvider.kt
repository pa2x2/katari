package mihon.entry.interactions.book.library

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryOutsideReleasePeriodFilterProvider

internal class BookOutsideReleasePeriodFilterProvider : EntryOutsideReleasePeriodFilterProvider {
    override val type = EntryType.BOOK
}
