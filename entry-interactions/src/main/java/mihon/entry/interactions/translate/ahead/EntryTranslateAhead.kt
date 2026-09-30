package mihon.entry.interactions.translate.ahead

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.entry.interactions.download.EntryDownloadInteraction
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translate.EntryTranslatePreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.sortedForReading

/**
 * Translates the next unread chapters ahead of the reader once a quarter of the current one is read, as download ahead
 * downloads them. Only chapters that are downloaded or queued for download can be translated; chapters already
 * translated or in the translation queue are left alone.
 */
internal class EntryTranslateAhead(
    private val preferences: EntryTranslatePreferences,
    private val feature: EntryTranslateFeature,
    private val download: EntryDownloadInteraction,
    private val getEntryWithChapters: GetEntryWithChapters,
    private val entries: EntryRepository,
) : EntryTranslateAheadTrigger {
    private val mutex = Mutex()

    /** Chapters whose progress already triggered translation ahead in this process. */
    private val triggered = mutableSetOf<Long>()

    override suspend fun onProgressed(
        visibleEntry: Entry,
        child: EntryChapter,
        fraction: Double,
        deduplicateByNumber: Boolean,
    ) {
        val amount = preferences.translateAheadWhileReading.get()
        if (amount <= 0 || !fraction.isFinite() || fraction < THRESHOLD) return
        mutex.withLock {
            if (child.id in triggered) return
            val order = readingOrder(visibleEntry, child, deduplicateByNumber)
            val index = order.indexOfFirst { it.id == child.id }
            if (index < 0) return
            triggered += child.id
            val queuedForDownload = download.queueState.first()
                .flatMap { it.items }
                .filter { it.state != EntryDownloadState.ERROR }
                .mapTo(HashSet()) { it.childId }
            order.drop(index + 1)
                .filterNot(EntryChapter::read)
                .take(amount)
                .groupBy(EntryChapter::entryId)
                .forEach { (entryId, upcoming) ->
                    val entry = entries.getEntryById(entryId) ?: return@forEach
                    if (!feature.isApplicable(entry.type)) return@forEach
                    val statuses = feature.observeStatuses(entry).first()
                    val chapters = upcoming.filter { chapter ->
                        chapter.id !in statuses &&
                            (chapter.id in queuedForDownload || download.isDownloaded(entry, chapter))
                    }
                    feature.translateWithCurrentSettings(entry, chapters)
                }
        }
    }

    private suspend fun readingOrder(
        visibleEntry: Entry,
        currentChild: EntryChapter,
        deduplicateByNumber: Boolean,
    ): List<EntryChapter> {
        val ordered = getEntryWithChapters.awaitChapters(visibleEntry).sortedForReading(visibleEntry)
        if (!deduplicateByNumber) return ordered
        return ordered.groupBy { it.entryId to it.chapterNumber }.values.map { children ->
            children.find { it.id == currentChild.id }
                ?: children.find { it.scanlator == currentChild.scanlator }
                ?: children.first()
        }
    }

    private companion object {
        /** The share of a chapter read before the next ones are translated, as for download ahead. */
        const val THRESHOLD = 0.25
    }
}
