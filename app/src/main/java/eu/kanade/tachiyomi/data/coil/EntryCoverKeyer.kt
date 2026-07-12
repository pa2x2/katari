package eu.kanade.tachiyomi.data.coil

import coil3.key.Keyer
import coil3.request.Options
import eu.kanade.domain.entry.model.hasCustomCover
import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.entry.model.EntryCover
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.domain.entry.model.Entry as DomainEntry

class EntryKeyer : Keyer<DomainEntry> {
    override fun key(data: DomainEntry, options: Options): String {
        return if (data.hasCustomCover()) {
            "${data.id};${data.coverLastModified}"
        } else {
            "${data.thumbnailUrl};${data.coverLastModified}"
        }
    }
}

class EntryCoverKeyer(
    private val coverCache: CoverCache = Injekt.get(),
) : Keyer<EntryCover> {
    override fun key(data: EntryCover, options: Options): String {
        return if (coverCache.getCustomCoverFile(data.entryId).exists()) {
            "${data.entryId};${data.lastModified}"
        } else {
            "${data.url};${data.lastModified}"
        }
    }
}
