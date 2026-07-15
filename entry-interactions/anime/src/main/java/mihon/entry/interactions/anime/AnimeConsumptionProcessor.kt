package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryConsumptionProcessor
import mihon.entry.interactions.consumptionStatus
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class AnimeConsumptionProcessor(
    private val entryProgressRepository: EntryProgressRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : EntryConsumptionProcessor {
    override val type: EntryType = EntryType.ANIME
    override val supportsBookmark: Boolean = false

    override suspend fun setConsumed(entry: Entry, chapters: List<EntryChapter>, consumed: Boolean) {
        entry.requireAnime()
        val chaptersToUpdate = chapters.filter { canSetConsumed(it.consumptionStatus(), consumed) }
        if (chaptersToUpdate.isEmpty()) return

        val timestamp = now()
        chaptersToUpdate.forEach { chapter ->
            val current = entryProgressRepository.get(chapter.entryId, "", chapter.url)
            val updated = if (consumed) {
                current?.copy(
                    chapterId = chapter.id,
                    completed = true,
                    completionUpdatedAt = timestamp,
                ) ?: animeProgressState(
                    entryId = chapter.entryId,
                    chapterId = chapter.id,
                    resourceKey = chapter.url,
                    positionMs = 0L,
                    durationMs = 0L,
                    completed = true,
                    locatorUpdatedAt = 0L,
                    completionUpdatedAt = timestamp,
                )
            } else {
                (
                    current ?: animeProgressState(
                        entryId = chapter.entryId,
                        chapterId = chapter.id,
                        resourceKey = chapter.url,
                        positionMs = 0L,
                        durationMs = 0L,
                        completed = false,
                        locatorUpdatedAt = timestamp,
                        completionUpdatedAt = timestamp,
                    )
                    ).copy(
                    chapterId = chapter.id,
                    locator = EntryProgressLocator(kind = ANIME_PROGRESS_LOCATOR_KIND),
                    completed = false,
                    locatorUpdatedAt = timestamp,
                    completionUpdatedAt = timestamp,
                )
            }
            entryProgressRepository.mergeAndSyncChild(updated)
        }
    }

    override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) {
        entry.requireAnime()
    }
}
