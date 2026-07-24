package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryLibraryProgressEvidence
import mihon.entry.interactions.EntryLibraryProgressProvider
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class AnimeLibraryProgressProvider(
    private val entryProgressRepository: EntryProgressRepository,
) : EntryLibraryProgressProvider {
    override val type = EntryType.ANIME

    override suspend fun evidence(entry: Entry, chapters: List<EntryChapter>): EntryLibraryProgressEvidence {
        entry.requireAnime()
        val chapterIds = chapters.mapTo(mutableSetOf(), EntryChapter::id)
        val playbackStates = entryProgressRepository.getByEntryId(entry.id)
            .filter { it.chapterId in chapterIds }
        val inProgress = playbackStates
            .asSequence()
            .filter { !it.completed && it.positionMs > 0L && it.durationMs > 0L }
            .maxByOrNull { it.lastWatchedAt }

        return EntryLibraryProgressEvidence(
            hasMediaProgress = playbackStates.any { it.positionMs > 0L },
            inProgressItemId = inProgress?.chapterId,
            inProgressFraction = inProgress?.let {
                (it.positionMs.toFloat() / it.durationMs.toFloat()).coerceIn(0f, 1f)
            },
            lastActivityAt = playbackStates.maxOfOrNull { it.lastWatchedAt } ?: 0L,
        )
    }
}
