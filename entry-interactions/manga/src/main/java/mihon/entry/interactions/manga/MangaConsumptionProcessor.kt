package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryConsumptionProcessor
import mihon.entry.interactions.EntryConsumptionStatus
import mihon.entry.interactions.consumptionStatus
import mihon.entry.interactions.manga.download.DownloadManager
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.service.SourceManager

internal class MangaConsumptionProcessor(
    private val entryChapterRepository: EntryChapterRepository,
    private val downloadPreferences: DownloadPreferences,
    private val downloadManager: DownloadManager,
    private val sourceManager: SourceManager,
) : EntryConsumptionProcessor {
    override val type: EntryType = EntryType.MANGA
    override val supportsBookmark: Boolean = true

    override fun canSetConsumed(status: EntryConsumptionStatus, consumed: Boolean): Boolean {
        return when (consumed) {
            true -> !status.consumed
            false -> status.consumed || status.hasPartialProgress
        }
    }

    override suspend fun setConsumed(entry: Entry, chapters: List<EntryChapter>, consumed: Boolean) {
        entry.requireManga()
        val chaptersToUpdate = chapters.filter { canSetConsumed(it.consumptionStatus(), consumed) }
        if (chaptersToUpdate.isEmpty()) return

        entryChapterRepository.updateAll(
            chaptersToUpdate.map {
                it.copy(
                    read = consumed,
                    lastPageRead = if (!consumed) 0 else it.lastPageRead,
                )
            },
        )

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
