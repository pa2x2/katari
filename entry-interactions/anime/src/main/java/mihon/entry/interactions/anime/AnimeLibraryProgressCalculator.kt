package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.first
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.PlaybackState
import tachiyomi.domain.entry.repository.PlaybackStateRepository
import tachiyomi.domain.entry.service.EntryLibraryProgressCalculator
import tachiyomi.domain.entry.service.EntryLibraryState
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.ProgressState

fun animeEntryLibraryProgressCalculator(
    playbackStateRepository: PlaybackStateRepository,
): EntryLibraryProgressCalculator {
    return AnimeLibraryProgressCalculator(playbackStateRepository)
}

private class AnimeLibraryProgressCalculator(
    private val playbackStateRepository: PlaybackStateRepository,
) : EntryLibraryProgressCalculator {
    override val entryType = EntryType.ANIME

    override suspend fun calculate(
        entry: Entry,
        chapters: List<EntryChapter>,
        lastRead: Long,
    ): EntryLibraryState {
        entry.requireAnime()

        val playbackStates = getPlaybackStates(entry, chapters)
        val inProgressPlayback = playbackStates
            .asSequence()
            .filter { !it.completed && it.positionMs > 0L && it.durationMs > 0L }
            .maxByOrNull { it.lastWatchedAt }

        return EntryLibraryState(
            progress = ProgressState(
                totalCount = chapters.size.toLong(),
                consumedCount = chapters.count { it.read }.toLong(),
                inProgressItemId = inProgressPlayback?.chapterId,
                inProgressFraction = inProgressPlayback?.let {
                    (it.positionMs.toFloat() / it.durationMs.toFloat()).coerceIn(0f, 1f)
                },
                hasStarted = chapters.any { it.read } || playbackStates.any { it.positionMs > 0L },
                continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE,
            ),
            lastRead = playbackStates.maxOfOrNull { it.lastWatchedAt } ?: 0L,
            continueEntryId = selectPrimaryEpisode(chapters, playbackStates)?.id,
        )
    }

    override fun merge(members: List<LibraryItem>): EntryLibraryState {
        val progress = ProgressState(
            totalCount = members.sumOf { it.totalCount },
            consumedCount = members.sumOf { it.consumedCount },
            inProgressItemId = members.firstNotNullOfOrNull { it.progress.inProgressItemId },
            inProgressFraction = members.firstNotNullOfOrNull { it.progress.inProgressFraction },
            hasStarted = members.any { it.hasStarted },
            continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE,
        )

        return EntryLibraryState(
            progress = progress,
            lastRead = members.maxOfOrNull { it.lastRead } ?: 0L,
            continueEntryId = progress.inProgressItemId ?: members.firstNotNullOfOrNull { it.continueEntryId },
        )
    }

    private suspend fun getPlaybackStates(
        entry: Entry,
        chapters: List<EntryChapter>,
    ): List<PlaybackState> {
        val chapterIds = chapters.map { it.id }.toSet()
        return playbackStateRepository.getByEntryIdAsFlow(entry.id)
            .first()
            .filter { it.chapterId in chapterIds }
    }

    private fun selectPrimaryEpisode(
        chapters: List<EntryChapter>,
        playbackStates: List<PlaybackState>,
    ): EntryChapter? {
        val playbackStateByChapterId by lazy { playbackStates.associateBy { it.chapterId } }
        val inProgressChapter = chapters
            .asSequence()
            .mapNotNull { chapter -> playbackStateByChapterId[chapter.id] }
            .filter { !it.completed && it.positionMs > 0L && it.durationMs > 0L }
            .maxByOrNull { it.lastWatchedAt }
            ?.let { playbackState -> chapters.firstOrNull { it.id == playbackState.chapterId } }

        if (inProgressChapter != null) return inProgressChapter
        return chapters.firstOrNull { !it.read } ?: chapters.firstOrNull()
    }
}
