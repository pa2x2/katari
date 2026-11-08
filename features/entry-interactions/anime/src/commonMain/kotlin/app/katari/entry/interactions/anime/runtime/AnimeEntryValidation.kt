package app.katari.entry.interactions.anime.runtime

import app.katari.entry.interactions.download.EntryDownloadQueueGroup
import app.katari.entry.interactions.download.EntryDownloadQueueItem
import app.katari.entry.interactions.download.EntryDownloadStatus
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

internal fun Entry.requireAnime() {
    require(type == EntryType.ANIME) {
        "Anime entry interaction received ${type.name}; expected ${EntryType.ANIME.name}"
    }
}

private fun EntryType.requireAnime() {
    require(this == EntryType.ANIME) {
        "Anime entry interaction received $name; expected ${EntryType.ANIME.name}"
    }
}

internal fun EntryDownloadStatus.requireAnime(): EntryDownloadStatus {
    entryType.requireAnime()
    return this
}

internal fun EntryDownloadQueueItem.requireAnime(): EntryDownloadQueueItem {
    entryType.requireAnime()
    return this
}

internal fun EntryDownloadQueueGroup.requireAnime(): EntryDownloadQueueGroup {
    entryType.requireAnime()
    items.forEach { it.requireAnime() }
    return this
}

internal fun List<EntryDownloadQueueItem>.requireAnime(): List<EntryDownloadQueueItem> {
    forEach { it.requireAnime() }
    return this
}
