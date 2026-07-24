package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mihon.entry.interactions.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.EntryDownloadOwnerResolver
import mihon.entry.interactions.EntryDownloadProcessor
import mihon.entry.interactions.EntryDownloadQueueGroup
import mihon.entry.interactions.EntryDownloadQueueItem
import mihon.entry.interactions.EntryDownloadSettingProvider
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryDownloadStatus
import mihon.entry.interactions.manga.download.model.DownloadState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class MangaDownloadProcessor(
    private val dependencies: MangaEntryInteractionRuntimeDependencies,
) : EntryDownloadProcessor,
    EntryDownloadSettingProvider,
    EntryBulkDownloadCandidateProcessor {
    private val downloadManager = dependencies.downloadManager
    private val ownerResolver = EntryDownloadOwnerResolver(dependencies.entryRepository)

    override val type: EntryType = EntryType.MANGA
    override val changes: Flow<Unit> = combine(
        dependencies.downloadCache.changes,
        downloadManager.queueState.map { Unit },
    ) { _, _ -> }
    override val isInitializing: Flow<Boolean> = dependencies.downloadCache.isInitializing
    override val isRunning: Flow<Boolean> = downloadManager.isDownloaderRunning
    override val queueState: Flow<List<EntryDownloadQueueGroup>> = downloadManager.queueState
        .map { downloads -> downloads.toMangaEntryDownloadQueueGroups() }
        .map { groups -> groups.map { it.requireManga() } }
    override val events = downloadManager.events

    override fun updates(): Flow<EntryDownloadStatus> {
        return merge(
            downloadManager.statusFlow().map { it.toEntryDownloadStatus() },
            downloadManager.progressFlow().map { it.toEntryDownloadStatus() },
        ).map { it.requireManga() }
    }

    override fun queueStatusUpdates(): Flow<EntryDownloadQueueItem> {
        return downloadManager.statusFlow()
            .map { download -> download.toEntryDownloadQueueItem().requireManga() }
    }

    override fun queueProgressUpdates(): Flow<EntryDownloadQueueItem> {
        return downloadManager.progressFlow()
            .map { download -> download.toEntryDownloadQueueItem().requireManga() }
    }

    override suspend fun runDownloadsUntilIdle() {
        downloadManager.runDownloadsUntilIdle()
    }

    override fun startDownloads() {
        downloadManager.startDownloads()
    }

    override fun pauseDownloads() {
        downloadManager.pauseDownloads()
    }

    override fun clearQueue() {
        downloadManager.clearQueue()
    }

    override fun invalidateCache() {
        dependencies.downloadCache.invalidateCache()
    }

    override fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        downloadManager.renameSource(oldSource, newSource)
    }

    override suspend fun renameEntry(entry: Entry, newTitle: String) {
        entry.requireManga()
        downloadManager.renameManga(entry, newTitle)
    }

    override fun reorderQueue(items: List<EntryDownloadQueueItem>) {
        items.requireManga()
        downloadManager.reorderQueue(
            items.mapNotNull { item ->
                downloadManager.queueState.value.firstOrNull { it.chapter.id == item.childId }
            },
        )
    }

    override fun reorderSeries(entryId: Long, moveToTop: Boolean) {
        val (series, others) = downloadManager.queueState.value.partition { it.entry.id == entryId }
        downloadManager.reorderQueue(if (moveToTop) series + others else others + series)
    }

    override fun cancelQueuedDownloads(items: List<EntryDownloadQueueItem>) {
        items.requireManga()
        val chapterIds = items.map { it.childId }.toSet()
        val downloads = downloadManager.queueState.value.filter { it.chapter.id in chapterIds }
        if (downloads.isNotEmpty()) {
            downloadManager.cancelQueuedDownloads(downloads)
        }
    }

    override suspend fun queue(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean) {
        entry.requireManga()
        queueByOwner(entry, chapters, autoStart)
    }

    override suspend fun download(entry: Entry, chapters: List<EntryChapter>, startNow: Boolean) {
        entry.requireManga()
        queueByOwner(entry, chapters, autoStart = false)
        if (startNow) {
            downloadManager.startDownloadsNow(chapters.map(EntryChapter::id))
        } else {
            downloadManager.startDownloads()
        }
    }

    override suspend fun resolveBulkDownloadCandidatePool(
        entry: Entry,
        candidates: List<EntryChapter>?,
    ): List<EntryChapter> {
        entry.requireManga()
        return candidates ?: dependencies.entryChapterRepository.getChaptersByEntryIdAwait(
            entry.id,
            applyScanlatorFilter = true,
        ).filterNot { isDownloaded(entry, it) }
    }

    override suspend fun delete(entry: Entry, chapters: List<EntryChapter>) {
        entry.requireManga()
        downloadManager.deleteChapters(chapters, entry, dependencies.sourceManager.getOrStub(entry.source))
    }

    override suspend fun cleanup(entry: Entry, chapters: List<EntryChapter>) {
        entry.requireManga()
        downloadManager.enqueueChaptersToDelete(chapters, entry)
    }

    override suspend fun deleteEntryDownloads(entry: Entry): Boolean {
        entry.requireManga()
        return downloadManager.deleteManga(entry, dependencies.sourceManager.getOrStub(entry.source))
    }

    override fun hasDownloads(entry: Entry): Boolean {
        entry.requireManga()
        return downloadManager.getDownloadCount(entry) > 0
    }

    override fun getDownloadCount(entry: Entry): Int {
        entry.requireManga()
        return downloadManager.getDownloadCount(entry)
    }

    override fun getTotalDownloadCount(): Int {
        return downloadManager.getDownloadCount()
    }

    override fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean): Boolean {
        entry.requireManga()
        return downloadManager.isChapterDownloaded(
            chapterName = chapter.name,
            chapterScanlator = chapter.scanlator,
            chapterUrl = chapter.url,
            mangaTitle = entry.title,
            sourceId = entry.source,
            skipCache = skipCache,
        )
    }

    override fun getStatus(
        chapterId: Long,
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        entryTitle: String,
        sourceId: Long,
    ): EntryDownloadStatus {
        return downloadManager.getQueuedDownloadOrNull(chapterId)?.toEntryDownloadStatus()
            ?: EntryDownloadStatus(
                entryType = EntryType.MANGA,
                chapterId = chapterId,
                state = if (
                    downloadManager.isChapterDownloaded(
                        chapterName = chapterName,
                        chapterScanlator = chapterScanlator,
                        chapterUrl = chapterUrl,
                        mangaTitle = entryTitle,
                        sourceId = sourceId,
                    )
                ) {
                    EntryDownloadState.DOWNLOADED
                } else {
                    EntryDownloadState.NOT_DOWNLOADED
                },
            )
    }

    override fun cancelQueuedDownload(chapterId: Long): EntryDownloadStatus? {
        val download = downloadManager.getQueuedDownloadOrNull(chapterId) ?: return null
        downloadManager.cancelQueuedDownloads(listOf(download))
        download.status = DownloadState.NOT_DOWNLOADED
        return download.toEntryDownloadStatus().requireManga()
    }

    private suspend fun queueByOwner(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean) {
        val owners = ownerResolver.resolve(entry, chapters)
        owners.forEach { owner ->
            downloadManager.downloadChapters(owner.entry, owner.children, autoStart = false)
        }
        if (autoStart && owners.isNotEmpty()) downloadManager.startDownloads()
    }
}
