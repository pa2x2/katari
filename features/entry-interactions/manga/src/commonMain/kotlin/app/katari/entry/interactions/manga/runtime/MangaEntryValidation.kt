package app.katari.entry.interactions.manga.runtime

import app.katari.entry.interactions.download.EntryDownloadQueueGroup
import app.katari.entry.interactions.download.EntryDownloadQueueItem
import app.katari.entry.interactions.download.EntryDownloadStatus
import eu.kanade.tachiyomi.source.entry.EntryType
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
