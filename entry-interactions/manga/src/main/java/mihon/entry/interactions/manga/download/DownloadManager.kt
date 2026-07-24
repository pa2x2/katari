package mihon.entry.interactions.manga.download

import android.content.Context
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import logcat.LogPriority
import mihon.entry.interactions.EntryDownloadQueuePolicy
import mihon.entry.interactions.EntryDownloadWorkController
import mihon.entry.interactions.manga.download.model.DownloadState
import mihon.entry.interactions.manga.download.model.MangaDownload
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This class is used to manage chapter downloads in the application. It must be instantiated once
 * and retrieved through dependency injection. You can use this class to queue new chapters or query
 * downloaded chapters.
 */
@OptIn(DelicateCoroutinesApi::class)
internal class DownloadManager(
    private val context: Context,
    private val provider: DownloadProvider = Injekt.get(),
    private val cache: DownloadCache = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloader: Downloader = Downloader(context, provider, cache),
    private val pendingDeleter: DownloadPendingDeleter = DownloadPendingDeleter(context),
    private val workController: EntryDownloadWorkController = Injekt.get(),
) {

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
        if (queueState.value.isNotEmpty()) workController.start()
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
        return queueState.value.find { it.chapter.id == chapterId }
    }

    fun startDownloadsNow(chapterIds: Collection<Long>) {
        reorderQueue(
            EntryDownloadQueuePolicy.promote(
                queue = queueState.value,
                keys = chapterIds,
                keyOf = { it.chapter.id },
                isActive = { it.status == DownloadState.DOWNLOADING },
            ),
        )
        startDownloads()
    }

    /**
     * Reorders the download queue.
     *
     * @param downloads value to set the download queue to
     */
    fun reorderQueue(downloads: List<MangaDownload>) {
        downloader.updateQueue(downloads)
    }

    /**
     * Tells the downloader to enqueue the given list of chapters.
     *
     * @param manga the manga of the chapters.
     * @param chapters the list of chapters to enqueue.
     * @param autoStart whether to start the downloader after enqueing the chapters.
     */
    fun downloadChapters(manga: Entry, chapters: List<EntryChapter>, autoStart: Boolean = true) {
        downloader.queueChapters(manga, chapters, autoStart)
        if (autoStart && !downloader.isRunning && queueState.value.isNotEmpty()) workController.start()
    }

    /**
     * Tells the downloader to enqueue the given list of downloads at the start of the queue.
     *
     * @param downloads the list of downloads to enqueue.
     */
    fun addDownloadsToStartOfQueue(downloads: List<MangaDownload>) {
        if (downloads.isEmpty()) return
        queueState.value.toMutableList().apply {
            addAll(0, downloads)
            reorderQueue(this)
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
        val chapterDir = provider.findChapterDir(chapter.name, chapter.scanlator, chapter.url, manga.title, source)
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
            chapterDirs.forEach { it.delete() }
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
            val entryDirectory = provider.findEntryDir(manga.title, source)
            val removed = entryDirectory == null || entryDirectory.delete() || !entryDirectory.exists()
            if (!removed) return@withIOContext false
            cache.removeManga(manga)

            // Delete source directory if empty
            val sourceDir = provider.findSourceDir(source)
            if (sourceDir?.listFiles()?.isEmpty() == true) {
                sourceDir.delete()
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
        launchIO {
            val pendingChapters = pendingDeleter.getPendingChapters()
            for ((manga, chapters) in pendingChapters) {
                val source = sourceManager.get(manga.source) ?: continue
                deleteChapters(chapters, manga, source)
            }
        }
    }

    /**
     * Renames source download folder
     *
     * @param oldSource the old source.
     * @param newSource the new source.
     */
    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        val oldFolder = provider.findSourceDir(oldSource) ?: return
        val newName = provider.getSourceDirName(newSource)

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
        val oldFolder = provider.findEntryDir(manga.title, source) ?: return
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
     * Renames an already downloaded chapter
     *
     * @param source the source of the manga.
     * @param manga the manga of the chapter.
     * @param oldChapter the existing chapter with the old name.
     * @param newChapter the target chapter with the new name.
     */
    suspend fun renameChapter(source: UnifiedSource, manga: Entry, oldChapter: EntryChapter, newChapter: EntryChapter) {
        val oldNames = provider.getValidChapterDirNames(oldChapter.name, oldChapter.scanlator, oldChapter.url)
        val mangaDir = provider.getEntryDir(manga.title, source).getOrElse { e ->
            logcat(LogPriority.ERROR, e) { "Manga download folder doesn't exist. Skipping renaming after source sync" }
            return
        }

        // Assume there's only 1 version of the chapter name formats present
        val oldDownload = oldNames.asSequence()
            .mapNotNull { mangaDir.findFile(it) }
            .firstOrNull() ?: return

        var newName = provider.getChapterDirName(newChapter.name, newChapter.scanlator, newChapter.url)
        if (oldDownload.isFile && oldDownload.extension == "cbz") {
            newName += ".cbz"
        }

        if (oldDownload.name == newName) return

        if (oldDownload.renameTo(newName)) {
            cache.removeChapter(oldChapter, manga)
            cache.addChapter(newName, mangaDir, manga)
        } else {
            logcat(LogPriority.ERROR) { "Could not rename downloaded chapter: ${oldNames.joinToString()}" }
        }
    }

    fun statusFlow(): Flow<MangaDownload> = queueState
        .flatMapLatest { downloads ->
            downloads
                .map { download ->
                    download.statusFlow.drop(1).map { download }
                }
                .merge()
        }
        .onStart {
            emitAll(
                queueState.value.filter { download -> download.status == DownloadState.DOWNLOADING }.asFlow(),
            )
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
