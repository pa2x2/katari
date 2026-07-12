package tachiyomi.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryCover

/**
 * A local/domain representation of one item in a unified catalog list.
 *
 * It pairs the source-level catalog item with the local [Entry] so the UI has
 * stable IDs, favorite status, and cover data.
 */
sealed class CatalogListItem {
    abstract val id: Long
    abstract val title: String
    abstract val cover: EntryCover
    abstract val favorite: Boolean
    abstract val sourceItemOrientation: EntryItemOrientation
    abstract val sourceId: Long
    abstract val url: String
    abstract val entryType: EntryType

    data class EntryItem(
        val entry: Entry,
        override val sourceItemOrientation: EntryItemOrientation,
    ) : CatalogListItem() {
        override val id: Long get() = entry.id
        override val title: String get() = entry.displayTitle
        override val cover: EntryCover
            get() = EntryCover(
                entryId = entry.id,
                sourceId = entry.source,
                isFavorite = entry.favorite,
                url = entry.thumbnailUrl,
                lastModified = entry.coverLastModified,
            )
        override val favorite: Boolean get() = entry.favorite
        override val sourceId: Long get() = entry.source
        override val url: String get() = entry.url
        override val entryType: EntryType get() = entry.type
    }
}
