package mihon.entry.interactions.manga.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.ResumableEntryImageSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.DiskUtil.NOMEDIA_FILE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.core.archive.ZipWriter
import mihon.entry.interactions.download.EntryDownloadEntryIdentity
import mihon.entry.interactions.download.EntryDownloadEvent
import mihon.entry.interactions.download.EntryDownloadMessage
import mihon.entry.interactions.download.EntryDownloadQueuePolicy
import mihon.entry.interactions.manga.download.model.DownloadState
import mihon.entry.interactions.manga.download.model.MangaDownload
import mihon.entry.interactions.runtime.EntryPageImageCache
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.Response
import okio.buffer
import okio.sink
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.i18n.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * This class is the one in charge of downloading chapters.
 *
 * Its queue contains the list of chapters to download.
 */
internal class Downloader(
    private val context: Context,
    private val provider: DownloadProvider,
    private val cache: DownloadCache,
    private val sourceManager: SourceManager = Injekt.get(),
    private val pageImageCache: EntryPageImageCache = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val xml: XML = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val scope: CoroutineScope = Injekt.get(),
) {

    /**
     * Store for persisting downloads across restarts.
     */
    private val store = DownloadStore(context)

    /**
     * Queue where active downloads are kept.
     */
    private val _queueState = MutableStateFlow<List<MangaDownload>>(emptyList())
    val queueState = _queueState.asStateFlow()
    private val _events = MutableSharedFlow<EntryDownloadEvent>(replay = 16, extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    // Held by every start and stop, which come from the main thread, the download worker and the download jobs, so
    // a download job's check of the current run can't interleave with another caller's start or stop
    private val runLock = Any()

    @Volatile
    private var downloaderJob: Job? = null

    // The last canceled run, kept so the next run can wait for it to finish
    private var canceledJob: Job? = null
    private val _isRunningFlow = MutableStateFlow(false)
    val isRunningFlow = _isRunningFlow.asStateFlow()
    private val initialized = CompletableDeferred<Unit>()

    /**
     * Whether the downloader is running.
     */
    val isRunning: Boolean
        get() = downloaderJob?.isActive ?: false

    /**
     * Whether the downloader is paused
     */
    @Volatile
    var isPaused: Boolean = false

    init {
        scope.launch {
            try {
                addAllToQueue(store.restore())
            } finally {
                initialized.complete(Unit)
            }
        }
    }

    /**
     * Starts the downloader. It doesn't do anything if it's already running or there isn't anything
     * to download.
     *
     * @return true if the downloader is started, false otherwise.
     */
    fun start(): Boolean {
        synchronized(runLock) {
            if (isRunning || queueState.value.isEmpty()) {
                return false
            }

            val pending = queueState.value.filter { it.status == DownloadState.QUEUE }
            if (pending.isEmpty()) return false

            isPaused = false

            _isRunningFlow.value = true
            launchDownloaderJob()

            return pending.isNotEmpty()
        }
    }

    /**
     * Stops the downloader.
     */
    fun stop(reason: EntryDownloadMessage? = null) {
        synchronized(runLock) {
            cancelDownloaderJob()
            queueState.value
                .filter { it.status == DownloadState.DOWNLOADING }
                .forEach { it.status = DownloadState.ERROR }

            if (reason != null) {
                reportWarning(reason)
                return
            }

            isPaused = false
        }
    }

    // A canceled run's download jobs can still finish or fail after a newer run started, and must not stop that run
    private fun stopIfCurrent(owner: Job) {
        synchronized(runLock) {
            if (downloaderJob === owner && owner.isActive) stop()
        }
    }

    /**
     * Pauses the downloader
     */
    fun pause() {
        synchronized(runLock) {
            cancelDownloaderJob()
            queueState.value
                .filter { it.status == DownloadState.DOWNLOADING }
                .forEach { it.status = DownloadState.QUEUE }
            isPaused = true
        }
    }

    /**
     * Removes everything from the queue.
     */
    fun clearQueue() {
        synchronized(runLock) {
            cancelDownloaderJob()

            internalClearQueue()
        }
    }

    /**
     * Prepares the subscriptions to start downloading.
     */
    private fun launchDownloaderJob() {
        if (isRunning) return

        val previousJob = canceledJob
        // Started only once it is the current run, so its download jobs can tell they still own it
        val job = scope.launch(start = CoroutineStart.LAZY) {
            // Page writes block, so a canceled run can still be writing the pages this one would start on. Not
            // cancelable, so a run canceled while waiting still finishes after the one before it.
            withContext(NonCancellable) { previousJob?.join() }
            val owner = coroutineContext.job
            val activeDownloadsFlow = combine(
                queueState,
                downloadPreferences.parallelSourceLimit.changes(),
            ) { a, b -> a to b }.transformLatest { (queue, parallelCount) ->
                while (true) {
                    val activeDownloads = queue.asSequence()
                        // Ignore completed downloads, leave them in the queue
                        .filter { it.status.value <= DownloadState.DOWNLOADING.value }
                        .groupBy { it.source }
                        .toList()
                        .take(parallelCount)
                        .map { (_, downloads) -> downloads.first() }
                    emit(activeDownloads)

                    if (activeDownloads.isEmpty()) break
                    // Suspend until a download enters the ERROR state
                    val activeDownloadsErroredFlow =
                        combine(activeDownloads.map(MangaDownload::statusFlow)) { states ->
                            states.contains(DownloadState.ERROR)
                        }.filter { it }
                    activeDownloadsErroredFlow.first()
                }
            }
                .distinctUntilChanged()

            // Use supervisorScope to cancel child jobs when the downloader job is cancelled
            supervisorScope {
                val sourceJobs = mutableMapOf<UnifiedSource, Job>()
                val downloadJobs = ConcurrentHashMap<MangaDownload, Job>()

                // A source runs several of its chapters at once, so a removed one is stopped by itself
                launch {
                    queueState.collect { queue ->
                        downloadJobs.keys.filter { it !in queue }.forEach { downloadJobs.remove(it)?.cancel() }
                    }
                }

                activeDownloadsFlow.collectLatest { activeDownloads ->
                    val activeSources = activeDownloads.map { it.source }
                    val sourceJobsToStop = sourceJobs.filter { it.key !in activeSources }
                    sourceJobsToStop.forEach { (source, job) ->
                        job.cancel()
                        sourceJobs.remove(source)
                    }

                    val sourcesToStart = activeSources.filter { it !in sourceJobs }
                    sourcesToStart.forEach { source ->
                        sourceJobs[source] = launchSourceJob(source, downloadJobs, owner)
                    }
                }
            }
        }
        downloaderJob = job
        job.invokeOnCompletion {
            synchronized(runLock) {
                if (downloaderJob === job) {
                    downloaderJob = null
                    _isRunningFlow.value = false
                }
            }
        }
        job.start()
    }

    /**
     * Downloads the queued chapters of [source] in queue order. They share the source's page slots, and the next
     * chapter starts once every page of the current one has a slot, so slots don't sit idle while a chapter's last
     * pages finish and it's archived.
     */
    private fun CoroutineScope.launchSourceJob(
        source: UnifiedSource,
        downloadJobs: MutableMap<MangaDownload, Job>,
        owner: Job,
    ) = launchIO {
        val slots = Semaphore(downloadPreferences.parallelPageLimit.get())
        while (true) {
            // A canceled job can mark a chapter as downloading after it's been reset, so that counts as waiting too
            val download = queueState
                .map { queue ->
                    queue.firstOrNull {
                        it.source == source &&
                            (it.status == DownloadState.QUEUE || it.status == DownloadState.DOWNLOADING) &&
                            it !in downloadJobs
                    }
                }
                .filterNotNull()
                .first()
            val pagesStarted = CompletableDeferred<Unit>()
            val job = launchDownloadJob(download, owner, slots, pagesStarted)
            downloadJobs[download] = job
            job.invokeOnCompletion { downloadJobs.remove(download, job) }
            // Removed from the queue before the job was registered, so the removal check above missed it
            if (download !in queueState.value) downloadJobs.remove(download)?.cancel()
            pagesStarted.await()
        }
    }

    private fun CoroutineScope.launchDownloadJob(
        download: MangaDownload,
        owner: Job,
        slots: Semaphore,
        pagesStarted: CompletableDeferred<Unit>,
    ) = launchIO {
        try {
            downloadChapter(download, slots, pagesStarted)

            // Remove successful download from queue
            if (download.status == DownloadState.DOWNLOADED) {
                removeFromQueue(download)
            }
            if (areAllDownloadsFinished()) {
                stopIfCurrent(owner)
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            logcat(LogPriority.ERROR, e)
            reportError()
            stopIfCurrent(owner)
        } finally {
            // Also when the chapter fails before its pages, so the source moves on to the next one
            pagesStarted.complete(Unit)
        }
    }

    /**
     * Destroys the downloader subscriptions.
     */
    private fun cancelDownloaderJob() {
        downloaderJob?.let { job ->
            job.cancel()
            canceledJob = job
        }
        downloaderJob = null
        _isRunningFlow.value = false
    }

    suspend fun awaitIdle() {
        downloaderJob?.join()
    }

    suspend fun awaitInitialized() {
        initialized.await()
    }

    /**
     * Creates a download object for every chapter and adds them to the downloads queue.
     *
     * @param entry the manga of the chapters to download.
     * @param chapters the list of chapters to download.
     * @param autoStart whether to start the downloader after enqueing the chapters.
     */
    suspend fun queueChapters(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean) {
        if (chapters.isEmpty()) return

        val source = sourceManager.get(entry.source) ?: return
        val wasEmpty = queueState.value.isEmpty()
        val downloadedChapterIds = provider.findDownloadedChapterIds(chapters, entry, source)
        val queuedChapterIds = queueState.value.mapTo(HashSet()) { it.chapter.id }
        val chaptersToQueue = chapters.asSequence()
            // Filter out those already downloaded.
            .filter { it.id !in downloadedChapterIds }
            // Add chapters to queue from the start.
            .sortedByDescending { it.sourceOrder }
            // Filter out those already enqueued.
            .filter { it.id !in queuedChapterIds }
            // Create a download for each one.
            .map { MangaDownload(source, entry, it) }
            .toList()

        if (chaptersToQueue.isNotEmpty()) {
            addAllToQueue(chaptersToQueue)

            // Start downloader if needed
            if (autoStart && wasEmpty) {
                val queuedDownloads = queueState.value.count { it.source !is UnmeteredSource }
                val maxDownloadsFromSource = queueState.value
                    .groupBy { it.source }
                    .filterKeys { it !is UnmeteredSource }
                    .maxOfOrNull { it.value.size }
                    ?: 0
                if (
                    queuedDownloads > DOWNLOADS_QUEUED_WARNING_THRESHOLD ||
                    maxDownloadsFromSource > CHAPTERS_PER_SOURCE_QUEUE_WARNING_THRESHOLD
                ) {
                    reportWarning(
                        message = EntryDownloadMessage.Resource(
                            resource = MR.strings.download_queue_size_warning,
                            args = listOf(context.stringResource(MR.strings.app_name)),
                        ),
                        timeoutMillis = WARNING_NOTIF_TIMEOUT_MS,
                        helpUrl = HELP_WARNING_URL,
                    )
                }
            }
        }
    }

    /**
     * Downloads a chapter.
     *
     * @param download the chapter to be downloaded.
     * @param slots the page download slots of the chapter's source.
     * @param pagesStarted completed once every page has a slot.
     */
    private suspend fun downloadChapter(
        download: MangaDownload,
        slots: Semaphore,
        pagesStarted: CompletableDeferred<Unit>,
    ) {
        val mangaDir = provider.getEntryDir(download.entry.title, download.source).getOrElse {
            reportError(EntryDownloadMessage.Resource(MR.strings.download_notifier_storage_error), download)
            download.status = DownloadState.ERROR
            return
        }

        val availSpace = DiskUtil.getAvailableStorageSpace(mangaDir)
        if (availSpace != -1L && availSpace < MIN_DISK_SPACE) {
            reportError(
                EntryDownloadMessage.Resource(MR.strings.download_notifier_insufficient_storage),
                download,
            )
            download.status = DownloadState.ERROR
            return
        }

        val chapterDirname = provider.getChapterDirName(
            download.chapter.name,
            download.chapter.scanlator,
            download.chapter.url,
        )

        // Being stopped after the download is published but before it's indexed leaves a finished download on disk
        // that is still queued, and downloading it again would replace it for nothing or, when the archive setting
        // changed meanwhile, save a second copy next to it
        val isPublished = listOf("$chapterDirname.cbz", chapterDirname).any { name ->
            mangaDir.findFile(name)?.isValidMangaChapterArtifact() == true
        }
        if (isPublished) {
            mangaDir.findFile(chapterDirname + TMP_DIR_SUFFIX)?.delete()
            cache.addChapter(chapterDirname, mangaDir, download.entry)
            download.status = DownloadState.DOWNLOADED
            return
        }

        val tmpDir = mangaDir.createDirectory(chapterDirname + TMP_DIR_SUFFIX)!!

        try {
            // If the page list already exists, start from the file
            val pageList = download.pages ?: run {
                // Otherwise, pull page list from network and add them to download object
                val media = download.source.getMedia(download.chapter.toSEntryChapter())
                if (media is EntryMedia.ImagePages && media.pages.isEmpty()) {
                    throw Exception(context.stringResource(MR.strings.page_list_empty_error))
                }
                val chapter = media as? EntryMedia.ImagePages
                    ?: error("Source ${download.source.name} did not return image pages")
                // Don't trust index from source
                val reIndexedPages = chapter.pages.mapIndexed { index, page ->
                    Page(
                        index,
                        page.url,
                        page.imageUrl,
                    )
                }
                download.pages = reIndexedPages
                reIndexedPages
            }

            download.status = DownloadState.DOWNLOADING

            // Start downloading images, consider we may have downloaded images already
            coroutineScope {
                pageList.forEach { page ->
                    slots.acquire()
                    // Released on completion, since a page canceled before it starts never runs its body
                    launch(Dispatchers.IO) {
                        getOrDownloadImage(page, download, tmpDir)
                    }.invokeOnCompletion { slots.release() }
                }
                pagesStarted.complete(Unit)
            }

            // Do after download completes

            if (!isDownloadSuccessful(download, tmpDir)) {
                download.status = DownloadState.ERROR
                return
            }

            createComicInfoFile(
                tmpDir,
                download.entry,
                download.chapter,
                download.source,
            )

            if (downloadPreferences.saveChaptersAsCBZ.get()) {
                archiveChapter(mangaDir, chapterDirname, tmpDir)
            } else {
                DiskUtil.createNoMediaFile(tmpDir, context)
                publishChapterDirectory(mangaDir, chapterDirname, tmpDir, download)
            }
            cache.addChapter(chapterDirname, mangaDir, download.entry)

            download.status = DownloadState.DOWNLOADED
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            // If the page list threw, it will resume here
            logcat(LogPriority.ERROR, error)
            reportError(download = download)
            download.status = DownloadState.ERROR
        }
    }

    /**
     * Gets the image from the filesystem if it exists or downloads it otherwise.
     *
     * @param page the page to download.
     * @param download the download of the page.
     * @param tmpDir the temporary directory of the download.
     */
    private suspend fun getOrDownloadImage(page: Page, download: MangaDownload, tmpDir: UniFile) {
        if (page.imageUrl == null) {
            page.status = Page.State.LoadPage
            page.imageUrl = download.source.requireEntryImageSource().getImageUrl(page.toEntryImagePage())
        }

        val digitCount = (download.pages?.size ?: 0).toString().length.coerceAtLeast(3)
        val filename = "%0${digitCount}d".format(Locale.ENGLISH, page.number)

        // Try to find the image file
        val imageFile = tmpDir.listFiles()?.firstOrNull {
            isDownloadedPageImage(it.name ?: return@firstOrNull false, filename)
        }

        try {
            // If the image is already downloaded, do nothing. Otherwise download from network
            val file = when {
                imageFile != null -> imageFile
                pageImageCache.isImageInCache(
                    page.imageUrl!!,
                ) -> copyImageFromCache(pageImageCache.getImageFile(page.imageUrl!!), tmpDir, filename)

                else -> downloadImage(page, download.source.requireEntryImageSource(), tmpDir, filename)
            }

            // When the page is ready, set page path, progress (just in case) and status
            splitTallImageIfNeeded(page, tmpDir)

            page.uri = file.uri
            page.progress = 100
            page.status = Page.State.Ready
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            // Mark this page as error and allow to download the remaining
            page.progress = 0
            page.status = Page.State.Error(e)
            reportError(download = download)
        }
    }

    private fun reportError(
        message: EntryDownloadMessage = EntryDownloadMessage.Resource(MR.strings.download_notifier_unknown_error),
        download: MangaDownload? = null,
    ) {
        download?.failure = message
        _events.tryEmit(
            EntryDownloadEvent.Error(
                entryType = EntryType.MANGA,
                entryIdentity = download?.entry?.let(EntryDownloadEntryIdentity::from),
                title = download?.entry?.title,
                subtitle = download?.chapter?.name,
                message = message,
            ),
        )
    }

    private fun reportWarning(
        message: EntryDownloadMessage,
        timeoutMillis: Long? = null,
        helpUrl: String? = null,
    ) {
        _events.tryEmit(EntryDownloadEvent.Warning(message, timeoutMillis, helpUrl))
    }

    /**
     * Downloads the image from network to a file in tmpDir.
     *
     * @param page the page to download.
     * @param source the source of the page.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the image.
     */
    private suspend fun downloadImage(
        page: Page,
        source: EntryImageSource,
        tmpDir: UniFile,
        filename: String,
    ): UniFile {
        page.status = Page.State.DownloadImage
        page.progress = 0
        return flow {
            val file = tmpDir.findFile("$filename.tmp")
                ?: tmpDir.createFile("$filename.tmp")!!

            try {
                source.getImageForDownload(page.toEntryImagePage(), page, file).let { download ->
                    download.response.use { response ->
                        file.openOutputStream(download.appendToExistingFile).use { output ->
                            output.sink().buffer().use { sink ->
                                response.body.source().use { source ->
                                    sink.writeAll(source)
                                    sink.flush()
                                }
                            }
                        }
                        val extension = getImageExtension(response, file)
                        file.renameTo("$filename.$extension")
                    }
                }
            } catch (e: HttpException) {
                if (e.code == 416) {
                    file.delete()
                }
                throw e
            }
            emit(file)
        }
            // Retry 3 times, waiting 2, 4 and 8 seconds between attempts.
            .retryWhen { _, attempt ->
                if (attempt < 3) {
                    delay((2L shl attempt.toInt()).seconds)
                    true
                } else {
                    false
                }
            }
            .first()
    }

    /**
     * Copies the image from cache to file in tmpDir.
     *
     * @param cacheFile the file from cache.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the image.
     */
    private fun copyImageFromCache(cacheFile: File, tmpDir: UniFile, filename: String): UniFile {
        // Delete temp file if it exists
        tmpDir.findFile("$filename.tmp")?.delete()
        val tmpFile = tmpDir.createFile("$filename.tmp")!!
        cacheFile.inputStream().use { input ->
            tmpFile.openOutputStream().use { output ->
                input.copyTo(output)
            }
        }
        val extension = ImageUtil.findImageType(cacheFile.inputStream()) ?: return tmpFile
        tmpFile.renameTo("$filename.${extension.extension}")
        cacheFile.delete()
        return tmpFile
    }

    /**
     * Returns the extension of the downloaded image from the network response, or if it's null,
     * analyze the file. If everything fails, assume it's a jpg.
     *
     * @param response the network response of the image.
     * @param file the file where the image is already downloaded.
     */
    private fun getImageExtension(response: Response, file: UniFile): String {
        val mime = response.body.contentType()?.run { if (type == "image") "image/$subtype" else null }
        return ImageUtil.getExtensionFromMimeType(mime) { file.openInputStream() }
    }

    private fun UnifiedSource.requireEntryImageSource(): EntryImageSource =
        this as? EntryImageSource
            ?: error("Source $name does not support direct image downloads")

    private fun Page.toEntryImagePage(): EntryImagePage =
        EntryImagePage(index, url, imageUrl)

    private fun splitTallImageIfNeeded(page: Page, tmpDir: UniFile) {
        if (!downloadPreferences.splitTallImages.get()) return

        try {
            val filenamePrefix = "%03d".format(Locale.ENGLISH, page.number)
            val imageFile = tmpDir.listFiles()?.firstOrNull { it.name.orEmpty().startsWith(filenamePrefix) }
                ?: error(context.stringResource(MR.strings.download_notifier_split_page_not_found, page.number))

            // If the original page was previously split, then skip
            if (imageFile.name.orEmpty().startsWith("${filenamePrefix}__")) return

            ImageUtil.splitTallImage(tmpDir, imageFile, filenamePrefix)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to split downloaded image" }
        }
    }

    /**
     * Checks if the download was successful.
     *
     * @param download the download to check.
     * @param tmpDir the directory where the download is currently stored.
     */
    private fun isDownloadSuccessful(
        download: MangaDownload,
        tmpDir: UniFile,
    ): Boolean {
        // Page list hasn't been initialized
        val downloadPageCount = download.pages?.size ?: return false

        // Ensure that all pages have been downloaded
        if (download.downloadedImages != downloadPageCount) {
            return false
        }

        // Ensure that the chapter folder has all the pages
        val downloadedImagesCount = tmpDir.listFiles().orEmpty().count {
            val fileName = it.name.orEmpty()
            when {
                fileName in listOf(COMIC_INFO_FILE, NOMEDIA_FILE) -> false
                fileName.endsWith(".tmp") -> false
                // Only count the first split page and not the others
                fileName.contains("__") && !fileName.substringAfter("__").startsWith("001.") -> false
                else -> true
            }
        }
        return downloadedImagesCount == downloadPageCount
    }

    /**
     * Checks if the file name matches a downloaded page image.
     *
     * @param fileName Name of the file to check
     * @param pagePrefix Expected page prefix (e.g., "001")
     */
    private fun isDownloadedPageImage(fileName: String, pagePrefix: String): Boolean =
        !fileName.endsWith(".tmp") && (
            fileName.startsWith("$pagePrefix.") ||
                fileName.startsWith("${pagePrefix}__001.")
            )

    /**
     * Archive the chapter pages as a CBZ.
     */
    private fun archiveChapter(
        mangaDir: UniFile,
        dirname: String,
        tmpDir: UniFile,
    ) {
        val finalName = "$dirname.cbz"
        val stagingName = finalName + TMP_DIR_SUFFIX
        val backupName = finalName + PUBLISH_BACKUP_SUFFIX
        recoverPublicationBackup(mangaDir, finalName)
        mangaDir.findFile(stagingName)?.delete()
        val zip = mangaDir.createFile(stagingName) ?: error("Failed to create temporary chapter archive")
        ZipWriter(context, zip).use { writer ->
            tmpDir.listFiles()?.forEach { file ->
                writer.write(file)
            }
        }
        check(zip.length() != 0L) { "Temporary chapter archive is empty" }
        val existing = mangaDir.findFile(finalName)
        if (existing != null && !existing.renameTo(backupName)) {
            error("Failed to preserve existing chapter archive")
        }
        if (!zip.renameTo(finalName)) {
            mangaDir.findFile(backupName)?.renameTo(finalName)
            error("Failed to publish chapter archive")
        }
        val published = mangaDir.findFile(finalName)
        if (published == null || !published.isFile || published.length() == 0L) {
            published?.delete()
            mangaDir.findFile(backupName)?.renameTo(finalName)
            error("Published chapter archive failed validation")
        }
        mangaDir.findFile(backupName)?.delete()
        tmpDir.delete()
    }

    private fun publishChapterDirectory(
        mangaDir: UniFile,
        dirname: String,
        tmpDir: UniFile,
        download: MangaDownload,
    ) {
        check(isDownloadSuccessful(download, tmpDir)) { "Temporary chapter directory failed validation" }
        val backupName = dirname + PUBLISH_BACKUP_SUFFIX
        recoverPublicationBackup(mangaDir, dirname)
        val existing = mangaDir.findFile(dirname)
        if (existing != null && !existing.renameTo(backupName)) {
            error("Failed to preserve existing chapter directory")
        }
        if (!tmpDir.renameTo(dirname)) {
            mangaDir.findFile(backupName)?.renameTo(dirname)
            error("Failed to publish chapter directory")
        }
        val published = mangaDir.findFile(dirname)
        if (published == null || !published.isDirectory || !isDownloadSuccessful(download, published)) {
            published?.delete()
            mangaDir.findFile(backupName)?.renameTo(dirname)
            error("Published chapter directory failed validation")
        }
        mangaDir.findFile(backupName)?.delete()
    }

    private fun recoverPublicationBackup(mangaDir: UniFile, finalName: String) {
        val backup = mangaDir.findFile(finalName + PUBLISH_BACKUP_SUFFIX) ?: return
        val published = mangaDir.findFile(finalName)
        if (published?.isValidMangaChapterArtifact() == true) {
            backup.delete()
            return
        }
        published?.delete()
        backup.renameTo(finalName)
    }

    /**
     * Creates a ComicInfo.xml file inside the given directory.
     */
    private suspend fun createComicInfoFile(
        dir: UniFile,
        entry: Entry,
        chapter: EntryChapter,
        source: UnifiedSource,
    ) {
        val categories = getCategories.await(entry.id).map { it.name.trim() }.takeUnless { it.isEmpty() }
        val urls = getTracks.await(entry.id)
            .mapNotNull { track ->
                track.remoteUrl.takeUnless { url -> url.isBlank() }?.trim()
            }
            .plus(chapter.url.trim())
            .distinct()

        val comicInfo = createComicInfo(
            entry,
            chapter,
            urls,
            categories,
            source.name,
        )

        // Remove the old file
        dir.findFile(COMIC_INFO_FILE)?.delete()
        dir.createFile(COMIC_INFO_FILE)!!.openOutputStream().use {
            val comicInfoString = xml.encodeToString(ComicInfo.serializer(), comicInfo)
            it.write(comicInfoString.toByteArray())
        }
    }

    /**
     * Returns true if all the queued downloads are in DOWNLOADED or ERROR state.
     */
    private fun areAllDownloadsFinished(): Boolean {
        return queueState.value.none { it.status.value <= DownloadState.DOWNLOADING.value }
    }

    private fun addAllToQueue(downloads: List<MangaDownload>) {
        _queueState.update { queue ->
            // Callers filter against an earlier queue, so a chapter may have been queued in the meantime
            val queuedIds = queue.mapTo(HashSet()) { it.chapter.id }
            val toAdd = downloads.filter { it.chapter.id !in queuedIds }.distinctBy { it.chapter.id }
            toAdd.forEach { download ->
                download.status = DownloadState.QUEUE
            }
            store.addAll(toAdd)
            queue + toAdd
        }
    }

    private fun removeFromQueue(download: MangaDownload) {
        _queueState.update {
            store.remove(download)
            if (download.status == DownloadState.DOWNLOADING || download.status == DownloadState.QUEUE) {
                download.status = DownloadState.NOT_DOWNLOADED
            }
            it - download
        }
    }

    private inline fun removeFromQueueIf(predicate: (MangaDownload) -> Boolean) {
        _queueState.update { queue ->
            val downloads = queue.filter { predicate(it) }
            store.removeAll(downloads)
            downloads.forEach { download ->
                if (download.status == DownloadState.DOWNLOADING || download.status == DownloadState.QUEUE) {
                    download.status = DownloadState.NOT_DOWNLOADED
                }
            }
            queue - downloads
        }
    }

    fun removeFromQueue(chapters: List<EntryChapter>) {
        val chapterIds = chapters.map { it.id }
        removeFromQueueIf { it.chapter.id in chapterIds }
    }

    fun removeFromQueue(manga: Entry) {
        removeFromQueueIf { it.entry.id == manga.id }
    }

    private fun internalClearQueue() {
        _queueState.update {
            it.forEach { download ->
                if (download.status == DownloadState.DOWNLOADING || download.status == DownloadState.QUEUE) {
                    download.status = DownloadState.NOT_DOWNLOADED
                }
            }
            store.clear()
            emptyList()
        }
    }

    /**
     * Replaces the queue with [transform] applied to the current queue, so downloads queued or removed meanwhile
     * aren't lost to a stale copy of it. Active downloads stay ahead of the result and can't be removed by it.
     */
    fun updateQueue(transform: (List<MangaDownload>) -> List<MangaDownload>) {
        _queueState.update { queue ->
            val downloads = EntryDownloadQueuePolicy.reorderPending(
                queue = queue,
                requested = transform(queue).distinctBy { it.chapter.id },
                keyOf = { it.chapter.id },
                isActive = { it.status == DownloadState.DOWNLOADING },
            )
            val keptIds = downloads.mapTo(HashSet()) { it.chapter.id }
            queue.forEach { download ->
                if (download.chapter.id !in keptIds && download.status == DownloadState.QUEUE) {
                    download.status = DownloadState.NOT_DOWNLOADED
                }
            }
            val queuedIds = queue.mapTo(HashSet()) { it.chapter.id }
            downloads.forEach { download ->
                if (download.chapter.id !in queuedIds) download.status = DownloadState.QUEUE
            }
            store.clear()
            store.addAll(downloads)
            downloads
        }
        if (queueState.value.isEmpty()) stop()
    }

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"
        const val PUBLISH_BACKUP_SUFFIX = ".bak_tmp"
        const val WARNING_NOTIF_TIMEOUT_MS = 30_000L
        const val CHAPTERS_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 15
        private const val HELP_WARNING_URL =
            "https://mihon.app/docs/faq/library#why-am-i-warned-about-large-bulk-updates-and-downloads"
        private const val DOWNLOADS_QUEUED_WARNING_THRESHOLD = 30
    }
}

internal class ImageDownloadResponse(
    val response: Response,
    val appendToExistingFile: Boolean,
)

internal suspend fun EntryImageSource.getImageForDownload(
    page: EntryImagePage,
    progress: ProgressListener,
    partialFile: UniFile,
): ImageDownloadResponse {
    val resumableSource = this as? ResumableEntryImageSource
    val response = if (resumableSource != null) {
        resumableSource.getImage(page, progress, partialFile.length())
    } else {
        getImage(page, progress)
    }
    return ImageDownloadResponse(
        response = response,
        appendToExistingFile = resumableSource != null && response.code == 206,
    )
}

// Arbitrary minimum required space to start a download: 200 MB
private const val MIN_DISK_SPACE = 200L * 1024 * 1024
