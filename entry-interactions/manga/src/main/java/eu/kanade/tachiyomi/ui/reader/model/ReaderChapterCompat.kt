package eu.kanade.tachiyomi.ui.reader.model

import mihon.entry.interactions.manga.download.DownloadCache
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun EntryChapter.toReaderChapter(): Chapter {
    return Chapter(
        id = id,
        mangaId = entryId,
        read = read,
        bookmark = bookmark,
        dateFetch = dateFetch,
        sourceOrder = sourceOrder,
        url = url,
        name = name,
        dateUpload = dateUpload,
        chapterNumber = chapterNumber,
        scanlator = scanlator,
        lastModifiedAt = lastModifiedAt,
        version = version,
        memo = memo,
    )
}

internal fun Chapter.toEntryChapter(): EntryChapter {
    return EntryChapter(
        id = id,
        entryId = mangaId,
        url = url,
        name = name,
        read = read,
        bookmark = bookmark,
        chapterNumber = chapterNumber,
        scanlator = scanlator,
        dateUpload = dateUpload,
        dateFetch = dateFetch,
        sourceOrder = sourceOrder,
        lastModifiedAt = lastModifiedAt,
        version = version,
        isSyncing = false,
        memo = memo,
    )
}

internal fun List<Chapter>.filterDownloaded(mangaById: Map<Long, Entry>): List<Chapter> {
    return filter { chapter ->
        val manga = mangaById[chapter.mangaId] ?: return@filter false
        manga.source == LocalSource.ID ||
            Injekt.get<DownloadCache>().isChapterDownloaded(
                chapter.name,
                chapter.scanlator,
                chapter.url,
                manga.title,
                manga.source,
                false,
            )
    }
}

internal fun List<Chapter>.removeDuplicates(currentChapter: Chapter): List<Chapter> {
    return groupBy { it.mangaId to it.chapterNumber }
        .map { (_, chapters) ->
            chapters.find { it.id == currentChapter.id }
                ?: chapters.find { it.scanlator == currentChapter.scanlator }
                ?: chapters.first()
        }
}
