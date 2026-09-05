package mihon.entry.interactions.book.download

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.runtime.requireBook
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadOwnerResolver
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.download.EntryDownloadQueueGroup
import mihon.entry.interactions.download.EntryDownloadQueueItem
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.download.EntryDownloadStatus
import mihon.entry.interactions.download.observeEntryDownloadQueue
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager

internal class BookDownloadProcessor(
    private val dependencies: BookDownloadProcessorDependencies,
) : EntryDownloadProcessor,
    EntryBulkDownloadCandidateProcessor {
    private val manager: BookDownloadManager = dependencies.manager
    private val cache: BookDownloadCache = dependencies.cache
    private val ownerResolver = EntryDownloadOwnerResolver(dependencies.entryRepository)

    override val type: EntryType = EntryType.BOOK
    override val changes: Flow<Unit> = manager.cacheChanges.onStart {
        cache.awaitInitialSnapshot()
        emit(Unit)
    }
    override val isInitializing: Flow<Boolean> = cache.isInitializing
    override val isRunning: Flow<Boolean> = manager.isRunning
    override val queueState: Flow<List<EntryDownloadQueueGroup>> = observeEntryDownloadQueue(
        queue = manager.queueState,
        statusUpdates = manager.statusFlow(),
        progressUpdates = manager.progressFlow(),
    ) { downloads -> downloads.toBookEntryDownloadQueueGroups(dependencies.sourceManager) }
    override val events = manager.events

    override fun updates(): Flow<EntryDownloadStatus> = merge(
        manager.statusFlow().map(BookDownload::toEntryDownloadStatus),
        manager.progressFlow().map(BookDownload::toEntryDownloadStatus),
        manager.cachePackageUpdates.map(BookDownloadPackageUpdate::toEntryDownloadStatus),
    )

    override suspend fun hasPendingDownloads(): Boolean = manager.hasPendingDownloads()

    override suspend fun runDownloadsUntilIdle() {
        manager.runDownloads()
    }

    override fun startDownloads() = manager.startDownloads()

    override fun pauseDownloads() = manager.pauseDownloads()

    override fun clearQueue() = manager.clearQueue()

    override fun invalidateCache() = manager.invalidateCache()

    override fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        manager.renameSource(oldSource, newSource)
    }

    override suspend fun renameEntry(entry: Entry, newTitle: String) {
        entry.requireBook()
        manager.renameEntry(entry, newTitle)
    }

    override fun reorderQueue(items: List<EntryDownloadQueueItem>) {
        require(items.all { it.entryType == EntryType.BOOK })
        manager.reorderQueue(
            items.mapNotNull { item -> manager.queueState.value.firstOrNull { it.chapter.id == item.childId } },
        )
    }

    override fun reorderSeries(entryId: Long, moveToTop: Boolean) {
        val (series, others) = manager.queueState.value.partition { it.entry.id == entryId }
        manager.reorderQueue(if (moveToTop) series + others else others + series)
    }

    override fun cancelQueuedDownloads(items: List<EntryDownloadQueueItem>) {
        require(items.all { it.entryType == EntryType.BOOK })
        manager.removeFromQueue(items.map(EntryDownloadQueueItem::childId))
    }

    override suspend fun queue(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean) {
        entry.requireBook()
        queueByOwner(entry, chapters, autoStart)
    }

    override suspend fun download(entry: Entry, chapters: List<EntryChapter>, startNow: Boolean) {
        entry.requireBook()
        queueByOwner(entry, chapters, autoStart = false)
        if (startNow) {
            manager.startDownloadsNow(chapters.map(EntryChapter::id))
        } else {
            manager.startDownloads()
        }
    }

    override suspend fun resolveBulkDownloadCandidatePool(
        entry: Entry,
        candidates: List<EntryChapter>?,
    ): List<EntryChapter> {
        entry.requireBook()
        return (candidates ?: dependencies.getEntryWithChapters.awaitChapters(entry))
            .filterNot { isDownloadedByOwner(entry, it) }
    }

    override suspend fun delete(entry: Entry, chapters: List<EntryChapter>) {
        entry.requireBook()
        manager.delete(entry, chapters)
    }

    override suspend fun deleteEntryDownloads(entry: Entry): Boolean {
        entry.requireBook()
        return manager.deleteEntryDownloads(entry)
    }

    override fun hasDownloads(entry: Entry): Boolean = getDownloadCount(entry) > 0

    override fun getDownloadCount(entry: Entry): Int {
        entry.requireBook()
        return cache.getDownloadCount(entry)
    }

    override fun getTotalDownloadCount(): Int = cache.getTotalDownloadCount()

    override fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean): Boolean {
        entry.requireBook()
        if (skipCache) manager.invalidateCache()
        return cache.isDownloaded(BookDownloadPackageKey(entry.source, entry.url, chapter.url))
    }

    override fun getStatus(
        chapterId: Long,
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        entryTitle: String,
        sourceId: Long,
    ): EntryDownloadStatus = manager.queueState.value
        .firstOrNull { it.chapter.id == chapterId }
        ?.toEntryDownloadStatus()
        ?: EntryDownloadStatus(
            entryType = EntryType.BOOK,
            chapterId = chapterId,
            state = if (cache.find(sourceId, chapterUrl, entryTitle) != null) {
                EntryDownloadState.DOWNLOADED
            } else {
                EntryDownloadState.NOT_DOWNLOADED
            },
        )

    override fun cancelQueuedDownload(chapterId: Long): EntryDownloadStatus? {
        val download = manager.queueState.value.firstOrNull { it.chapter.id == chapterId } ?: return null
        manager.removeFromQueue(listOf(chapterId))
        download.status = BookDownload.State.NOT_DOWNLOADED
        return download.toEntryDownloadStatus()
    }

    private suspend fun queueByOwner(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean) {
        val owners = ownerResolver.resolve(entry, chapters)
        owners.forEach { owner ->
            manager.queueBooks(owner.entry, owner.children, autoStart = false)
        }
        if (autoStart && owners.isNotEmpty()) manager.startDownloads()
    }

    private suspend fun isDownloadedByOwner(visibleEntry: Entry, chapter: EntryChapter): Boolean {
        val owner = if (chapter.entryId == visibleEntry.id) {
            visibleEntry
        } else {
            dependencies.entryRepository.getEntryById(chapter.entryId)
        } ?: return false
        return cache.isDownloaded(BookDownloadPackageKey(owner.source, owner.url, chapter.url))
    }
}

private fun BookDownloadPackageUpdate.toEntryDownloadStatus() = EntryDownloadStatus(
    entryType = EntryType.BOOK,
    chapterId = manifest.childId,
    state = if (downloaded) EntryDownloadState.DOWNLOADED else EntryDownloadState.NOT_DOWNLOADED,
    entryId = manifest.entryId,
    sourceId = manifest.sourceId,
    persistedContentChanged = true,
)

internal data class BookDownloadProcessorDependencies(
    val manager: BookDownloadManager,
    val cache: BookDownloadCache,
    val sourceManager: SourceManager,
    val entryRepository: EntryRepository,
    val getEntryWithChapters: GetEntryWithChapters,
)
