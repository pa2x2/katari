package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryConsumptionProcessor
import mihon.entry.interactions.consumptionStatus
import mihon.entry.interactions.manga.download.DownloadManager
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.source.service.SourceManager

internal class MangaConsumptionProcessor(
    private val entryChapterRepository: EntryChapterRepository,
    private val entryProgressRepository: EntryProgressRepository,
    private val downloadPreferences: DownloadPreferences,
    private val downloadManager: DownloadManager,
    private val sourceManager: SourceManager,
) : EntryConsumptionProcessor {
    override val type: EntryType = EntryType.MANGA
    override val supportsBookmark: Boolean = true

    override suspend fun setConsumed(entry: Entry, chapters: List<EntryChapter>, consumed: Boolean) {
        entry.requireManga()
        val chaptersToUpdate = chapters.filter { chapter ->
            val progress = entryProgressRepository.get(chapter.entryId, "", chapter.progressResourceKey)
            canSetConsumed(
                chapter.consumptionStatus(hasPartialProgress = progress?.hasPartialMangaProgress == true),
                consumed,
            )
        }
        if (chaptersToUpdate.isEmpty()) return

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

        if (consumed && downloadPreferences.removeAfterMarkedAsRead.get()) {
            downloadManager.deleteChapters(chaptersToUpdate, entry, sourceManager.getOrStub(entry.source))
        }
    }

    override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) {
        entry.requireManga()
        val chaptersToUpdate = chapters
            .filter { canSetBookmarked(it.consumptionStatus(), bookmarked) }
            .map { it.copy(bookmark = bookmarked) }
        if (chaptersToUpdate.isEmpty()) return

        entryChapterRepository.updateAll(chaptersToUpdate)
    }
}
