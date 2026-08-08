package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.toEntryChapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.entry.interactions.manga.page.MangaPageStore
import mihon.entry.interactions.manga.page.acquisition.MangaPageAcquirer
import mihon.entry.interactions.manga.page.acquisition.MangaPageAcquisitionCoordinator
import mihon.entry.interactions.manga.page.acquisition.MangaPageAcquisitionIntent
import mihon.entry.interactions.manga.page.acquisition.PreemptedMangaPagePreload
import mihon.entry.interactions.manga.page.acquisition.StoredMangaPageAcquirer
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entry.adapter.toSEntryChapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.PriorityBlockingQueue
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.math.min

/**
 * Loader used to load chapters from an online source.
 */
internal class EntryPageLoader private constructor(
    private val chapter: ReaderChapter,
    private val source: UnifiedSource,
    private val chapterCache: MangaPageStore,
    private val pageAcquirer: MangaPageAcquirer,
) : PageLoader() {

    internal constructor(
        chapter: ReaderChapter,
        source: UnifiedSource,
    ) : this(
        chapter = chapter,
        source = source,
        chapterCache = Injekt.get(),
        pageAcquirer = Injekt.get<MangaPageAcquisitionCoordinator>(),
    )

    internal constructor(
        chapter: ReaderChapter,
        source: UnifiedSource,
        chapterCache: MangaPageStore,
    ) : this(
        chapter = chapter,
        source = source,
        chapterCache = chapterCache,
        pageAcquirer = StoredMangaPageAcquirer(chapterCache),
    )

    private val imageSource = source as EntryImageSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * A queue used to manage requests one by one while allowing priorities.
     */
    private val queue = PriorityBlockingQueue<PriorityPage>()
    private val activeLoadLock = Any()
    private val preloadPreemptionMutex = Mutex()
    private var activeLoad: ActivePageLoad? = null

    @Volatile
    private var selectedPage: ReaderPage? = null

    private val preloadSize = 4

    init {
        scope.launchIO {
            flow {
                while (true) {
                    emit(runInterruptible { queue.take() })
                }
            }
                .filter { it.page.status == Page.State.Queue }
                .collect {
                    executeQueuedLoad(it)
                }
        }
    }

    override var isLocal: Boolean = false

    /**
     * Returns the page list for a chapter. It tries to return the page list from the local cache,
     * otherwise fallbacks to network.
     */
    override suspend fun getPages(): List<ReaderPage> {
        val domainChapter = chapter.chapter.toDomainChapter()!!
        val pages = chapterCache.getOrPutPageList(domainChapter) {
            val media = source.getMedia(domainChapter.toEntryChapter().toSEntryChapter())
            check(media is EntryMedia.ImagePages) {
                "Source ${source.name} did not return image pages"
            }
            media.pages.map { page -> Page(page.index, page.url, page.imageUrl) }
        }
        return pages.mapIndexed { index, page ->
            // Don't trust sources and use our own indexing
            ReaderPage(index, page.url, page.imageUrl)
        }
    }

    /**
     * Loads a page through the queue. Handles re-enqueueing pages if they were evicted from the cache.
     */
    override suspend fun loadPage(page: ReaderPage) {
        loadPage(page, preloadSize)
    }

    override suspend fun loadPage(page: ReaderPage, preloadCount: Int) = withIOContext {
        val imageUrl = page.imageUrl

        // Check if the image has been deleted
        if (page.status == Page.State.Ready && imageUrl != null && !chapterCache.isImageInCache(imageUrl)) {
            page.setProgressiveImageSession(null)
            page.status = Page.State.Queue
        }

        // Automatically retry failed pages when subscribed to this page
        if (page.status is Page.State.Error) {
            page.setProgressiveImageSession(null)
            page.status = Page.State.Queue
        }

        val priority = if (selectedPage === page) PriorityPage.DEFAULT else PriorityPage.ADJACENT
        val queuedPages = enqueuePageAndNextPages(page, priority, preloadCount)

        suspendCancellableCoroutine<Nothing> { continuation ->
            continuation.invokeOnCancellation {
                queuedPages.forEach {
                    if (it.page.status == Page.State.Queue) {
                        queue.remove(it)
                    }
                }
            }
        }
    }

    override fun selectPage(page: ReaderPage) {
        selectedPage = page
        scope.launchIO {
            val preemptedPreload = if (pageAcquirer.prioritizesVisiblePages && page.status != Page.State.Ready) {
                preemptActivePreload()
            } else {
                null
            }
            if (page.status == Page.State.Queue) {
                queue.offer(PriorityPage(page, PriorityPage.DEFAULT))
            }
            requeuePreemptedPreload(preemptedPreload, except = page)
        }
    }

    override fun preloadPage(page: ReaderPage) {
        if (page.status is Page.State.Error) {
            page.setProgressiveImageSession(null)
            page.status = Page.State.Queue
        }
        enqueuePageAndNextPages(page, PriorityPage.ADJACENT)
    }

    override fun preloadPages(pages: List<ReaderPage>) {
        enqueuePages(pages)
    }

    override fun setPreloadPages(pages: List<ReaderPage>) {
        val requestedPages = pages.toSet()
        queue.removeAll { it.priority == PriorityPage.ADJACENT && it.page !in requestedPages }
        val alreadyQueued = queue.mapTo(mutableSetOf(), PriorityPage::page)
        enqueuePages(pages.filterNot(alreadyQueued::contains))
    }

    private fun enqueuePages(pages: List<ReaderPage>) {
        pages.forEach { page ->
            if (page.status is Page.State.Error) {
                page.setProgressiveImageSession(null)
                page.status = Page.State.Queue
            }
            if (page.status == Page.State.Queue) queue.offer(PriorityPage(page, PriorityPage.ADJACENT))
        }
    }

    /**
     * Retries a page. This method is only called from user interaction on the viewer.
     */
    override fun retryPage(page: ReaderPage) {
        scope.launchIO {
            val preemptedPreload = if (pageAcquirer.prioritizesVisiblePages) {
                preemptActivePreload()
            } else {
                null
            }
            if (page.status is Page.State.Error) {
                page.setProgressiveImageSession(null)
                page.status = Page.State.Queue
            }
            if (page.status == Page.State.Queue) {
                queue.offer(PriorityPage(page, PriorityPage.RETRY))
            }
            requeuePreemptedPreload(preemptedPreload, except = page)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun recycle() {
        super.recycle()
        scope.cancel()
        queue.clear()
        selectedPage = null
        chapter.pages?.forEach { it.setProgressiveImageSession(null) }

        // Cache current page list progress for online chapters to allow a faster reopen
        chapter.pages?.let { pages ->
            launchIO {
                try {
                    // Convert to pages without reader information
                    val pagesToSave = pages.map { Page(it.index, it.url, it.imageUrl) }
                    chapterCache.putPageListToCache(chapter.chapter.toDomainChapter()!!, pagesToSave)
                } catch (e: Throwable) {
                    if (e is CancellationException) {
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Preloads the given [amount] of pages after the [currentPage] with a lower priority.
     *
     * @return a list of [PriorityPage] that were added to the [queue]
     */
    private fun preloadNextPages(currentPage: ReaderPage, amount: Int): List<PriorityPage> {
        if (amount <= 0) return emptyList()
        val pageIndex = currentPage.index
        val pages = currentPage.chapter.pages ?: return emptyList()
        if (pageIndex == pages.lastIndex) return emptyList()

        return pages
            .subList(pageIndex + 1, min(pageIndex + 1 + amount, pages.size))
            .mapNotNull {
                if (it.status == Page.State.Queue) {
                    PriorityPage(it, PriorityPage.ADJACENT).apply { queue.offer(this) }
                } else {
                    null
                }
            }
    }

    private fun enqueuePageAndNextPages(
        page: ReaderPage,
        priority: Int,
        preloadCount: Int = preloadSize,
    ): List<PriorityPage> {
        val queuedPages = mutableListOf<PriorityPage>()
        if (page.status == Page.State.Queue) {
            queuedPages += PriorityPage(page, priority).also(queue::offer)
        }
        queuedPages += preloadNextPages(page, preloadCount)
        return queuedPages
    }

    /**
     * Loads the page, retrieving the image URL and downloading the image if necessary.
     * Downloaded images are stored in the chapter cache.
     *
     * @param page the page whose source image has to be downloaded.
     */
    private suspend fun executeQueuedLoad(priorityPage: PriorityPage) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            internalLoadPage(
                page = priorityPage.page,
                force = priorityPage.priority == PriorityPage.RETRY,
                intent = if (priorityPage.priority == PriorityPage.ADJACENT) {
                    MangaPageAcquisitionIntent.Preload
                } else {
                    MangaPageAcquisitionIntent.Visible
                },
            )
        }
        synchronized(activeLoadLock) {
            activeLoad = ActivePageLoad(priorityPage, job)
        }
        job.start()
        try {
            job.join()
        } finally {
            synchronized(activeLoadLock) {
                if (activeLoad?.job == job) activeLoad = null
            }
        }
    }

    private suspend fun internalLoadPage(
        page: ReaderPage,
        force: Boolean,
        intent: MangaPageAcquisitionIntent,
    ) {
        try {
            if (page.imageUrl.isNullOrEmpty()) {
                page.status = Page.State.LoadPage
                page.imageUrl = imageSource.getImageUrl(page.toEntryImagePage())
            }
            val imageUrl = page.imageUrl!!

            val imageFile = pageAcquirer.acquire(
                imageUrl = imageUrl,
                force = force,
                intent = intent,
                onFetch = { page.status = Page.State.DownloadImage },
                onProgressiveState = page::setProgressiveImageSession,
                fetch = { imageSource.getImage(page.toEntryImagePage(), page) },
            )
            page.stream = imageFile::inputStream
            page.status = Page.State.Ready
        } catch (e: Throwable) {
            if (e is PreemptedMangaPagePreload) {
                page.status = Page.State.Queue
                return
            }
            page.status = Page.State.Error(e)
            if (e is CancellationException) {
                throw e
            }
        }
    }

    private suspend fun preemptActivePreload(): ReaderPage? = preloadPreemptionMutex.withLock {
        val active = synchronized(activeLoadLock) {
            activeLoad?.takeIf { it.priorityPage.priority == PriorityPage.ADJACENT }
        } ?: return@withLock null
        active.job.cancel(PreemptedMangaPagePreload())
        active.job.join()
        active.priorityPage.page.takeIf { it.status == Page.State.Queue }
    }

    private fun requeuePreemptedPreload(preempted: ReaderPage?, except: ReaderPage) {
        if (preempted != null && preempted !== except) {
            queue.offer(PriorityPage(preempted, PriorityPage.ADJACENT))
        }
    }
}

private class ActivePageLoad(
    val priorityPage: PriorityPage,
    val job: Job,
)

private fun ReaderPage.toEntryImagePage(): EntryImagePage =
    EntryImagePage(index, url, imageUrl)

/**
 * Data class used to keep ordering of pages in order to maintain priority.
 */
@OptIn(ExperimentalAtomicApi::class)
private class PriorityPage(
    val page: ReaderPage,
    val priority: Int,
) : Comparable<PriorityPage> {
    companion object {
        private val idGenerator = AtomicInt(0)

        const val RETRY = 2
        const val DEFAULT = 1
        const val ADJACENT = 0
    }

    private val identifier = idGenerator.incrementAndFetch()

    override fun compareTo(other: PriorityPage): Int {
        val p = other.priority.compareTo(priority)
        return if (p != 0) p else identifier.compareTo(other.identifier)
    }
}
