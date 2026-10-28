package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.entry.model.Entry

fun Entry.removeCovers(coverCache: CoverCache) {
    if (isLocalEntry()) return
    coverCache.deleteFromCache(this, true)
}

private fun Entry.isLocalEntry(): Boolean = source == tachiyomi.source.local.LocalSource.ID
