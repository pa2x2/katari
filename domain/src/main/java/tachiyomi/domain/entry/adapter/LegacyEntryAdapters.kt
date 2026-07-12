package tachiyomi.domain.entry.adapter

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.model.CatalogItem
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryStatus

fun SManga.toEntry(sourceId: Long): Entry {
    return Entry.create().copy(
        source = sourceId,
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genre = getGenres(),
        status = EntryStatus.from(status),
        thumbnailUrl = thumbnail_url,
        updateStrategy = update_strategy.toEntryUpdateStrategy(),
        initialized = initialized,
        memo = memo,
        type = EntryType.MANGA,
    )
}

fun SEntry.toEntry(sourceId: Long): Entry {
    return Entry.create().copy(
        source = sourceId,
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genre = genre,
        status = EntryStatus.from(status),
        thumbnailUrl = thumbnailUrl,
        updateStrategy = updateStrategy ?: EntryUpdateStrategy.ALWAYS_UPDATE,
        initialized = initialized,
        memo = memo,
        type = type,
    )
}

fun CatalogItem.toEntry(sourceId: Long): Entry = when (this) {
    is CatalogItem.MangaItem -> manga.toEntry(sourceId)
}

private fun UpdateStrategy.toEntryUpdateStrategy(): EntryUpdateStrategy = when (this) {
    UpdateStrategy.ALWAYS_UPDATE -> EntryUpdateStrategy.ALWAYS_UPDATE
    UpdateStrategy.ONLY_FETCH_ONCE -> EntryUpdateStrategy.ONLY_FETCH_ONCE
}
