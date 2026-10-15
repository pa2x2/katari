package eu.kanade.domain.entry.model

import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entry.model.Entry

fun Entry.hasCustomCover(coverCache: CoverCache): Boolean {
    return coverCache.hasCustomCover(id)
}

fun Entry.chaptersFiltered(downloadedOnly: Boolean): Boolean {
    val downloadedFilter = if (downloadedOnly) {
        TriState.ENABLED_IS
    } else {
        this.downloadedFilter
    }

    return unreadFilter != TriState.DISABLED ||
        downloadedFilter != TriState.DISABLED ||
        bookmarkedFilter != TriState.DISABLED
}
