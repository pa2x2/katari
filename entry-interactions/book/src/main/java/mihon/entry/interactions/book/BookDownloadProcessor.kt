package mihon.entry.interactions.book

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryBulkDownloadAction
import mihon.entry.interactions.EntryBulkDownloadActionType
import mihon.entry.interactions.EntryBulkDownloadCandidateResult
import mihon.entry.interactions.EntryDownloadProcessor
import mihon.entry.interactions.EntryDownloadQueueGroup
import mihon.entry.interactions.EntryDownloadQueueItem
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryDownloadStatus
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadManager
import mihon.entry.interactions.book.download.BookDownloadPackageKey
import mihon.entry.interactions.book.download.model.BookDownload
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.sortedForReading

internal class BookDownloadProcessor(
    private val dependencies: BookDownloadProcessorDependencies,
) : EntryDownloadProcessor {
    private val manager: BookDownloadManager = dependencies.manager
    private val cache: BookDownloadCache = dependencies.cache
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            dependencies.mergedEntryRepository.subscribeAll().collect(cache::updateMergedEntries)
        }
    }

    override val type: EntryType = EntryType.BOOK
    override val changes: Flow<Unit> = merge(
        manager.cacheChanges,
        manager.queueState.map { Unit },
    )
    override val isInitializing: Flow<Boolean> = cache.isInitializing
    override val isRunning: Flow<Boolean> = manager.isRunning
    override val queueState: Flow<List<EntryDownloadQueueGroup>> = manager.queueState.map {
        it.toBookEntryDownloadQueueGroups(dependencies.sourceManager)
    }

    override fun updates(): Flow<EntryDownloadStatus> = merge(
        manager.statusFlow().map(BookDownload::toEntryDownloadStatus),
        manager.progressFlow().map(BookDownload::toEntryDownloadStatus),
    )

    override fun queueStatusUpdates(): Flow<EntryDownloadQueueItem> =
        manager.statusFlow().map(BookDownload::toEntryDownloadQueueItem)

    override fun queueProgressUpdates(): Flow<EntryDownloadQueueItem> =
        manager.progressFlow().map(BookDownload::toEntryDownloadQueueItem)

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
        queueByOwner(entry, chapters, autoStart = !startNow)
        if (startNow) chapters.singleOrNull()?.id?.let(manager::startDownloadNow)
    }

    override fun supportsDownloadOptions(entry: Entry): Boolean {
        entry.requireBook()
        return false
    }

    override suspend fun resolveDownloadOptions(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
    ) = null

    override fun supportsBulkDownload(entry: Entry): Boolean {
        entry.requireBook()
        return true
    }

    override suspend fun resolveBulkDownloadCandidates(
        entry: Entry,
        action: EntryBulkDownloadAction,
        candidates: List<EntryChapter>?,
        memberEntryIds: List<Long>,
    ): EntryBulkDownloadCandidateResult {
        entry.requireBook()
        if (action.type == EntryBulkDownloadActionType.BOOKMARKED) {
            return EntryBulkDownloadCandidateResult.Unsupported
        }
        val chapters = (candidates ?: dependencies.getEntryWithChapters.awaitChapters(entry.id))
            .filterNot { it.read }
            .filterNot { isDownloadedByOwner(entry, it) }
        val ordered = chapters
            .filterNot { it.read }
            .sortedForReading(entry, memberEntryIds.ifEmpty { chapters.map(EntryChapter::entryId).distinct() })
        return EntryBulkDownloadCandidateResult.Supported(
            chapters = when (action.type) {
                EntryBulkDownloadActionType.NEXT -> action.limit?.let(ordered::take) ?: ordered
                EntryBulkDownloadActionType.UNREAD -> ordered
                EntryBulkDownloadActionType.BOOKMARKED -> error("Handled above")
            },
        )
    }

    override suspend fun filterAutoDownloadCandidates(
        entry: Entry,
        chapters: List<EntryChapter>,
    ): List<EntryChapter> {
        entry.requireBook()
        return dependencies.filterEntryChaptersForDownload.await(entry, chapters)
    }

    override suspend fun delete(entry: Entry, chapters: List<EntryChapter>) {
        entry.requireBook()
        manager.delete(entry, chapters)
    }

    override suspend fun deleteEntryDownloads(entry: Entry) {
        entry.requireBook()
        manager.deleteEntryDownloads(entry, cache.memberEntryIds(entry.id))
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
        val downloadsByOwner = chapters.groupBy(EntryChapter::entryId).mapNotNull { (ownerId, ownerChapters) ->
            val owner = if (ownerId == entry.id) entry else dependencies.entryRepository.getEntryById(ownerId)
            owner?.takeIf { it.type == EntryType.BOOK }?.let { it to ownerChapters }
        }
        downloadsByOwner.forEach { (owner, ownerChapters) ->
            manager.queueBooks(owner, ownerChapters, autoStart = false)
        }
        if (autoStart && downloadsByOwner.isNotEmpty()) manager.startDownloads()
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

internal data class BookDownloadProcessorDependencies(
    val manager: BookDownloadManager,
    val cache: BookDownloadCache,
    val sourceManager: tachiyomi.domain.source.service.SourceManager,
    val entryRepository: EntryRepository,
    val getEntryWithChapters: tachiyomi.domain.entry.interactor.GetEntryWithChapters,
    val filterEntryChaptersForDownload: mihon.domain.chapter.interactor.FilterEntryChaptersForDownload,
    val mergedEntryRepository: tachiyomi.domain.entry.repository.MergedEntryRepository,
)
