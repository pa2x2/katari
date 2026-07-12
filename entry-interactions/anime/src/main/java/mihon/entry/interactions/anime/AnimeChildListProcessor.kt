package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.video.player.formatPlaybackTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.EntryChildListProcessor
import mihon.entry.interactions.EntryChildListRequest
import mihon.entry.interactions.EntryChildListRow
import mihon.entry.interactions.EntryChildProgressLabel
import mihon.entry.interactions.EntryChildProgressRequest
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.PlaybackStateRepository
import tachiyomi.domain.entry.service.sortedForMergedDisplay
import tachiyomi.domain.entry.service.sortedForReading
import tachiyomi.i18n.MR

internal class AnimeChildListProcessor(
    private val playbackStateRepository: PlaybackStateRepository,
) : EntryChildListProcessor {
    override val type: EntryType = EntryType.ANIME

    override fun sortedForReading(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> {
        return chapters.sortedForReading(entry, memberIds)
    }

    override fun sortedForDisplay(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> {
        return chapters.sortedForMergedDisplay(entry, memberIds)
    }

    override fun buildDisplayList(request: EntryChildListRequest): List<EntryChildListRow> {
        val sortedChapters = sortedForDisplay(
            entry = request.entry,
            chapters = request.chapters,
            memberIds = request.memberIds,
        )
        if (request.memberIds.size <= 1) {
            return sortedChapters.map(EntryChildListRow::Child)
        }

        return buildList {
            request.memberIds.forEach { memberId ->
                val memberChapters = sortedChapters.filter { it.entryId == memberId }
                if (memberChapters.isEmpty()) return@forEach

                add(
                    EntryChildListRow.MemberHeader(
                        entryId = memberId,
                        title = request.memberTitleById[memberId].orEmpty().ifBlank { request.fallbackTitle },
                    ),
                )
                addAll(memberChapters.map(EntryChildListRow::Child))
            }
        }
    }

    override fun progressLabels(request: EntryChildProgressRequest): Flow<Map<Long, EntryChildProgressLabel>> {
        val stateFlows = request.memberIds
            .distinct()
            .map(playbackStateRepository::getByEntryIdAsFlow)

        if (stateFlows.isEmpty()) {
            return flowOf(emptyMap())
        }

        return combine(stateFlows) { statesByMember ->
            val playbackStateByChapterId = statesByMember
                .flatMap { it }
                .associateBy { it.chapterId }

            request.chapters.mapNotNull { episode ->
                if (episode.read) return@mapNotNull null

                val playbackState = playbackStateByChapterId[episode.id] ?: return@mapNotNull null
                if (playbackState.completed || playbackState.positionMs <= 0L) return@mapNotNull null

                val position = formatPlaybackTimestamp(playbackState.positionMs)
                val label = if (playbackState.durationMs > 0L) {
                    EntryChildProgressLabel(
                        resource = MR.strings.episode_progress_timestamp,
                        args = listOf(position, formatPlaybackTimestamp(playbackState.durationMs)),
                    )
                } else {
                    EntryChildProgressLabel(
                        resource = MR.strings.episode_progress_position,
                        args = listOf(position),
                    )
                }

                episode.id to label
            }.toMap()
        }
    }
}
