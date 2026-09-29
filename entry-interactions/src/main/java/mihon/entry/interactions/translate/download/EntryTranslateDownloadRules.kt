package mihon.entry.interactions.translate.download

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mihon.entry.interactions.download.EntryAutomaticDownloadFeature
import mihon.entry.interactions.download.EntryDownloadInteraction
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translate.EntryTranslatePreferences
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository

/**
 * Queues downloads for translation as the rules ask: every chapter newly queued for download of a series that
 * translates its downloads, and every chapter automatic download fetches while new chapters are translated. The series
 * setting applies to downloads the category rules skip; chapters already in the translation queue keep their item.
 *
 * Only chapters added to the download queue count, so pausing or restarting downloads does not bring back a
 * translation the user cancelled. Both rules are handled one event at a time so neither queues a chapter twice.
 */
internal class EntryTranslateDownloadRules(
    private val download: EntryDownloadInteraction,
    private val automaticDownload: EntryAutomaticDownloadFeature,
    private val preferences: EntryTranslatePreferences,
    private val languages: EntryTranslationLanguagesFeature,
    private val feature: EntryTranslateFeature,
    private val repository: EntryTranslationQueueRepository,
    private val entries: EntryRepository,
    private val chapters: EntryChapterRepository,
) {
    suspend fun run() {
        // The restored download queue is the baseline; its chapters were queued before this process started.
        download.isInitializing.first { !it }
        merge(newlyQueued(), automaticDownload.scheduled.map { Queued.Automatically(it.entry, it.chapters) })
            .collect { queued ->
                when (queued) {
                    is Queued.ForDownload -> onQueuedForDownload(queued.entryId, queued.chapterIds)
                    is Queued.Automatically -> if (preferences.translateNewChapters.get()) {
                        translate(queued.entry, queued.chapters)
                    }
                }
            }
    }

    private fun newlyQueued(): Flow<Queued.ForDownload> = flow {
        var known: Set<Long>? = null
        download.queueState
            .map { groups -> groups.flatMap { it.items } }
            .collect { items ->
                val previous = known
                known = items.mapTo(HashSet()) { it.childId }
                if (previous == null) return@collect
                items.filter { it.childId !in previous }
                    .groupBy({ it.entryId }, { it.childId })
                    .forEach { (entryId, chapterIds) -> emit(Queued.ForDownload(entryId, chapterIds)) }
            }
    }

    private suspend fun onQueuedForDownload(entryId: Long, chapterIds: List<Long>) {
        val entry = entries.getEntryById(entryId) ?: return
        if (!languages.observe(entry).first().translateDownloads) return
        translate(entry, chapterIds.mapNotNull { chapters.getChapterById(it) })
    }

    private suspend fun translate(entry: Entry, chapters: List<EntryChapter>) {
        val queued = repository.getAll().mapTo(HashSet()) { it.chapterId }
        feature.translateWithCurrentSettings(entry, chapters.filter { it.id !in queued })
    }

    private sealed interface Queued {
        data class ForDownload(val entryId: Long, val chapterIds: List<Long>) : Queued

        data class Automatically(val entry: Entry, val chapters: List<EntryChapter>) : Queued
    }
}
