package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryOutsideReleasePeriodFilterProvider

internal class BookOutsideReleasePeriodFilterProvider : EntryOutsideReleasePeriodFilterProvider {
    override val type = EntryType.BOOK
}
