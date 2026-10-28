package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingSource
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedAnchor
import eu.kanade.domain.source.model.SourceFeedGap
import eu.kanade.domain.source.model.SourceFeedNewItems
import eu.kanade.domain.source.model.SourceFeedTimeline
import eu.kanade.domain.source.model.snapshot
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.requireValidFilters
import eu.kanade.tachiyomi.ui.base.observation.ObservedItemStore
import eu.kanade.tachiyomi.ui.browse.feed.timeline.TimelineUpdate
import eu.kanade.tachiyomi.ui.browse.feed.timeline.retainingItems
import eu.kanade.tachiyomi.ui.browse.feed.timeline.withGapPage
import eu.kanade.tachiyomi.ui.browse.feed.timeline.withNewItemsViewed
import eu.kanade.tachiyomi.ui.browse.feed.timeline.withRefreshPage
import eu.kanade.tachiyomi.ui.browse.feed.timeline.withoutGap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex

class FeedViewModel<T : Any>(
    private val feedId: String,
    private val initialFilterSnapshot: List<eu.kanade.domain.source.model.FilterStateNode>,
    private val chronological: Boolean = true,
    private val browseFeedService: BrowseFeedService,
    private val hideInLibraryItems: Boolean,
    private val source: FeedItemSource<T>,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    val state: StateFlow<FeedViewModel.State>
        field = MutableStateFlow<FeedViewModel.State>(initialState(browseFeedService, feedId))

    private var currentSavedAnchor = browseFeedService.anchorSnapshot(feedId).resolvedAnchor()
    private var persistedAnchor = currentSavedAnchor
    private var resolvedFilters: EntryFilterList? = null

    // Refreshes, appends and gap loads page the same source and edit the same timeline, so only one runs at a time.
    private val loadLock = Mutex()
    private val persistLock = Any()

    // The last timeline this model saved, so the saved-timeline flow only applies changes made elsewhere.
    @Volatile
    private var lastPersistedTimeline: SourceFeedTimeline? = null

    private val itemStore = ObservedItemStore(
        scope = viewModelScope + workerDispatcher,
        keyOf = source::itemRef,
        supersedes = source::supersedes,
        observeItems = source::observeItems,
    )

    init {
        viewModelScope.launch(workerDispatcher) {
            browseFeedService.timeline(feedId).collectLatest { timeline ->
                if (timeline == lastPersistedTimeline) return@collectLatest
                state.update {
                    it.withTimeline(timeline).copy(hasLoaded = it.hasLoaded || timeline.resolvedItems().isNotEmpty())
                }
            }
        }

        launchLoad {
            if (hideInLibraryItems) {
                normalizeTimeline()
            }

            if (state.value.itemRefs.isEmpty()) {
                refreshInternal()
            }
        }
    }

    fun refresh() {
        launchLoad { refreshInternal() }
    }

    fun loadMore() {
        if (state.value.nextPageKey == null) return
        launchLoad { appendInternal() }
    }

    fun loadGap(gap: SourceFeedGap) {
        launchLoad { loadGapInternal(gap, pagesLoaded = 0, pagingSource = null, addsAboveReader = false) }
    }

    fun saveAnchor(itemRef: FeedItemRef?, scrollOffset: Int) {
        val anchor = SourceFeedAnchor.fromItem(itemRef, scrollOffset)

        if (currentSavedAnchor == anchor) return

        currentSavedAnchor = anchor

        if (persistedAnchor != anchor) {
            persistedAnchor = anchor
            browseFeedService.saveAnchor(feedId = feedId, anchor = anchor)
        }
    }

    fun savedAnchorSnapshot(): SourceFeedAnchor {
        return currentSavedAnchor
    }

    fun consumeNewItemsIndicator() {
        state.update {
            if (it.newItemsAvailableCount == 0) it else it.copy(newItemsAvailableCount = 0)
        }
    }

    /** The reader has scrolled up to the newest item; the next refresh that brings items starts a new batch. */
    fun markNewItemsViewed() {
        if (state.value.newItems?.viewed != false) return
        editTimeline({ TimelineUpdate(it.withNewItemsViewed(), addedCount = 0, gap = null) })
    }

    fun itemState(ref: FeedItemRef): StateFlow<T?> {
        return itemStore.stateOf(ref)
    }

    private fun launchLoad(block: suspend () -> Unit) {
        if (!loadLock.tryLock()) return
        viewModelScope.launch(workerDispatcher) {
            try {
                block()
            } finally {
                loadLock.unlock()
            }
        }
    }

    private suspend fun refreshInternal() {
        if (!chronological) {
            refreshCurrentPageInternal()
            return
        }

        state.update { it.copy(isRefreshing = true, error = null) }

        val (pagingSource, page) = try {
            val pagingSource = newPagingSource()
            pagingSource to loadPage(pagingSource, null)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            state.update { it.copy(isRefreshing = false, hasLoaded = true, error = e) }
            return
        }

        val pageRefs = acceptVisibleItems(page.data)
        val result = editTimeline({ it.withRefreshPage(pageRefs, page.nextKey) }) { refreshed, added ->
            refreshed.copy(
                isRefreshing = false,
                hasLoaded = true,
                newItemsAvailableCount = refreshed.newItemsAvailableCount + added.addedCount,
            )
        }

        result.gap?.let { loadGapInternal(it, pagesLoaded = 1, pagingSource = pagingSource, addsAboveReader = true) }
    }

    /**
     * Loads [gap] page by page until it closes or [CATCH_UP_PAGE_LIMIT] pages, counting [pagesLoaded] already loaded
     * for it, have been read. What is left stays a gap the reader can load later. A refresh catching up
     * [addsAboveReader]: its items count towards the new-items chip, unlike those of a gap the reader asked to load.
     */
    private suspend fun loadGapInternal(
        gap: SourceFeedGap,
        pagesLoaded: Int,
        pagingSource: PagingSource<Long, T>?,
        addsAboveReader: Boolean,
    ) {
        var current = gap
        var loaded = pagesLoaded
        state.update { it.copy(gapLoad = GapLoad(current.after, loaded), gapError = null) }

        try {
            val gapSource = pagingSource ?: newPagingSource()
            while (loaded < CATCH_UP_PAGE_LIMIT) {
                val page = try {
                    loadPage(gapSource, current.nextPageKey)
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    if (source.isEndOfListing(e)) {
                        val closed = current
                        editTimeline({ TimelineUpdate(it.withoutGap(closed), addedCount = 0, gap = null) })
                    } else {
                        state.update { it.copy(gapError = GapError(current, e)) }
                    }
                    return
                }

                loaded++
                val pageRefs = acceptVisibleItems(page.data)
                val loading = current
                val pagesSoFar = loaded
                val result = editTimeline({ it.withGapPage(loading, pageRefs, page.nextKey) }) { loadedState, added ->
                    loadedState.copy(
                        gapLoad = added.gap?.let { GapLoad(it.after, pagesSoFar) },
                        newItemsAvailableCount = loadedState.newItemsAvailableCount +
                            if (addsAboveReader) added.addedCount else 0,
                    )
                }
                current = result.gap ?: return
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            state.update { it.copy(gapError = GapError(current, e)) }
        } finally {
            state.update { it.copy(gapLoad = null) }
        }
    }

    private suspend fun refreshCurrentPageInternal() {
        state.update { it.copy(isRefreshing = true, newItemsAvailableCount = 0, error = null) }

        val page = try {
            loadPage(newPagingSource(), null)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            state.update { it.copy(isRefreshing = false, hasLoaded = true, error = e) }
            return
        }

        val refs = acceptVisibleItems(page.data)
        editTimeline({ TimelineUpdate(SourceFeedTimeline.fromItems(refs, page.nextKey), addedCount = 0, gap = null) }) {
                refreshed,
                _,
            ->
            refreshed.copy(isRefreshing = false, hasLoaded = true)
        }
    }

    private suspend fun appendInternal() {
        var pageKey = state.value.nextPageKey ?: return
        val knownRefs = state.value.itemRefs.toHashSet()
        val newRefs = mutableListOf<FeedItemRef>()
        var nextPageKey: Long? = pageKey
        var error: Throwable? = null
        var pagesScanned = 0

        state.update { it.copy(isAppending = true, appendError = null) }

        try {
            val pagingSource = newPagingSource()
            while (pagesScanned < MAX_APPEND_PAGE_SCANS) {
                val page = try {
                    loadPage(pagingSource, pageKey)
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    if (source.isEndOfListing(e)) nextPageKey = null else error = e
                    break
                }

                pagesScanned++
                newRefs += acceptVisibleItems(page.data).filter(knownRefs::add)
                nextPageKey = page.nextKey
                if (newRefs.isNotEmpty()) break
                pageKey = page.nextKey ?: break
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            error = e
        }

        val appendedNextPageKey = if (error != null) state.value.nextPageKey else nextPageKey
        editTimeline({ timeline ->
            val existing = timeline.resolvedItems()
            val existingSet = existing.toHashSet()
            val appended = timeline.copy(
                items = existing + newRefs.filterNot { it in existingSet },
                legacyMangaIds = emptyList(),
                nextPageKey = appendedNextPageKey,
            )
            TimelineUpdate(appended, addedCount = 0, gap = null)
        }) { appendedState, _ ->
            appendedState.copy(isAppending = false, hasLoaded = true, appendError = error)
        }
    }

    private suspend fun newPagingSource(): PagingSource<Long, T> {
        return source.createPagingSource(ensureFiltersLoaded())
    }

    private suspend fun loadPage(
        pagingSource: PagingSource<Long, T>,
        pageKey: Long?,
    ): PagingSource.LoadResult.Page<Long, T> {
        val params: PagingSource.LoadParams<Long> = if (pageKey == null) {
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = PAGE_SIZE,
                placeholdersEnabled = false,
            )
        } else {
            PagingSource.LoadParams.Append(
                key = pageKey,
                loadSize = PAGE_SIZE,
                placeholdersEnabled = false,
            )
        }

        return when (val result = pagingSource.load(params)) {
            is PagingSource.LoadResult.Page -> result
            is PagingSource.LoadResult.Error -> throw result.throwable
            is PagingSource.LoadResult.Invalid -> error("Invalid paging state for feed $feedId")
        }
    }

    private suspend fun ensureFiltersLoaded(): EntryFilterList {
        resolvedFilters?.let { return it }
        val filters = source.resolveFilters()
        filters.requireValidFilters()
        val savedState = browseFeedService.stateSnapshot()
        val presetId = savedState.feeds.firstOrNull { it.id == feedId }?.presetId
        val original = savedState.presets.firstOrNull { it.id == presetId }
        if (original != null && original.filters == initialFilterSnapshot) {
            browseFeedService.migratePresetFilters(original, filters.snapshot())
        }
        resolvedFilters = filters
        return filters
    }

    private fun acceptVisibleItems(items: List<T>): List<FeedItemRef> {
        val visibleItems = if (hideInLibraryItems) items.filterNot(source::isItemInLibrary) else items
        itemStore.seedAll(visibleItems)
        return visibleItems.map(source::itemRef)
    }

    private suspend fun normalizeTimeline() {
        val refs = state.value.itemRefs
        if (refs.isEmpty()) return

        val anchor = currentSavedAnchor
        val keptRefs = source.filterNonLibraryRefs(refs)

        val normalizedAnchor = anchor.takeIf {
            it.resolvedItem() == null || it.resolvedItem() in keptRefs
        } ?: SourceFeedAnchor()

        if (keptRefs == refs && normalizedAnchor == anchor) return

        if (keptRefs != refs) {
            editTimeline({ TimelineUpdate(it.retainingItems(keptRefs), addedCount = 0, gap = null) }) { normalized, _ ->
                normalized.copy(hasLoaded = keptRefs.isNotEmpty())
            }
        }

        if (normalizedAnchor != anchor) {
            currentSavedAnchor = normalizedAnchor
            persistedAnchor = normalizedAnchor
            browseFeedService.saveAnchor(feedId, normalizedAnchor)
            state.update { it.copy(savedAnchor = normalizedAnchor) }
        }
    }

    /**
     * Applies [edit] to the current timeline and [andState] to the resulting state in one atomic update, then saves
     * the timeline. Both run again if the state changed in between, so they must not have side effects.
     */
    private fun editTimeline(
        edit: (SourceFeedTimeline) -> TimelineUpdate,
        andState: (State, TimelineUpdate) -> State = { edited, _ -> edited },
    ): TimelineUpdate {
        while (true) {
            val current = state.value
            val result = edit(current.timeline())
            if (state.compareAndSet(current, andState(current.withTimeline(result.timeline), result))) {
                persistTimeline()
                return result
            }
        }
    }

    // Saves whatever is current, under a lock, so a slower save can never overwrite a newer timeline.
    private fun persistTimeline() {
        synchronized(persistLock) {
            val timeline = state.value.timeline()
            if (timeline == lastPersistedTimeline) return
            lastPersistedTimeline = timeline
            browseFeedService.saveTimeline(feedId = feedId, timeline = timeline)
        }
    }

    @Immutable
    data class State(
        val itemRefs: List<FeedItemRef> = emptyList(),
        val nextPageKey: Long? = null,
        val gaps: List<SourceFeedGap> = emptyList(),
        val newItems: SourceFeedNewItems? = null,
        val savedAnchor: SourceFeedAnchor = SourceFeedAnchor(),
        val isRefreshing: Boolean = false,
        val isAppending: Boolean = false,
        val gapLoad: GapLoad? = null,
        val gapError: GapError? = null,
        val appendError: Throwable? = null,
        /** Items a refresh added above where the reader was, for the chip that jumps to them. */
        val newItemsAvailableCount: Int = 0,
        val hasLoaded: Boolean = false,
        /** Why the last refresh failed before it loaded anything. */
        val error: Throwable? = null,
    )

    /** The gap below [after] is loading; [pagesLoaded] of at most [CATCH_UP_PAGE_LIMIT] pages are in. */
    @Immutable
    data class GapLoad(
        val after: FeedItemRef,
        val pagesLoaded: Int,
    )

    @Immutable
    data class GapError(
        val gap: SourceFeedGap,
        val error: Throwable,
    )

    companion object {
        /** Pages one refresh or one tap on a gap loads before it leaves the rest as a gap. */
        const val CATCH_UP_PAGE_LIMIT = 10

        private const val PAGE_SIZE = 25
        private const val MAX_APPEND_PAGE_SCANS = 10

        private fun initialState(
            browseFeedService: BrowseFeedService,
            feedId: String,
        ): State {
            val timeline = browseFeedService.timelineSnapshot(feedId)
            return State(
                savedAnchor = browseFeedService.anchorSnapshot(feedId).resolvedAnchor(),
                hasLoaded = timeline.resolvedItems().isNotEmpty(),
            ).withTimeline(timeline)
        }
    }
}

private fun FeedViewModel.State.timeline(): SourceFeedTimeline {
    return SourceFeedTimeline.fromItems(itemRefs, nextPageKey, gaps, newItems)
}

private fun FeedViewModel.State.withTimeline(timeline: SourceFeedTimeline): FeedViewModel.State {
    return copy(
        itemRefs = timeline.resolvedItems(),
        nextPageKey = timeline.nextPageKey,
        gaps = timeline.gaps,
        newItems = timeline.newItems,
    )
}

private fun SourceFeedAnchor.resolvedAnchor(): SourceFeedAnchor {
    return SourceFeedAnchor.fromItem(resolvedItem(), scrollOffset)
}
