package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryDownloadQueueGroup
import mihon.entry.interactions.EntryDownloadQueueItem
import mihon.entry.interactions.EntryDownloadStatus
import tachiyomi.domain.entry.model.Entry

internal fun Entry.requireManga() {
    require(type == EntryType.MANGA) {
        "Manga entry interaction received ${type.name}; expected ${EntryType.MANGA.name}"
    }
}

private fun EntryType.requireManga() {
    require(this == EntryType.MANGA) {
        "Manga entry interaction received $name; expected ${EntryType.MANGA.name}"
    }
}

internal fun EntryDownloadStatus.requireManga(): EntryDownloadStatus {
    entryType.requireManga()
    return this
}

internal fun EntryDownloadQueueItem.requireManga(): EntryDownloadQueueItem {
    entryType.requireManga()
    return this
}

internal fun EntryDownloadQueueGroup.requireManga(): EntryDownloadQueueGroup {
    entryType.requireManga()
    items.forEach { it.requireManga() }
    return this
}

internal fun List<EntryDownloadQueueItem>.requireManga(): List<EntryDownloadQueueItem> {
    forEach { it.requireManga() }
    return this
}
