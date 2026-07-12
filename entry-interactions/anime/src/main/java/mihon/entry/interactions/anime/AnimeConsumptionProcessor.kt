package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryConsumptionProcessor
import mihon.entry.interactions.consumptionStatus
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.PlaybackStateRepository

internal class AnimeConsumptionProcessor(
    private val entryChapterRepository: EntryChapterRepository,
    private val playbackStateRepository: PlaybackStateRepository,
) : EntryConsumptionProcessor {
    override val type: EntryType = EntryType.ANIME
    override val supportsBookmark: Boolean = false

    override suspend fun setConsumed(entry: Entry, chapters: List<EntryChapter>, consumed: Boolean) {
        entry.requireAnime()
        val chaptersToUpdate = chapters.filter { canSetConsumed(it.consumptionStatus(), consumed) }
        if (chaptersToUpdate.isEmpty()) return

        entryChapterRepository.updateAll(
            chaptersToUpdate.map { it.copy(read = consumed) },
        )

        chapters.forEach { chapter ->
            playbackStateRepository.getByChapterId(chapter.id)
                ?.let { playbackState ->
                    playbackStateRepository.upsert(
                        playbackState.copy(
                            positionMs = if (consumed) playbackState.positionMs else 0L,
                            completed = consumed,
                        ),
                    )
                }
        }
    }

    override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) {
        entry.requireAnime()
    }
}
