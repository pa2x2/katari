package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.loader.scheduling.readAheadOrder
import eu.kanade.tachiyomi.ui.reader.loader.scheduling.schedulePageLoads
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.toEntryChapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mihon.entry.interactions.manga.page.MangaPageStore
import mihon.entry.interactions.manga.page.acquisition.MangaPageAcquirer
import mihon.entry.interactions.manga.page.acquisition.MangaPageAcquisitionIntent
import mihon.entry.interactions.manga.page.acquisition.PreemptedMangaPagePreload
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entry.adapter.toSEntryChapter
import kotlin.math.min

/**
 * Loader used to load chapters from an online source.
 */
internal class EntryPageLoader(
    private val chapter: ReaderChapter,
    private val source: UnifiedSource,
    private val chapterCache: MangaPageStore,
    private val pageAcquirer: MangaPageAcquirer,
    private val appScope: CoroutineScope,
) : PageLoader() {

    private val imageSource = source as EntryImageSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Guards the fields below. Callers only record which pages they want; scheduleLoads then starts and cancels
    // downloads on one coroutine, so two decisions never interleave.
    private val lock = Any()
    private var selectedPage: ReaderPage? = null
    private val retriedPages = LinkedHashSet<ReaderPage>()
    private val requests = mutableListOf<PageRequest>()

    // Null until a surface reports a reading position; until then pages load only when asked for
    private var readAheadPages: List<ReaderPage>? = null
    private val preloadedPages = LinkedHashSet<ReaderPage>()
    private val loads = HashMap<ReaderPage, PageLoad>()

    // Cancelled loads that haven't finished yet. A new load of the same page waits for them, otherwise their reset
    // to Queue could land after the new load has moved the page on.
    private val stoppingLoads = HashMap<ReaderPage, Job>()
    private val scheduleRequests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            while (true) {
                scheduleRequests.receive()
                synchronized(lock) { scheduleLoads() }
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
            if (media !is EntryMedia.ImagePages) {
                throw ReaderLoadException(
                    message = "Source ${source.name} did not return image pages",
                    canRetry = false,
                )
            }
            media.pages.map { page -> Page(page.index, page.url, page.imageUrl) }
        }
        return pages.mapIndexed { index, page ->
            // Don't trust sources and use our own indexing
            ReaderPage(index, page.url, page.imageUrl)
        }
    }

    override suspend fun loadPage(page: ReaderPage) {
        loadPage(page, PRELOAD_SIZE)
    }

    /**
     * Loads [page] for as long as the caller waits. Re-enqueues the page if its image was evicted from the cache.
     */
    override suspend fun loadPage(page: ReaderPage, preloadCount: Int) {
        withIOContext {
            val imageUrl = page.imageUrl
            if (page.status == Page.State.Ready && imageUrl != null && !chapterCache.isImageInCache(imageUrl)) {
                page.setProgressiveImageSession(null)
                page.status = Page.State.Queue
            }
        }
        // Automatically retry failed pages when subscribed to this page
        page.requeueIfFailed()

        val nextPages = pagesAfter(page, preloadCount)
        val request = updateWantedPages {
            PageRequest(page, nextPages, selected = page === selectedPage).also { requests += it }
        }
        try {
            awaitCancellation()
        } finally {
            updateWantedPages { requests.remove(request) }
        }
    }

    override fun selectPage(page: ReaderPage) {
        updateWantedPages { selectedPage = page }
    }

    override fun preloadPage(page: ReaderPage) {
        page.requeueIfFailed()
        updateWantedPages {
            preloadedPages += page
            preloadedPages += pagesAfter(page, PRELOAD_SIZE)
        }
    }

    override fun preloadPages(pages: List<ReaderPage>) {
        pages.forEach { it.requeueIfFailed() }
        updateWantedPages { preloadedPages += pages }
    }

    override fun setPreloadPages(pages: List<ReaderPage>) {
        pages.forEach { it.requeueIfFailed() }
        updateWantedPages {
            preloadedPages.clear()
            preloadedPages += pages
        }
    }

    override fun readAhead(page: ReaderPage, wholeChapter: Boolean) {
        val pages = chapter.pages ?: return
        val order = readAheadOrder(page.index, pages.size, wholeChapter).map(pages::get)
        // Each new reading position retries the pages that failed ahead of it, so a dropped connection doesn't
        // leave every page loaded ahead waiting for a manual retry
        order.forEach { it.requeueIfFailed() }
        updateWantedPages { readAheadPages = order }
    }

    override fun stopReadingAhead() {
        updateWantedPages { readAheadPages = null }
    }

    /**
     * Retries a page. This method is only called from user interaction on the viewer.
     */
    override fun retryPage(page: ReaderPage) {
        page.requeueIfFailed()
        updateWantedPages { retriedPages += page }
    }

    override fun recycle() {
        super.recycle()
        scope.cancel()
        synchronized(lock) {
            selectedPage = null
            retriedPages.clear()
            requests.clear()
            readAheadPages = null
            preloadedPages.clear()
            loads.clear()
            stoppingLoads.clear()
        }
        chapter.pages?.forEach { it.setProgressiveImageSession(null) }

        // Cache current page list progress for online chapters to allow a faster reopen. The loader's own scope is
        // cancelled by now.
        chapter.pages?.let { pages ->
            appScope.launch {
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

    private fun <R> updateWantedPages(update: () -> R): R {
        return synchronized(lock, update).also { scheduleRequests.trySend(Unit) }
    }

    /**
     * Makes the running downloads the [MAX_CONCURRENT_LOADS] most important pages still to load. The page being read
     * comes first, then retried pages, the pages surfaces wait for, the pages read ahead and the preloaded pages.
     */
    private fun scheduleLoads() {
        preloadedPages.removeAll { it !in loads && it.status != Page.State.Queue }
        retriedPages.removeAll { it !in loads && it.status != Page.State.Queue }

        val wanted = buildList {
            selectedPage?.let(::add)
            addAll(retriedPages)
            requests.forEach { add(it.page) }
            // Read ahead replaces the pages each request preloads after itself, so it alone decides how far ahead
            // pages load once there is a reading position
            readAheadPages?.let(::addAll) ?: requests.forEach { addAll(it.nextPages) }
            addAll(preloadedPages)
        }
        val changes = schedulePageLoads(
            wanted = wanted,
            loading = loads.keys.toSet(),
            missing = { it.status == Page.State.Queue },
            slots = MAX_CONCURRENT_LOADS,
        )
        changes.cancel.forEach { stopLoad(it, cause = null) }

        // Only a visible acquisition decodes progressively, so a preload of a page that became visible restarts
        if (pageAcquirer.prioritizesVisiblePages) {
            loads.entries
                .filter { (page, load) ->
                    load.intent == MangaPageAcquisitionIntent.Preload &&
                        intentOf(page) == MangaPageAcquisitionIntent.Visible
                }
                .map { it.key }
                .forEach { page ->
                    stopLoad(page, PreemptedMangaPagePreload())
                    startLoad(page)
                }
        }
        changes.start.forEach(::startLoad)
    }

    private fun intentOf(page: ReaderPage): MangaPageAcquisitionIntent {
        val visible = page === selectedPage ||
            page in retriedPages ||
            requests.any { it.page === page && it.selected }
        return if (visible) {
            MangaPageAcquisitionIntent.Visible
        } else {
            MangaPageAcquisitionIntent.Preload
        }
    }

    private fun startLoad(page: ReaderPage) {
        val previous = stoppingLoads[page]
        val intent = intentOf(page)
        val force = page in retriedPages
        val job = scope.launch {
            previous?.join()
            internalLoadPage(page, force, intent)
        }
        loads[page] = PageLoad(job, intent)
        job.invokeOnCompletion {
            synchronized(lock) {
                if (loads[page]?.job === job) loads.remove(page)
                if (stoppingLoads[page] === job) stoppingLoads.remove(page)
            }
            scheduleRequests.trySend(Unit)
        }
    }

    private fun stopLoad(page: ReaderPage, cause: CancellationException?) {
        val load = loads.remove(page) ?: return
        stoppingLoads[page] = load.job
        load.job.cancel(cause)
    }

    private fun pagesAfter(page: ReaderPage, count: Int): List<ReaderPage> {
        val pages = page.chapter.pages ?: return emptyList()
        return pages.subList(min(page.index + 1, pages.size), min(page.index + 1 + count, pages.size))
    }

    private fun ReaderPage.requeueIfFailed() {
        if (status is Page.State.Error) {
            setProgressiveImageSession(null)
            status = Page.State.Queue
        }
    }

    /**
     * Loads the page, retrieving the image URL and downloading the image if necessary.
     * Downloaded images are stored in the chapter cache.
     */
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
            if (e is CancellationException || !currentCoroutineContext().isActive) {
                // Page turns cancel downloads that are no longer wanted. Back in the queue, the page loads again
                // once it is wanted; as an error it would wait for a surface to retry it.
                page.setProgressiveImageSession(null)
                page.status = Page.State.Queue
                // Another loader's visible acquisition of the same image preempted this preload
                if (e is PreemptedMangaPagePreload) return
                throw e
            }
            page.status = Page.State.Error(e)
        }
    }
}

/**
 * A surface waiting for [page]. [selected] records whether the surface had selected the page when it asked, which
 * keeps a page loading as visible when a surface showing several pages selects each of them in turn.
 */
private class PageRequest(
    val page: ReaderPage,
    val nextPages: List<ReaderPage>,
    val selected: Boolean,
)

private class PageLoad(
    val job: Job,
    val intent: MangaPageAcquisitionIntent,
)

private fun ReaderPage.toEntryImagePage(): EntryImagePage =
    EntryImagePage(index, url, imageUrl)

private const val PRELOAD_SIZE = 4

// Each load holds a request to the source. OkHttp runs five requests per host at once by default; staying below
// that leaves room for the source's other requests, and more loads would only split the bandwidth further.
private const val MAX_CONCURRENT_LOADS = 4
