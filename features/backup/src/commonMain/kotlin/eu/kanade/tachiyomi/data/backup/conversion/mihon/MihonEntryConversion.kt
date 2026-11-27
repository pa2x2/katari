package eu.kanade.tachiyomi.data.backup.conversion.mihon

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.conversion.validMemo
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*

internal fun MihonManga.toBackupEntry(): BackupEntry = BackupEntry(
    source = source,
    url = url,
    title = title,
    // D027: the only custom info Katari keeps
    displayName = customTitle?.takeIf { it.isNotBlank() },
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

/** The custom info TachiyomiSY, Komikku, TachiyomiJ2K and Yōkai keep that Katari doesn't (D027), as report notes. */
internal val MihonManga.customInfoLeftBehind: List<StringResource>
    get() = listOfNotNull(
        MR.strings.backup_conversion_custom_author.takeIf { customAuthor != null },
        MR.strings.backup_conversion_custom_artist.takeIf { customArtist != null },
        MR.strings.backup_conversion_custom_description.takeIf { customDescription != null },
        MR.strings.backup_conversion_custom_genres.takeIf { customGenre != null },
        MR.strings.backup_conversion_custom_status.takeIf { customStatus != null },
        MR.strings.backup_conversion_custom_cover.takeIf { customThumbnailUrl != null },
    )

internal val MihonManga.hasSuwayomiMeta: Boolean
    get() = suwayomiMeta.isNotEmpty() || chapters.any { it.suwayomiMeta.isNotEmpty() }

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
