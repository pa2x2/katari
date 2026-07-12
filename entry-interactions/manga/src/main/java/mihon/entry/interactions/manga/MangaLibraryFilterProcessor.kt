package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryLibraryFilterProcessor
import tachiyomi.domain.entry.model.Entry

internal class MangaLibraryFilterProcessor : EntryLibraryFilterProcessor {
    override val type = EntryType.MANGA

    override fun supportsOutsideReleasePeriodFilter(entry: Entry): Boolean {
        entry.requireManga()
        return true
    }
}
