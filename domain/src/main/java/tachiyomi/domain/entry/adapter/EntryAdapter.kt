package tachiyomi.domain.entry.adapter

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryStatus

fun SEntry.toDomainEntry(
    sourceId: Long,
    type: EntryType,
): Entry {
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

fun Entry.toSEntry(): SEntry {
    return SEntry.create().also {
        it.url = url
        it.title = title
        it.artist = artist
        it.author = author
        it.description = description
        it.genre = genre
        it.status = status.value
        it.thumbnailUrl = thumbnailUrl
        it.updateStrategy = updateStrategy
        it.initialized = initialized
        it.memo = memo
        it.type = type
    }
}

fun SEntryChapter.toDomainChapter(
    entryId: Long,
    sourceOrder: Long = 0L,
    dateFetch: Long = 0L,
): EntryChapter {
    return EntryChapter.create().copy(
        entryId = entryId,
        url = url,
        name = name.ifBlank { url },
        dateUpload = dateUpload,
        chapterNumber = chapterNumber,
        scanlator = scanlator?.ifBlank { null },
        sourceOrder = sourceOrder,
        dateFetch = dateFetch,
        memo = memo,
    )
}

fun EntryChapter.toSEntryChapter(): SEntryChapter {
    return SEntryChapter.create().also {
        it.url = url
        it.name = name
        it.dateUpload = dateUpload
        it.chapterNumber = chapterNumber
        it.scanlator = scanlator
        it.memo = memo
    }
}

fun EntryChapter.copyFrom(sourceChapter: SEntryChapter, sourceOrder: Long): EntryChapter {
    return copy(
        url = sourceChapter.url,
        name = sourceChapter.name.ifBlank { sourceChapter.url },
        dateUpload = sourceChapter.dateUpload,
        chapterNumber = sourceChapter.chapterNumber,
        sourceOrder = sourceOrder,
        scanlator = sourceChapter.scanlator?.ifBlank { null },
        memo = sourceChapter.memo,
    )
}
