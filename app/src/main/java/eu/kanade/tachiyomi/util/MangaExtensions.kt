package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.entry.interactor.UpdateEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.source.local.image.LocalCoverManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.InputStream
import java.time.Instant

fun Entry.removeCovers(coverCache: CoverCache = Injekt.get()): Entry {
    if (isLocalEntry()) return this
    return if (coverCache.deleteFromCache(this, true) > 0) {
        copy(coverLastModified = Instant.now().toEpochMilli())
    } else {
        this
    }
}

suspend fun Entry.editCover(
    coverManager: LocalCoverManager,
    stream: InputStream,
    updateEntry: UpdateEntry = Injekt.get(),
    coverCache: CoverCache = Injekt.get(),
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
