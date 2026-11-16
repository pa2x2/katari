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
    private val now: () -> Long = System::currentTimeMillis,
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
        val timestamp = now()
        chaptersToUpdate.forEach { chapter ->
            val held = entryProgressRepository.get(chapter.entryId, "", chapter.progressResourceKey)
                ?: animeProgressState(
                    entryId = chapter.entryId,
                    chapterId = chapter.id,
                    resourceKey = chapter.progressResourceKey,
                    positionMs = 0L,
                    durationMs = 0L,
                    completed = false,
                    locatorUpdatedAt = 0L,
                    completionUpdatedAt = 0L,
                )
            val edit = if (consumed) {
                held.copy(
                    chapterId = chapter.id,
                    completed = true,
                    locatorUpdatedAt = 0L,
                    completionUpdatedAt = timestamp,
                )
            } else {
                held.copy(
                    chapterId = chapter.id,
                    locator = EntryProgressLocator(kind = ANIME_PROGRESS_LOCATOR_KIND),
                    completed = false,
                    locatorUpdatedAt = timestamp,
                    completionUpdatedAt = timestamp,
                )
            }
            entryProgressRepository.editAndSyncChild(edit)
        }
        return chaptersToUpdate
    }
}
