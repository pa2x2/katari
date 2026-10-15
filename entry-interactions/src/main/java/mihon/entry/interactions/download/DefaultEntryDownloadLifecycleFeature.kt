package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.sync.Mutex
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.sortedForReading
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences

/** Interprets media-neutral lifecycle events as shared cleanup and download-ahead policy. */
internal class DefaultEntryDownloadLifecycleFeature(
    private val downloadTypes: Set<EntryType>,
    private val bookmarkProtectedTypes: Set<EntryType>,
    private val downloadPreferences: DownloadPreferences,
    private val getCategories: GetCategories,
    private val getEntryWithChapters: GetEntryWithChapters,
    entryRepository: EntryRepository,
    private val downloads: EntryDownloadInteraction,
    private val globalLibraryPreferences: GlobalLibraryPreferences,
) : EntryDownloadLifecycleFeature {
    private val ownerResolver = EntryDownloadOwnerResolver(entryRepository)
    private val eventMutex = Mutex()
    private val downloadAheadTriggered = mutableSetOf<EntryDownloadIdentity>()

    override fun isApplicable(type: EntryType): Boolean = type in downloadTypes

    override suspend fun onEvent(event: EntryDownloadLifecycleEvent): EntryDownloadLifecycleResult {
        if (!isApplicable(event.visibleEntry.type)) {
            return EntryDownloadLifecycleResult.Inapplicable(event.visibleEntry.type)
        }
        eventMutex.lock()
        try {
            when (event) {
                is EntryDownloadLifecycleEvent.MarkedConsumed -> afterMarkedConsumed(event)
                is EntryDownloadLifecycleEvent.Progressed -> afterProgressed(event)
                is EntryDownloadLifecycleEvent.Completed -> afterCompleted(event)
            }
        } finally {
            eventMutex.unlock()
        }
        return EntryDownloadLifecycleResult.Handled
    }

    private suspend fun afterMarkedConsumed(event: EntryDownloadLifecycleEvent.MarkedConsumed) {
        val enabled = downloadPreferences.removeAfterMarkedAsRead.get()
        if (!enabled) return
        deleteEligible(event.visibleEntry, event.children, deferUntilViewerCloses = false)
    }

    private suspend fun afterProgressed(event: EntryDownloadLifecycleEvent.Progressed) {
        val amount = downloadPreferences.autoDownloadWhileReading.get()
        val progressEligible = event.fraction.isFinite() && event.fraction >= DOWNLOAD_AHEAD_THRESHOLD
        val enabled = amount > 0
        if (!progressEligible || !enabled) return

        val currentOwner = ownerResolver.resolve(event.visibleEntry, listOf(event.child)).singleOrNull() ?: return
        if (!isApplicable(currentOwner.entry.type)) return
        val identity = EntryDownloadIdentity.from(currentOwner.entry, event.child)
        if (identity in downloadAheadTriggered) return

        val readingOrder = readingOrder(event.visibleEntry, event.child, event.deduplicateByNumber)
        val currentIndex = readingOrder.indexOfFirst { it.id == event.child.id }
        if (currentIndex < 0) return
        val next = readingOrder.getOrNull(currentIndex + 1) ?: return
        val nextOwner = ownerResolver.resolve(event.visibleEntry, listOf(next)).singleOrNull() ?: return
        if (!isApplicable(nextOwner.entry.type)) return
        if (
            !downloads.isDownloaded(currentOwner.entry, event.child) ||
            !downloads.isDownloaded(nextOwner.entry, next)
        ) {
            return
        }

        val candidates = mutableListOf<EntryDownloadOwner>()
        for (child in readingOrder.drop(currentIndex + 1).filterNot(EntryChapter::read)) {
            val owner = ownerResolver.resolve(event.visibleEntry, listOf(child)).singleOrNull() ?: continue
            if (!isApplicable(owner.entry.type)) continue
            if (!downloads.isDownloaded(owner.entry, child)) candidates += owner
            if (candidates.size == amount) break
        }
        if (candidates.isEmpty()) return

        candidates.groupBy { it.entry.id }.values.forEach { owners ->
            downloads.queue(
                entry = owners.first().entry,
                chapters = owners.flatMap(EntryDownloadOwner::children),
                autoStart = false,
            )
        }
        downloads.startDownloads()
        downloadAheadTriggered += identity
    }

    private suspend fun afterCompleted(event: EntryDownloadLifecycleEvent.Completed) {
        afterProgressed(
            EntryDownloadLifecycleEvent.Progressed(
                visibleEntry = event.visibleEntry,
                child = event.child,
                fraction = 1.0,
                deduplicateByNumber = event.deduplicateByNumber,
            ),
        )

        val removeAfterReadSlots = downloadPreferences.removeAfterReadSlots.get()
        val cleanupEnabled = removeAfterReadSlots >= 0
        if (cleanupEnabled) {
            val readingOrder = readingOrder(event.visibleEntry, event.child, event.deduplicateByNumber)
            val currentIndex = readingOrder.indexOfFirst { it.id == event.child.id }
            readingOrder.getOrNull(currentIndex - removeAfterReadSlots)?.let { child ->
                val duplicates = if (event.completeEquivalentChildrenByNumber) {
                    completedDuplicates(event.visibleEntry, child)
                } else {
                    emptyList()
                }
                deleteEligible(event.visibleEntry, listOf(child) + duplicates, deferUntilViewerCloses = true)
            }
        }
        val currentOwner = ownerResolver.resolve(event.visibleEntry, listOf(event.child)).singleOrNull()
        currentOwner?.let { downloadAheadTriggered -= EntryDownloadIdentity.from(it.entry, event.child) }
    }

    private suspend fun deleteEligible(
        visibleEntry: Entry,
        children: List<EntryChapter>,
        deferUntilViewerCloses: Boolean,
    ) {
        ownerResolver.resolve(visibleEntry, children).forEach { owner ->
            if (!isApplicable(owner.entry.type)) return@forEach
            val categoryAllowed = !isExcluded(owner.entry)
            if (!categoryAllowed) return@forEach
            val removeBookmarked = downloadPreferences.removeBookmarkedChapters.get()
            val eligible = if (
                owner.entry.type !in bookmarkProtectedTypes ||
                removeBookmarked
            ) {
                owner.children
            } else {
                owner.children.filterNot(EntryChapter::bookmark)
            }
            if (eligible.isNotEmpty()) {
                if (deferUntilViewerCloses) {
                    downloads.cleanup(owner.entry, eligible)
                } else {
                    downloads.delete(owner.entry, eligible)
                }
            }
        }
    }

    // Completing a chapter also completes its duplicates when the user asked for that, which leaves their downloads
    // as read as the chapter's own
    private suspend fun completedDuplicates(visibleEntry: Entry, child: EntryChapter): List<EntryChapter> {
        val duplicateCompletion = LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_EXISTING in
            globalLibraryPreferences.markDuplicateReadChapterAsRead.get()
        if (!duplicateCompletion || !child.read || !child.isRecognizedNumber) return emptyList()
        return getEntryWithChapters.awaitChapters(visibleEntry).filter {
            it.id != child.id && it.read && it.isRecognizedNumber && it.chapterNumber == child.chapterNumber
        }
    }

    private suspend fun isExcluded(entry: Entry): Boolean {
        val excluded = downloadPreferences.removeExcludeCategories.get()
            .mapNotNull(String::toLongOrNull)
            .toSet()
        if (excluded.isEmpty()) return false
        val categories = getCategories.await(entry.id).map { it.id }.ifEmpty { listOf(DEFAULT_CATEGORY_ID) }
        return categories.any { it in excluded }
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
        const val DEFAULT_CATEGORY_ID = 0L
        const val DOWNLOAD_AHEAD_THRESHOLD = 0.25
    }
}
