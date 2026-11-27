package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import eu.kanade.tachiyomi.data.backup.conversion.CustomInfo
import eu.kanade.tachiyomi.data.backup.conversion.validMemo
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupPlaybackState
import eu.kanade.tachiyomi.data.backup.models.compatibility.toProgressState
import eu.kanade.tachiyomi.data.backup.models.toBackupEntryProgressState
import eu.kanade.tachiyomi.source.entry.ChapterKind
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

/** The anime as a Katari entry from [source], in the merged list's [categories]. */
internal fun AniyomiAnime.toBackupEntry(source: Long, categories: List<Long>): BackupEntry {
    val watchedAt = history.associate { it.url to it.lastRead }
    return BackupEntry(
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
        chapters = episodes.map { it.toBackupChapter() },
        categories = categories,
        favorite = favorite,
        chapterFlags = katariChapterFlags(episodeFlags),
        history = history,
        updateStrategy = updateStrategy,
        favoriteModifiedAt = favoriteModifiedAt,
        excludedScanlators = excludedScanlators + animiruExcludedScanlators,
        notes = notes,
        initialized = initialized,
        memo = memo.validMemo(),
        nextUpdate = nextUpdate,
        backgroundUrl = backgroundUrl,
        type = EntryType.ANIME,
        progressStates = episodes.mapNotNull { episode ->
            BackupPlaybackState(
                url = episode.url,
                positionMs = episode.lastSecondSeen,
                durationMs = episode.totalSeconds,
                completed = episode.seen,
                lastWatchedAt = watchedAt[episode.url] ?: 0,
            ).toProgressState()?.toBackupEntryProgressState()
        },
    )
}

/** The custom info Animetail, Anikku and Animiru keep, each at its own field numbers. */
internal val AniyomiAnime.customInfo: CustomInfo
    get() = CustomInfo(
        title = customTitle ?: animiruCustomTitle,
        artist = customArtist ?: animiruCustomArtist,
        author = customAuthor ?: animiruCustomAuthor,
        description = customDescription ?: animiruCustomDescription,
        genre = customGenre ?: animiruCustomGenre,
        status = customStatus ?: animiruCustomStatus,
        cover = customThumbnailUrl,
    )

/** Whether the anime holds watching state worth carrying even outside the library. */
internal val AniyomiAnime.hasProgress: Boolean
    get() = history.isNotEmpty() || episodes.any { it.seen || it.bookmark || it.lastSecondSeen > 0 }

/** Whether the person set how far the skip-intro button jumps for this anime, which Katari has no setting for (D057). */
internal val AniyomiAnime.hasSkipIntroLength: Boolean
    get() = viewerFlags and ANIME_INTRO_MASK != 0L

private fun AniyomiEpisode.toBackupChapter(): BackupChapter = BackupChapter(
    url = url,
    name = name,
    scanlator = scanlator,
    read = seen,
    bookmark = bookmark,
    dateFetch = dateFetch,
    dateUpload = dateUpload,
    chapterNumber = episodeNumber.toDouble(),
    sourceOrder = sourceOrder,
    memo = memo.validMemo(),
    summary = summary,
    previewUrl = previewUrl,
    kind = if (fillermark) ChapterKind.FILLER.code else ChapterKind.REGULAR.code,
)

/**
 * Aniyomi's episode list settings as Katari's chapter flags (D056). The sort direction, the seen, downloaded and
 * bookmarked filters and the name or number display sit where Katari keeps them; the filler filter and the sort move;
 * Katari's one switch for previews and summaries hides both where Aniyomi hid either.
 */
private fun katariChapterFlags(episodeFlags: Long): Long {
    val kept = episodeFlags and (
        Entry.CHAPTER_SORT_DIR_MASK or Entry.CHAPTER_UNREAD_MASK or Entry.CHAPTER_DOWNLOADED_MASK or
            Entry.CHAPTER_BOOKMARKED_MASK or Entry.CHAPTER_DISPLAY_MASK
        )
    val filler = when (episodeFlags and EPISODE_FILLERMARKED_MASK) {
        EPISODE_SHOW_FILLERMARKED -> Entry.CHAPTER_SHOW_FILLER
        EPISODE_SHOW_NOT_FILLERMARKED -> Entry.CHAPTER_SHOW_NOT_FILLER
        else -> Entry.SHOW_ALL
    }
    val sorting = when (episodeFlags and EPISODE_SORTING_MASK) {
        EPISODE_SORTING_NUMBER -> Entry.CHAPTER_SORTING_NUMBER
        EPISODE_SORTING_UPLOAD_DATE -> Entry.CHAPTER_SORTING_UPLOAD_DATE
        EPISODE_SORTING_ALPHABET -> Entry.CHAPTER_SORTING_ALPHABET
        else -> Entry.CHAPTER_SORTING_SOURCE
    }
    val previews = if (episodeFlags and (EPISODE_HIDE_PREVIEWS or EPISODE_HIDE_SUMMARIES) != 0L) {
        Entry.CHAPTER_HIDE_PREVIEWS
    } else {
        Entry.SHOW_ALL
    }
    return kept or filler or sorting or previews
}

// Aniyomi's tachiyomi.domain.entries.anime.model.Anime (aniyomiorg/aniyomi 97414446b8a9)
private const val EPISODE_SHOW_FILLERMARKED = 0x00000080L
private const val EPISODE_SHOW_NOT_FILLERMARKED = 0x00000100L
private const val EPISODE_FILLERMARKED_MASK = 0x00000180L
private const val EPISODE_SORTING_NUMBER = 0x00000200L
private const val EPISODE_SORTING_UPLOAD_DATE = 0x00000400L
private const val EPISODE_SORTING_ALPHABET = 0x00000600L
private const val EPISODE_SORTING_MASK = 0x00000600L
private const val EPISODE_HIDE_PREVIEWS = 0x00000800L
private const val EPISODE_HIDE_SUMMARIES = 0x00001000L
private const val ANIME_INTRO_MASK = 0x00000000000000FFL
