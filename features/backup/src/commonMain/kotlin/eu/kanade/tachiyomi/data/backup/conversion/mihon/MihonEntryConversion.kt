package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.conversion.CustomInfo
import eu.kanade.tachiyomi.data.backup.conversion.validMemo
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.source.entry.EntryType

internal fun MihonManga.toBackupEntry(): BackupEntry = BackupEntry(
    source = source,
    url = url,
    title = title,
    displayName = customInfo.displayName,
    artist = artist,
    author = author,
    description = description,
    genre = genre,
    status = status,
    thumbnailUrl = thumbnailUrl,
    dateAdded = dateAdded,
    chapters = chapters.map { it.toBackupChapter() },
    categories = categories,
    favorite = favorite,
    chapterFlags = chapterFlags.toLong(),
    viewerFlags = (viewerFlags ?: viewer).toLong(),
    history = history,
    updateStrategy = updateStrategy,
    favoriteModifiedAt = favoriteModifiedAt,
    excludedScanlators = excludedScanlators,
    notes = notes,
    initialized = initialized,
    memo = memo.validMemo(),
    nextUpdate = nextUpdate,
    type = EntryType.MANGA,
)

internal val MihonManga.customInfo: CustomInfo
    get() = CustomInfo.over(
        title = customTitle,
        artist = customArtist,
        author = customAuthor,
        description = customDescription,
        genre = customGenre,
        // TachiyomiJ2K and Yōkai write -1 for no custom status and read 0 as none too (CustomMangaManager, BackupManga)
        status = customStatus?.takeUnless { it == 0 || it == -1 },
        cover = customThumbnailUrl,
        ownTitle = title,
        ownArtist = artist,
        ownAuthor = author,
        ownDescription = description,
        ownGenre = genre,
        ownStatus = status,
        ownCover = thumbnailUrl,
    )

internal val MihonManga.hasSuwayomiMeta: Boolean
    get() = suwayomiMeta.isNotEmpty() || chapters.any { it.suwayomiMeta.isNotEmpty() }

/**
 * Whether the entry holds reading state worth carrying even outside the library. Restoring puts history on its chapter,
 * so history counts only where the backup holds that chapter.
 */
internal val MihonManga.hasProgress: Boolean
    get() = chapters.any { it.read || it.bookmark || it.lastPageRead > 0 } || history.any { it.url in chapterUrls }

/** Whether some of the entry's history is for chapters the backup doesn't hold, as in one made without chapters. */
internal val MihonManga.hasHistoryWithoutChapters: Boolean
    get() = history.any { it.url !in chapterUrls }

private val MihonManga.chapterUrls: Set<String>
    get() = chapters.mapTo(hashSetOf()) { it.url }

private fun MihonChapter.toBackupChapter(): BackupChapter = BackupChapter(
    url = url,
    name = name,
    scanlator = scanlator,
    read = read,
    bookmark = bookmark,
    lastPageRead = lastPageRead,
    dateFetch = dateFetch,
    dateUpload = dateUpload,
    chapterNumber = chapterNumber.toDouble(),
    sourceOrder = sourceOrder,
    memo = memo.validMemo(),
)
