package eu.kanade.tachiyomi.ui.video.player

import androidx.compose.runtime.Immutable
import mihon.entry.interactions.anime.state.lastWatchedAt
import mihon.entry.interactions.anime.state.positionMs
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.child.EntryChildListRequest
import mihon.entry.interactions.child.EntryChildListResult
import mihon.entry.interactions.child.EntryChildListRow
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryChapterGroup
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.model.groupId
import tachiyomi.domain.entry.service.sortedForReading

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
    memberSourceNameById: Map<Long, String>,
    playbackStates: List<EntryProgressState>,
    playingEpisodeId: Long,
    isShown: (EntryChapter) -> Boolean,
    chapterGroups: List<EntryChapterGroup>,
    childListFeature: EntryChildListFeature,
): VideoPlayerEpisodeDisplayData {
    val playbackStateByEpisodeId = playbackStates
        .mapNotNull { state -> state.chapterId?.let { it to state } }
        .toMap()
    // The playing episode stays listed even when the filters hide it, like in episodeWindow
    val filteredEpisodes = chapters.filter { it.id == playingEpisodeId || isShown(it) }
    val rows = when (
        val result = childListFeature.displayList(
            EntryChildListRequest(
                entry = entry,
                chapters = filteredEpisodes,
                memberIds = memberIds,
                memberTitleById = memberTitleById,
                includeMissingCounts = false,
                chapterGroups = chapterGroups,
                allChapters = chapters,
            ),
        )
    ) {
        is EntryChildListResult.Available -> result.display.rows
        is EntryChildListResult.Inapplicable -> emptyList()
    }
    val displayedEpisodes = rows.mapNotNull { (it as? EntryChildListRow.Child)?.chapter }

    return VideoPlayerEpisodeDisplayData(
        chapters = displayedEpisodes,
        playbackStateByChapterId = playbackStateByEpisodeId,
        primaryChapterId = selectPrimaryEpisodeIdForDisplay(
            episodes = filteredEpisodes.sortedForReading(entry, memberIds),
            playbackStateByEpisodeId = playbackStateByEpisodeId,
        ),
        chapterListItems = rows.toEpisodeListItems(
            episodes = displayedEpisodes,
            merged = memberIds.size > 1,
            memberSourceNameById = memberSourceNameById,
        ),
    )
}

private fun List<EntryChildListRow>.toEpisodeListItems(
    episodes: List<EntryChapter>,
    merged: Boolean,
    memberSourceNameById: Map<Long, String>,
): List<VideoPlayerEpisodeListEntry> {
    val episodesByMember = episodes.groupBy(EntryChapter::entryId)
    val episodesByGroup = episodes.groupBy(EntryChapter::groupId)
    return mapNotNull { row ->
        when (row) {
            is EntryChildListRow.Child -> VideoPlayerEpisodeListEntry.Item(row.chapter)
            is EntryChildListRow.MemberHeader -> {
                val memberEpisodes = episodesByMember[row.entryId].orEmpty()
                VideoPlayerEpisodeListEntry.MemberHeader(
                    animeId = row.entryId,
                    title = row.title,
                    sourceName = memberSourceNameById[row.entryId],
                    episodeCount = memberEpisodes.size,
                    unwatchedCount = memberEpisodes.count { !it.read },
                )
            }
            is EntryChildListRow.GroupHeader -> {
                val group = ChapterGroupId(row.entryId, row.groupKey)
                val groupEpisodes = episodesByGroup[group].orEmpty()
                VideoPlayerEpisodeListEntry.GroupHeader(
                    group = group,
                    title = row.title,
                    episodeCount = groupEpisodes.size,
                    unwatchedCount = groupEpisodes.count { !it.read },
                    nested = merged,
                )
            }
            is EntryChildListRow.MissingCount -> null
        }
    }
}

@Immutable
internal sealed class VideoPlayerEpisodeListEntry {
    @Immutable
    data class MemberHeader(
        val animeId: Long,
        val title: String,
        /** Null when every member comes from one source. */
        val sourceName: String?,
        val episodeCount: Int,
        val unwatchedCount: Int,
    ) : VideoPlayerEpisodeListEntry()

    /** A null [title] heads the episodes in no group, and an [episodeCount] of 0 a group with none yet. */
    @Immutable
    data class GroupHeader(
        val group: ChapterGroupId,
        val title: String?,
        val episodeCount: Int,
        val unwatchedCount: Int,
        /** Inside its merged member's card. */
        val nested: Boolean,
    ) : VideoPlayerEpisodeListEntry()

    @Immutable
    data class Item(
        val episode: EntryChapter,
    ) : VideoPlayerEpisodeListEntry()
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
