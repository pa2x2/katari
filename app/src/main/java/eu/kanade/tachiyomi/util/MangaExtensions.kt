package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.entry.interactor.UpdateEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.source.local.image.LocalCoverManager
import java.io.InputStream

fun Entry.removeCovers(coverCache: CoverCache) {
    if (isLocalEntry()) return
    coverCache.deleteFromCache(this, true)
}

suspend fun Entry.editCover(
    coverManager: LocalCoverManager,
    stream: InputStream,
    updateEntry: UpdateEntry,
    coverCache: CoverCache,
) {
    if (isLocalEntry()) {
        coverManager.update(url, stream)
        updateEntry.awaitUpdateCoverLastModified(id)
    } else if (favorite) {
        coverCache.setCustomCoverToCache(this, stream)
        updateEntry.awaitUpdateCoverLastModified(id)
    }
}

private fun Entry.isLocalEntry(): Boolean = source == tachiyomi.source.local.LocalSource.ID
