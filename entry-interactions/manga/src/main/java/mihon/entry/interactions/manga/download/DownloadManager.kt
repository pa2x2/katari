package mihon.entry.interactions.manga.download

import android.content.Context
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.download.EntryDownloadQueuePolicy
import mihon.entry.interactions.download.EntryDownloadWorkController
import mihon.entry.interactions.manga.download.model.DownloadState
import mihon.entry.interactions.manga.download.model.MangaDownload
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*
import java.util.concurrent.ConcurrentHashMap

/**
 * This class is used to manage chapter downloads in the application. It must be instantiated once
 * and retrieved through dependency injection. You can use this class to queue new chapters or query
 * downloaded chapters.
 */
internal class DownloadManager(
    private val context: Context,
    private val provider: DownloadProvider,
    private val cache: DownloadCache,
    private val sourceManager: SourceManager,
    private val downloader: Downloader,
    private val pendingDeleter: DownloadPendingDeleter,
    private val workController: EntryDownloadWorkController,
    private val scope: CoroutineScope,
) {

    init {
        scope.launch {
            downloader.awaitInitialized()
            if (queueState.value.isNotEmpty()) workController.resumeIfRequested()
        }
    }

    val isRunning: Boolean
        get() = downloader.isRunning

    val queueState
        get() = downloader.queueState
    val events
        get() = downloader.events

    val isDownloaderRunning
        get() = downloader.isRunningFlow

    /**
     * Tells the downloader to begin downloads.
     */
    fun startDownloads() {
        if (downloader.isRunning) return
        queueState.value.forEach { download ->
            if (download.status != DownloadState.DOWNLOADED) download.status = DownloadState.QUEUE
        }
        if (queueState.value.isNotEmpty()) workController.start()
    }

    suspend fun hasPendingDownloads(): Boolean {
        downloader.awaitInitialized()
        return queueState.value.any { it.status == DownloadState.QUEUE }
    }

    suspend fun runDownloadsUntilIdle() {
        downloader.awaitInitialized()
        if (!downloader.start()) return
        try {
            downloader.awaitIdle()
        } catch (error: kotlinx.coroutines.CancellationException) {
            downloader.pause()
            throw error
        }
    }

    /**
     * Tells the downloader to pause downloads.
     */
    fun pauseDownloads() {
        workController.stop()
        downloader.pause()
    }

    /**
     * Empties the download queue.
     */
    fun clearQueue() {
        workController.stop()
        downloader.clearQueue()
    }

    /**
     * Returns the download from queue if the chapter is queued for download
     * else it will return null which means that the chapter is not queued for download
     *
     * @param chapterId the chapter to check.
     */
    fun getQueuedDownloadOrNull(chapterId: Long): MangaDownload? {
        return getQueuedDownloadsByChapterId()[chapterId]
    }

    // Chapter lists look each of their chapters up in the same queue
    @Volatile
    private var queueIndex: QueueIndex? = null

    private class QueueIndex(val queue: List<MangaDownload>, val byChapterId: Map<Long, MangaDownload>)

    /** Returns the queued downloads by chapter id, built once per queue change. */
    fun getQueuedDownloadsByChapterId(): Map<Long, MangaDownload> {
        val queue = queueState.value
        queueIndex?.takeIf { it.queue === queue }?.let { return it.byChapterId }
        return queue.associateBy { it.chapter.id }.also { queueIndex = QueueIndex(queue, it) }
    }

    fun startDownloadsNow(chapterIds: Collection<Long>) {
        downloader.updateQueue { queue ->
            EntryDownloadQueuePolicy.promote(
                queue = queue,
                keys = chapterIds,
                keyOf = { it.chapter.id },
                isActive = { it.status == DownloadState.DOWNLOADING },
            )
        }
        startDownloads()
    }

    /**
     * Reorders the download queue.
     *
     * @param downloads the queue in its new order. Downloads that left the queue since are dropped, and ones
     * that joined it are kept at the end.
     */
    fun reorderQueue(downloads: List<MangaDownload>) {
        downloader.updateQueue { queue ->
            val queueById = queue.associateBy { it.chapter.id }
            val orderedIds = downloads.mapTo(HashSet()) { it.chapter.id }
            downloads.mapNotNull { queueById[it.chapter.id] } + queue.filter { it.chapter.id !in orderedIds }
        }
    }

    fun reorderSeries(entryId: Long, moveToTop: Boolean) {
        downloader.updateQueue { queue ->
            val (series, others) = queue.partition { it.entry.id == entryId }
            if (moveToTop) series + others else others + series
        }
    }

    /**
     * Tells the downloader to enqueue the given list of chapters.
     *
     * @param manga the manga of the chapters.
     * @param chapters the list of chapters to enqueue.
     * @param autoStart whether to start the downloader after enqueing the chapters.
     */
    suspend fun downloadChapters(manga: Entry, chapters: List<EntryChapter>, autoStart: Boolean = true) {
        downloader.queueChapters(manga, chapters, autoStart)
        if (autoStart && queueState.value.isNotEmpty()) {
            if (downloader.isRunning) workController.start() else startDownloads()
        }
    }

    /**
     * Tells the downloader to enqueue the given list of downloads at the start of the queue.
     *
     * @param downloads the list of downloads to enqueue.
     */
    fun addDownloadsToStartOfQueue(downloads: List<MangaDownload>) {
        if (downloads.isEmpty()) return
        downloader.updateQueue { queue ->
            // A chapter already queued keeps its download, which may be running
            val queueById = queue.associateBy { it.chapter.id }
            val chapterIds = downloads.mapTo(HashSet()) { it.chapter.id }
            downloads.map { queueById[it.chapter.id] ?: it } + queue.filter { it.chapter.id !in chapterIds }
        }
        startDownloads()
    }

    /**
     * Builds the page list of a downloaded chapter.
     *
     * @param source the source of the chapter.
     * @param manga the manga of the chapter.
     * @param chapter the downloaded chapter.
     * @return the list of pages from the chapter.
     */
    fun buildPageList(source: UnifiedSource, manga: Entry, chapter: EntryChapter): List<Page> {
        val chapterDir = provider.findChapterDir(
            chapter.name,
            chapter.scanlator,
            chapter.url,
            manga.downloadFolderTitle,
            source,
        )
        val files = chapterDir?.listFiles().orEmpty()
            .filter { it.isFile && ImageUtil.isImage(it.name) { it.openInputStream() } }

        if (files.isEmpty()) {
            throw Exception(context.stringResource(MR.strings.page_list_empty_error))
        }

        return files.sortedBy { it.name }
            .mapIndexed { i, file ->
                Page(i, uri = file.uri).apply { status = Page.State.Ready }
            }
    }

    /**
     * Returns true if the chapter is downloaded.
     *
     * @param chapterName the name of the chapter to query.
     * @param chapterScanlator scanlator of the chapter to query
     * @param mangaTitle the title of the manga to query.
     * @param sourceId the id of the source of the chapter.
     * @param skipCache whether to skip the directory cache and check in the filesystem.
     */
    fun isChapterDownloaded(
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        mangaTitle: String,
        sourceId: Long,
        skipCache: Boolean = false,
    ): Boolean {
        return cache.isChapterDownloaded(chapterName, chapterScanlator, chapterUrl, mangaTitle, sourceId, skipCache)
    }

    /** Returns the ids of the downloaded chapters among [chapters], all of [manga]. */
    fun getDownloadedChapterIds(chapters: List<EntryChapter>, manga: Entry): Set<Long> {
        return cache.getDownloadedChapterIds(chapters, manga.downloadFolderTitle, manga.source)
    }

    /**
     * Returns the amount of downloaded chapters.
     */
    fun getDownloadCount(): Int {
        return cache.getTotalDownloadCount()
    }

    /**
     * Returns the amount of downloaded chapters for a manga.
     *
     * @param manga the manga to check.
     */
    fun getDownloadCount(manga: Entry): Int {
        return cache.getDownloadCount(manga)
    }

    /** Returns the amount of downloaded chapters of each of [mangas], by entry id. */
    fun getDownloadCounts(mangas: List<Entry>): Map<Long, Int> {
        return cache.getDownloadCounts(mangas)
    }

    fun cancelQueuedDownloads(downloads: List<MangaDownload>) {
        removeFromDownloadQueue(downloads.map { it.chapter })
    }

    /**
     * Deletes the directories of a list of downloaded chapters.
     *
     * @param chapters the list of chapters to delete.
     * @param entry the manga of the chapters.
     * @param source the source of the chapters.
     */
    suspend fun deleteChapters(chapters: List<EntryChapter>, entry: Entry, source: UnifiedSource) {
        withIOContext {
            if (chapters.isEmpty()) return@withIOContext

            removeFromDownloadQueue(chapters)

            val (mangaDir, chapterDirs) = provider.findChapterDirs(chapters, entry, source)
            chapterDirs.forEach { chapterDir ->
                // A translation belongs to its download; left behind, it would also keep the entry folder.
                listOf(
                    provider.getChapterTranslationFileName(chapterDir),
                    provider.getChapterTranslationTemporaryFileName(chapterDir),
                ).forEach { mangaDir?.findFile(it)?.delete() }
                chapterDir.delete()
            }
            cache.removeChapters(chapters, entry)

            // Delete manga directory if empty
            if (mangaDir?.listFiles()?.isEmpty() == true) {
                deleteManga(entry, source, removeQueued = false)
            }
        }
    }

    /**
     * Deletes the directory of a downloaded manga.
     *
     * @param manga the manga to delete.
     * @param source the source of the manga.
     * @param removeQueued whether to also remove queued downloads.
     */
    suspend fun deleteManga(manga: Entry, source: UnifiedSource, removeQueued: Boolean = true): Boolean {
        return withIOContext {
            if (removeQueued) {
                downloader.removeFromQueue(manga)
            }
            val entryDirectory = provider.findEntryDir(manga.downloadFolderTitle, source)
            val removed = entryDirectory == null || entryDirectory.delete() || !entryDirectory.exists()
            if (!removed) return@withIOContext false
            cache.removeManga(manga)

            // Delete source directories if empty
            provider.findSourceDirs(source)
                .filter { it.listFiles()?.isEmpty() == true }
                .forEach { it.delete() }
            if (provider.findSourceDirs(source).isEmpty()) {
                cache.removeSource(source)
            }
            true
        }
    }

    private fun removeFromDownloadQueue(chapters: List<EntryChapter>) {
        val wasRunning = downloader.isRunning
        downloader.removeFromQueue(chapters)

        if (wasRunning && queueState.value.isEmpty()) {
            downloader.stop()
        }
    }

    /**
     * Adds a list of chapters to be deleted later.
     *
     * @param chapters the list of chapters to delete.
     * @param manga the manga of the chapters.
     */
    suspend fun enqueueChaptersToDelete(chapters: List<EntryChapter>, manga: Entry) {
        pendingDeleter.addChapters(chapters, manga)
    }

    /**
     * Triggers the execution of the deletion of pending chapters.
     */
    fun deletePendingChapters() {
        scope.launch {
            val pendingChapters = pendingDeleter.getPendingChapters()
            for ((manga, chapters) in pendingChapters) {
                val source = sourceManager.get(manga.source) ?: continue
                deleteChapters(chapters, manga, source)
            }
        }
    }

    /**
     * Renames source download folders. A legacy folder keeps the legacy form, so every source sharing it, renamed
     * together as the sources of one extension are, still finds it.
     *
     * @param oldSource the old source.
     * @param newSource the new source.
     */
    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        val oldNames = provider.getSourceDirNames(oldSource)
        val newNames = provider.getSourceDirNames(newSource)
        renameSourceDir(oldNames.current, newNames.current)
        if (oldNames.legacy != null && newNames.legacy != null) {
            renameSourceDir(oldNames.legacy, newNames.legacy)
        }
    }

    private fun renameSourceDir(oldName: String, newName: String) {
        val oldFolder = provider.findSourceDir(oldName) ?: return

        if (oldFolder.name == newName) return

        val capitalizationChanged = oldFolder.name.equals(newName, ignoreCase = true)
        if (capitalizationChanged) {
            val tempName = newName + Downloader.TMP_DIR_SUFFIX
            if (!oldFolder.renameTo(tempName)) {
                logcat(LogPriority.ERROR) { "Failed to rename source download folder: ${oldFolder.name}" }
                return
            }
        }

        if (!oldFolder.renameTo(newName)) {
            logcat(LogPriority.ERROR) { "Failed to rename source download folder: ${oldFolder.name}" }
        }
    }

    /**
     * Renames manga download folder
     *
     * @param manga the manga
     * @param newTitle the new manga title.
     */
    suspend fun renameManga(manga: Entry, newTitle: String) {
        val source = sourceManager.getOrStub(manga.source)
        val oldFolder = provider.findEntryDir(manga.downloadFolderTitle, source) ?: return
        val newName = provider.getEntryDirName(newTitle)

        if (oldFolder.name == newName) return

        // just to be safe, don't allow downloads for this manga while renaming it
        downloader.removeFromQueue(manga)

        val capitalizationChanged = oldFolder.name.equals(newName, ignoreCase = true)
        if (capitalizationChanged) {
            val tempName = newName + Downloader.TMP_DIR_SUFFIX
            if (!oldFolder.renameTo(tempName)) {
                logcat(LogPriority.ERROR) { "Failed to rename manga download folder: ${oldFolder.name}" }
                return
            }
        }

        if (oldFolder.renameTo(newName)) {
            cache.renameManga(manga, oldFolder, newTitle)
        } else {
            logcat(LogPriority.ERROR) { "Failed to rename manga download folder: ${oldFolder.name}" }
        }
    }

    /**
     * Emits a download when it joins the queue and whenever its status changes, including the status it had when it
     * left the queue. A download leaves the queue right after it finishes or is cancelled, which can stop its status
     * from being watched before that last change was seen, so the change is emitted on leaving instead.
     */
    fun statusFlow(): Flow<MangaDownload> = channelFlow {
        val watchers = mutableMapOf<Long, Pair<MangaDownload, Job>>()
        val lastSent = ConcurrentHashMap<Long, DownloadState>()
        queueState.collect { downloads ->
            val current = downloads.mapTo(HashSet()) { it.chapter.id }
            (watchers.keys - current).forEach { chapterId ->
                val (download, watcher) = watchers.remove(chapterId) ?: return@forEach
                watcher.cancelAndJoin()
                if (lastSent.remove(chapterId) != download.status) send(download)
            }
            downloads.filter { it.chapter.id !in watchers }.forEach { download ->
                lastSent[download.chapter.id] = download.status
                send(download)
                val watcher = launch {
                    download.statusFlow.drop(1).collect { status ->
                        lastSent[download.chapter.id] = status
                        send(download)
                    }
                }
                watchers[download.chapter.id] = download to watcher
            }
        }
    }

    fun progressFlow(): Flow<MangaDownload> = queueState
        .flatMapLatest { downloads ->
            downloads
                .map { download ->
                    download.progressFlow.drop(1).map { download }
                }
                .merge()
        }
        .onStart {
            emitAll(
                queueState.value.filter { download -> download.status == DownloadState.DOWNLOADING }
                    .asFlow(),
            )
        }
}
