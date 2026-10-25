package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.source.entry.ChapterKind
import mihon.entry.interactions.manga.download.DownloadCache
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.source.local.LocalSource

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
        // The legacy chapter doesn't carry them; repository state writes leave the stored ones alone
        summary = null,
        previewUrl = null,
        kind = ChapterKind.REGULAR,
        groupKey = null,
        groupPosition = null,
    )
}

internal fun List<Chapter>.filterDownloaded(mangaById: Map<Long, Entry>, downloadCache: DownloadCache): List<Chapter> {
    return filter { chapter ->
        val manga = mangaById[chapter.mangaId] ?: return@filter false
        manga.source == LocalSource.ID ||
            downloadCache.isChapterDownloaded(
                chapter.name,
                chapter.scanlator,
                chapter.url,
                manga.downloadFolderTitle,
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
