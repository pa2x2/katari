package app.katari.entry.interactions.host.lifecycle.profile

import app.katari.entry.interactions.host.removeCovers
import app.katari.entry.interactions.lifecycle.profile.host.EntryProfileMoveCustomCoverHost
import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.entry.model.Entry

class AppEntryProfileMoveCustomCoverHost(
    private val coverCache: CoverCache,
) : EntryProfileMoveCustomCoverHost {
    override suspend fun removeCustomCovers(entries: List<Entry>) {
        entries.forEach { entry -> entry.removeCovers(coverCache) }
    }
}
