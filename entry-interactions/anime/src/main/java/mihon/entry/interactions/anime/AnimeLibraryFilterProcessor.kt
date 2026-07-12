package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryLibraryFilterProcessor
import tachiyomi.domain.entry.model.Entry

internal class AnimeLibraryFilterProcessor : EntryLibraryFilterProcessor {
    override val type = EntryType.ANIME

    override fun supportsOutsideReleasePeriodFilter(entry: Entry): Boolean {
        entry.requireAnime()
        return false
    }
}
