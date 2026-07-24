package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.service.EntryLibraryProgressResolutionPort

/** Feature-owned boundary for optional unified-Library progress summaries. */
interface EntryLibraryProgressFeature : EntryLibraryProgressResolutionPort {
    fun isApplicable(type: EntryType): Boolean
}
