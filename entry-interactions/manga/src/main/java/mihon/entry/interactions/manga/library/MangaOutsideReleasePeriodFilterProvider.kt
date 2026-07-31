package mihon.entry.interactions.manga.library

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryOutsideReleasePeriodFilterProvider

internal class MangaOutsideReleasePeriodFilterProvider : EntryOutsideReleasePeriodFilterProvider {
    override val type = EntryType.MANGA
}
