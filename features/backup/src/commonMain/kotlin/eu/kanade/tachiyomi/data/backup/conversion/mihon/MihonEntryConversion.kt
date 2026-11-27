package eu.kanade.tachiyomi.data.backup.conversion.mihon

import app.katari.core.common.extensions.JsonObjectEmptyBytes
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.data.MemoColumnAdapter

internal fun MihonManga.toBackupEntry(): BackupEntry = BackupEntry(
    source = source,
    url = url,
    title = title,
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

/** Whether the entry holds reading state worth carrying even outside the library. */
internal val MihonManga.hasProgress: Boolean
    get() = history.isNotEmpty() || chapters.any { it.read || it.bookmark || it.lastPageRead > 0 }

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

// Restoring decodes the memo as a JSON object, which a fork's empty or malformed memo isn't
private fun ByteArray.validMemo(): ByteArray =
    takeIf { runCatching { MemoColumnAdapter.decode(it) }.isSuccess } ?: JsonObjectEmptyBytes
