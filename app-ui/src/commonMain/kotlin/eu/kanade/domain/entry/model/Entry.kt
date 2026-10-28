package eu.kanade.domain.entry.model

import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.entry.model.Entry

fun Entry.hasCustomCover(coverCache: CoverCache): Boolean {
    return coverCache.hasCustomCover(id)
}
