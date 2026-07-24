package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryOutsideReleasePeriodFilterProvider

internal class MangaOutsideReleasePeriodFilterProvider : EntryOutsideReleasePeriodFilterProvider {
    override val type = EntryType.MANGA
}
