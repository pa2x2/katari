package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryBookmarkProcessor
import mihon.entry.interactions.EntryConsumptionProcessor
import mihon.entry.interactions.consumptionStatus
import mihon.entry.interactions.shouldChangeConsumption
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class MangaConsumptionProcessor(
    private val entryChapterRepository: EntryChapterRepository,
    private val entryProgressRepository: EntryProgressRepository,
) : EntryConsumptionProcessor, EntryBookmarkProcessor {
    override val type: EntryType = EntryType.MANGA

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

        chaptersToUpdate.forEach { chapter ->
            val current = entryProgressRepository.get(chapter.entryId, "", chapter.progressResourceKey)
            val updated = if (consumed) {
                current?.copy(
                    chapterId = chapter.id,
                    completed = true,
                ) ?: mangaProgressState(
                    entryId = chapter.entryId,
                    chapterId = chapter.id,
                    resourceKey = chapter.progressResourceKey,
                    pageIndex = null,
                    pageCount = null,
                    completed = true,
                    locatorUpdatedAt = 0L,
                    completionUpdatedAt = 0L,
                )
            } else {
                (
                    current ?: mangaProgressState(
                        entryId = chapter.entryId,
                        chapterId = chapter.id,
                        resourceKey = chapter.progressResourceKey,
                        pageIndex = null,
                        pageCount = null,
                        completed = false,
                        locatorUpdatedAt = 0L,
                        completionUpdatedAt = 0L,
                    )
                    ).copy(
                    chapterId = chapter.id,
                    locator = EntryProgressLocator(kind = MANGA_PROGRESS_LOCATOR_KIND),
                    completed = false,
                )
            }
            entryProgressRepository.upsertAndSyncChild(updated)
        }

        return chaptersToUpdate
    }

    override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) {
        entry.requireManga()
        entryChapterRepository.updateAll(chapters.map { it.copy(bookmark = bookmarked) })
    }
}
