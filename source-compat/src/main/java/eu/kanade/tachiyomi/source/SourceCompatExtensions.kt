package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.adapter.LegacyMangaSourceAdapter
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryItemOrientationProvider
import eu.kanade.tachiyomi.source.entry.UnifiedSource

fun UnifiedSource.sourceItemOrientation(): EntryItemOrientation {
    return (this as? EntryItemOrientationProvider)?.itemOrientation
        ?: (this as? LegacyMangaSourceAdapter)?.source?.sourceItemOrientation()?.toEntryItemOrientation()
        ?: EntryItemOrientation.VERTICAL
}
