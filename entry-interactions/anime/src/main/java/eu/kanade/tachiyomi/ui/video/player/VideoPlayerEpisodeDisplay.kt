package eu.kanade.tachiyomi.ui.video.player

import androidx.compose.runtime.Immutable
import mihon.entry.interactions.anime.lastWatchedAt
import mihon.entry.interactions.anime.positionMs
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.service.getChapterSort
import tachiyomi.domain.entry.service.groupedByMergedMember
import tachiyomi.domain.entry.service.sortedForMergedDisplay
import tachiyomi.domain.entry.service.sortedForReading
import tachiyomi.domain.util.applyFilter

internal data class VideoPlayerEpisodeDisplayData(
    val chapters: List<EntryChapter>,
    val playbackStateByChapterId: Map<Long, EntryProgressState>,
    val primaryChapterId: Long?,
    val chapterListItems: List<VideoPlayerEpisodeListEntry>,
)

internal fun buildVideoPlayerEpisodeDisplayData(
    entry: Entry,
    chapters: List<EntryChapter>,
    memberIds: List<Long>,
    memberTitleById: Map<Long, String>,
    playbackStates: List<EntryProgressState>,
): VideoPlayerEpisodeDisplayData {
    val playbackStateByEpisodeId = playbackStates
        .mapNotNull { state -> state.chapterId?.let { it to state } }
        .toMap()
    val filteredEpisodes = chapters.filterEpisodesForDisplay(entry)
    val displayedEpisodes = filteredEpisodes.sortEpisodesForDisplay(entry, memberIds)

    return VideoPlayerEpisodeDisplayData(
        chapters = displayedEpisodes,
        playbackStateByChapterId = playbackStateByEpisodeId,
        primaryChapterId = selectPrimaryEpisodeIdForDisplay(
            episodes = filteredEpisodes.sortedForReading(entry, memberIds),
            playbackStateByEpisodeId = playbackStateByEpisodeId,
        ),
        chapterListItems = buildVideoPlayerEpisodeListItems(
            episodes = displayedEpisodes,
            memberIds = memberIds,
            memberTitleById = memberTitleById,
            fallbackTitle = entry.displayTitle,
        ),
    )
}

private fun buildVideoPlayerEpisodeListItems(
    episodes: List<EntryChapter>,
    memberIds: List<Long>,
    memberTitleById: Map<Long, String>,
    fallbackTitle: String,
): List<VideoPlayerEpisodeListEntry> {
    if (memberIds.size <= 1) {
        return episodes.map(VideoPlayerEpisodeListEntry::Item)
    }

    return buildList {
        episodes.groupedByMergedMember(memberIds).forEach { (memberId, memberEpisodes) ->
            add(
                VideoPlayerEpisodeListEntry.MemberHeader(
                    animeId = memberId,
                    title = memberTitleById[memberId].orEmpty().ifBlank { fallbackTitle },
                ),
            )
            addAll(memberEpisodes.map(VideoPlayerEpisodeListEntry::Item))
        }
    }
}

@Immutable
internal sealed class VideoPlayerEpisodeListEntry {
    @Immutable
    data class MemberHeader(
        val animeId: Long,
        val title: String,
    ) : VideoPlayerEpisodeListEntry()

    @Immutable
    data class Item(
        val episode: EntryChapter,
    ) : VideoPlayerEpisodeListEntry()
}

private fun List<EntryChapter>.filterEpisodesForDisplay(
    anime: Entry,
): List<EntryChapter> {
    val unwatchedFilter = anime.unreadFilter

    return asSequence()
        .filter { episode ->
            applyFilter(unwatchedFilter) { !episode.read }
        }
        .toList()
}

private fun List<EntryChapter>.sortEpisodesForDisplay(
    anime: Entry,
    memberIds: List<Long>,
): List<EntryChapter> {
    return if (memberIds.size > 1) {
        sortedForMergedDisplay(anime, memberIds)
    } else {
        sortedWith(getChapterSort(anime))
    }
}

private fun selectPrimaryEpisodeIdForDisplay(
    episodes: List<EntryChapter>,
    playbackStateByEpisodeId: Map<Long, EntryProgressState>,
): Long? {
    val inProgressEpisode = episodes
        .asSequence()
        .mapNotNull { episode ->
            val playbackState = playbackStateByEpisodeId[episode.id] ?: return@mapNotNull null
            if (playbackState.completed || playbackState.positionMs <= 0L) return@mapNotNull null
            episode to playbackState
        }
        .maxByOrNull { (_, playbackState) -> playbackState.lastWatchedAt }
        ?.first
    if (inProgressEpisode != null) {
        return inProgressEpisode.id
    }

    return episodes.firstOrNull { !it.read }?.id ?: episodes.firstOrNull()?.id
}
