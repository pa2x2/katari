package mihon.entry.interactions.host

import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID

internal fun Entry.removeCovers(coverCache: CoverCache) {
    if (source == LOCAL_SOURCE_ID) return
    coverCache.deleteFromCache(this, true)
}
