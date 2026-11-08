package app.katari.entry.interactions.anime.state

import app.katari.entry.interactions.anime.runtime.requireAnime
import app.katari.entry.interactions.state.EntryConsumptionProcessor
import app.katari.entry.interactions.state.consumptionStatus
import app.katari.entry.interactions.state.shouldChangeConsumption
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class AnimeConsumptionProcessor(
    private val entryProgressRepository: EntryProgressRepository,
) : EntryConsumptionProcessor {

    override suspend fun setConsumed(
        entry: Entry,
        chapters: List<EntryChapter>,
        consumed: Boolean,
    ): List<EntryChapter> {
        entry.requireAnime()
        val chaptersToUpdate = chapters.filter { chapter ->
            val current = entryProgressRepository.get(chapter.entryId, "", chapter.progressResourceKey)
            shouldChangeConsumption(
                chapter.consumptionStatus(hasPartialProgress = current?.hasPartialAnimeProgress == true),
                consumed,
            )
        }
        chaptersToUpdate.forEach { chapter ->
            val current = entryProgressRepository.get(chapter.entryId, "", chapter.progressResourceKey)
            val updated = if (consumed) {
                current?.copy(
                    chapterId = chapter.id,
                    completed = true,
                ) ?: animeProgressState(
                    entryId = chapter.entryId,
                    chapterId = chapter.id,
                    resourceKey = chapter.progressResourceKey,
                    positionMs = 0L,
                    durationMs = 0L,
                    completed = true,
                    locatorUpdatedAt = 0L,
                    completionUpdatedAt = 0L,
                )
            } else {
                (
                    current ?: animeProgressState(
                        entryId = chapter.entryId,
                        chapterId = chapter.id,
                        resourceKey = chapter.progressResourceKey,
                        positionMs = 0L,
                        durationMs = 0L,
                        completed = false,
                        locatorUpdatedAt = 0L,
                        completionUpdatedAt = 0L,
                    )
                    ).copy(
                    chapterId = chapter.id,
                    locator = EntryProgressLocator(kind = ANIME_PROGRESS_LOCATOR_KIND),
                    completed = false,
                )
            }
            entryProgressRepository.upsertAndSyncChild(updated)
        }
        return chaptersToUpdate
    }
}
