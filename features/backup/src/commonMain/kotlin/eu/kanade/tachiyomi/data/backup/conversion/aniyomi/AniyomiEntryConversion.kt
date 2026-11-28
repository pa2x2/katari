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

/** The anime as a Katari entry from [source], in [categories] as orders in the anime category list. */
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
    get() = CustomInfo.over(
        title = customTitle ?: animiruCustomTitle,
        artist = customArtist ?: animiruCustomArtist,
        author = customAuthor ?: animiruCustomAuthor,
        description = customDescription ?: animiruCustomDescription,
        genre = customGenre ?: animiruCustomGenre,
        status = customStatus ?: animiruCustomStatus,
        cover = customThumbnailUrl,
        ownTitle = title,
        ownArtist = artist,
        ownAuthor = author,
        ownDescription = description,
        ownGenre = genre,
        ownStatus = status,
        ownCover = thumbnailUrl,
    )

/**
 * Whether the anime holds watching state worth carrying even outside the library. Restoring puts history on its
 * episode, so history counts only where the backup holds that episode.
 */
internal val AniyomiAnime.hasProgress: Boolean
    get() = episodes.any { it.seen || it.bookmark || it.lastSecondSeen > 0 } || history.any { it.url in episodeUrls }

/** Whether some of the anime's history is for episodes the backup doesn't hold, as in a backup made without them. */
internal val AniyomiAnime.hasHistoryWithoutEpisodes: Boolean
    get() = history.any { it.url !in episodeUrls }

private val AniyomiAnime.episodeUrls: Set<String>
    get() = episodes.mapTo(hashSetOf()) { it.url }

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
    // D072
    dateUpload = dateUploadOverride.takeIf { it > 0 } ?: dateUpload,
    chapterNumber = episodeNumber.toDouble(),
    sourceOrder = sourceOrder,
    memo = memo.validMemo(),
    summary = summary,
    previewUrl = previewUrl,
    kind = if (fillermark || legacyFillermark) ChapterKind.FILLER.code else ChapterKind.REGULAR.code,
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
    val previews = if (hidesPreviews(episodeFlags) || hidesSummaries(episodeFlags)) {
        Entry.CHAPTER_HIDE_PREVIEWS
    } else {
        Entry.SHOW_ALL
    }
    return kept or katariFillerFilter(episodeFlags) or katariSorting(episodeFlags) or previews
}

// Aniyomi's tachiyomi.domain.entries.anime.model.Anime (aniyomiorg/aniyomi 97414446b8a9)
private const val ANIME_INTRO_MASK = 0x00000000000000FFL
