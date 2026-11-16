package app.katari.entry.interactions.manga.state

import app.katari.entry.interactions.manga.runtime.requireManga
import app.katari.entry.interactions.state.EntryBookmarkProcessor
import app.katari.entry.interactions.state.EntryConsumptionProcessor
import app.katari.entry.interactions.state.consumptionStatus
import app.katari.entry.interactions.state.shouldChangeConsumption
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class MangaConsumptionProcessor(
    private val entryChapterRepository: EntryChapterRepository,
    private val entryProgressRepository: EntryProgressRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : EntryConsumptionProcessor, EntryBookmarkProcessor {

    override suspend fun setConsumed(
        entry: Entry,
        chapters: List<EntryChapter>,
        consumed: Boolean,
    ): List<EntryChapter> {
        entry.requireManga()
        val chaptersToUpdate = chapters.filter { chapter ->
            val progress = entryProgressRepository.get(chapter.entryId, "", chapter.progressResourceKey)
            shouldChangeConsumption(
                chapter.consumptionStatus(hasPartialProgress = progress?.hasPartialMangaProgress == true),
                consumed,
            )
        }
        if (chaptersToUpdate.isEmpty()) return emptyList()

        val timestamp = now()
        chaptersToUpdate.forEach { chapter ->
            val held = entryProgressRepository.get(chapter.entryId, "", chapter.progressResourceKey)
                ?: mangaProgressState(
                    entryId = chapter.entryId,
                    chapterId = chapter.id,
                    resourceKey = chapter.progressResourceKey,
                    pageIndex = null,
                    pageCount = null,
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
                    locator = EntryProgressLocator(kind = MANGA_PROGRESS_LOCATOR_KIND),
                    completed = false,
                    locatorUpdatedAt = timestamp,
                    completionUpdatedAt = timestamp,
                )
            }
            entryProgressRepository.editAndSyncChild(edit)
        }

        return chaptersToUpdate
    }

    override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) {
        entry.requireManga()
        entryChapterRepository.updateAll(chapters.map { it.copy(bookmark = bookmarked) })
    }
}
