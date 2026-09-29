package mihon.entry.interactions.translate.download

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.download.EntryDownloadInteraction
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository

/**
 * Translates every download of a series that translates its downloads: chapters newly queued for download are queued
 * for translation too, unless they already are. Only chapters added to the download queue count, so pausing or
 * restarting downloads does not bring back a translation the user cancelled.
 */
internal class EntryTranslateSeriesDownloads(
    private val download: EntryDownloadInteraction,
    private val languages: EntryTranslationLanguagesFeature,
    private val feature: EntryTranslateFeature,
    private val repository: EntryTranslationQueueRepository,
    private val entries: EntryRepository,
    private val chapters: EntryChapterRepository,
) {
    suspend fun run() {
        // The restored download queue is the baseline; its chapters were queued before this process started.
        download.isInitializing.first { !it }
        var known: Set<Long>? = null
        download.queueState
            .map { groups -> groups.flatMap { it.items } }
            .collect { items ->
                val previous = known
                known = items.mapTo(HashSet()) { it.childId }
                if (previous == null) return@collect
                items.filter { it.childId !in previous }
                    .groupBy({ it.entryId }, { it.childId })
                    .forEach { (entryId, chapterIds) -> onQueued(entryId, chapterIds) }
            }
    }

    private suspend fun onQueued(entryId: Long, chapterIds: List<Long>) {
        val entry = entries.getEntryById(entryId) ?: return
        if (!languages.observe(entry).first().translateDownloads) return
        val queued = repository.getAll().mapTo(HashSet()) { it.chapterId }
        val added = chapterIds.filter { it !in queued }.mapNotNull { chapters.getChapterById(it) }
        feature.translateWithCurrentSettings(entry, added)
    }
}
