package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingSource
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedAnchor
import eu.kanade.domain.source.model.SourceFeedTimeline
import eu.kanade.domain.source.model.snapshot
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.requireValidFilters
import eu.kanade.tachiyomi.ui.base.observation.ObservedItemStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.util.concurrent.atomic.AtomicLong

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
    private val refreshGeneration = AtomicLong()
    private val itemStore = ObservedItemStore(
        scope = viewModelScope + workerDispatcher,
        keyOf = source::itemRef,
        supersedes = source::supersedes,
        observeItems = source::observeItems,
    )

    init {
        viewModelScope.launch(workerDispatcher) {
            browseFeedService.timeline(feedId).collectLatest { timeline ->
                val items = timeline.resolvedItems()
                state.update {
                    it.copy(
                        itemRefs = items,
                        nextPageKey = timeline.nextPageKey,
                        hasLoaded = it.hasLoaded || items.isNotEmpty(),
                    )
                }
            }
        }

        viewModelScope.launch(workerDispatcher) {
            if (hideInLibraryItems) {
                normalizeTimeline()
            }

            if (browseFeedService.timelineSnapshot(feedId).resolvedItems().isEmpty()) {
                refreshInternal(manual = false)
            }
        }
    }

    fun refresh(manual: Boolean = false) {
        val currentState = state.value
        if (currentState.isRefreshing || currentState.isBridgingRefresh) return

        val pendingRefresh = currentState.pendingRefresh
        if (pendingRefresh != null) {
            if (pendingRefresh.nextPageKey == null) return
            viewModelScope.launch(workerDispatcher) {
                resumePendingRefresh(currentState, pendingRefresh)
            }
            return
        }

        viewModelScope.launch(workerDispatcher) {
            refreshInternal(manual = manual)
        }
    }

    fun loadMore() {
        val currentState = state.value
        if (
            currentState.isRefreshing ||
            currentState.isBridgingRefresh ||
            currentState.isAppending ||
            currentState.pendingRefresh != null ||
            currentState.nextPageKey == null
        ) {
            return
        }

        viewModelScope.launch(workerDispatcher) {
            appendInternal()
        }
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
            if (it.newItemsAvailableCount == 0 || it.pendingRefresh != null) {
                it
            } else {
                it.copy(newItemsAvailableCount = 0)
            }
        }
    }

    fun showNewItems() {
        while (true) {
            val currentState = state.value
            val pendingRefresh = currentState.pendingRefresh
            if (pendingRefresh == null) {
                consumeNewItemsIndicator()
                return
            }
            // While more pages remain, the refresh can still reach the current timeline and keep
            // the reading position. Only a refresh that exhausted the source without reaching it
            // replaces the timeline.
            if (pendingRefresh.nextPageKey != null) return

            val anchor = SourceFeedAnchor.fromItem(pendingRefresh.itemRefs.firstOrNull(), scrollOffset = 0)
            val activatedState = currentState.copy(
                itemRefs = pendingRefresh.itemRefs,
                nextPageKey = pendingRefresh.nextPageKey,
                savedAnchor = anchor,
                pendingRefresh = null,
                newItemsAvailableCount = 0,
                newItemsCountIsLowerBound = false,
            )
            if (state.compareAndSet(currentState, activatedState)) {
                currentSavedAnchor = anchor
                persistedAnchor = anchor
                browseFeedService.saveAnchor(feedId = feedId, anchor = anchor)
                persistTimeline(
                    itemRefs = pendingRefresh.itemRefs,
                    nextPageKey = pendingRefresh.nextPageKey,
                )
                return
            }
        }
    }

    fun itemState(ref: FeedItemRef): StateFlow<T?> {
        return itemStore.stateOf(ref)
    }

    private suspend fun refreshInternal(manual: Boolean) {
        if (!chronological) {
            refreshCurrentPageInternal(manual = manual)
            return
        }

        val currentState = state.value
        val generation = refreshGeneration.incrementAndGet()
        val existingRefs = currentState.itemRefs
        val existingRefSet = existingRefs.toHashSet()
        var error: Throwable? = null

        state.update {
            it.copy(
                isRefreshing = true,
                isManualRefresh = manual,
                newItemsAvailableCount = if (manual && it.pendingRefresh == null) {
                    0
                } else {
                    it.newItemsAvailableCount
                },
                error = null,
            )
        }

        val pagingSource = try {
            newPagingSource()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            state.update {
                it.copy(
                    isRefreshing = false,
                    isManualRefresh = false,
                    hasLoaded = true,
                    error = e,
                )
            }
            return
        }

        val page = try {
            loadPage(pagingSource, null)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            error = e
            null
        }

        if (page == null) {
            state.update {
                it.copy(
                    isRefreshing = false,
                    isManualRefresh = false,
                    hasLoaded = true,
                    error = error,
                )
            }
            return
        }

        val pageRefs = acceptVisibleItems(page.data)
        val overlapIndex = pageRefs.indexOfFirst { it in existingRefSet }

        if (existingRefs.isNotEmpty() && overlapIndex < 0 && pageRefs.isNotEmpty()) {
            val pendingRefresh = PendingRefresh(
                itemRefs = pageRefs,
                nextPageKey = page.nextKey,
            )
            state.update {
                it.copy(
                    isRefreshing = false,
                    isManualRefresh = false,
                    pendingRefresh = pendingRefresh,
                    isBridgingRefresh = page.nextKey != null,
                    newItemsAvailableCount = pageRefs.size,
                    newItemsCountIsLowerBound = page.nextKey != null,
                    hasLoaded = true,
                    error = null,
                )
            }
            bridgePendingRefresh(
                pagingSource = pagingSource,
                generation = generation,
                existingRefs = existingRefs,
                existingNextPageKey = currentState.nextPageKey,
                initialRefresh = pendingRefresh,
            )
            return
        }

        val prependedRefs = if (existingRefs.isEmpty()) {
            pageRefs
        } else {
            pageRefs.take(overlapIndex.coerceAtLeast(0))
        }
        val mergedRefs = if (existingRefs.isEmpty()) pageRefs else (prependedRefs + existingRefs).distinct()
        val nextPageKey = if (existingRefs.isEmpty()) page.nextKey else currentState.nextPageKey

        persistTimeline(itemRefs = mergedRefs, nextPageKey = nextPageKey)

        state.update {
            it.copy(
                itemRefs = mergedRefs,
                nextPageKey = nextPageKey,
                isRefreshing = false,
                isManualRefresh = false,
                newItemsAvailableCount = if (manual && error == null) prependedRefs.size else 0,
                newItemsCountIsLowerBound = false,
                pendingRefresh = null,
                hasLoaded = true,
                error = error,
            )
        }
    }

    private suspend fun bridgePendingRefresh(
        pagingSource: PagingSource<Long, T>,
        generation: Long,
        existingRefs: List<FeedItemRef>,
        existingNextPageKey: Long?,
        initialRefresh: PendingRefresh,
    ) {
        val existingRefSet = existingRefs.toHashSet()
        val bridgedRefs = initialRefresh.itemRefs.toMutableList()
        val bridgedRefSet = bridgedRefs.toHashSet()
        val loadedRefs = bridgedRefs.toHashSet()
        var nextPageKey = initialRefresh.nextPageKey

        while (nextPageKey != null && refreshGeneration.get() == generation) {
            val page = try {
                loadPage(pagingSource, nextPageKey)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                if (refreshGeneration.get() == generation) {
                    state.update {
                        if (refreshGeneration.get() != generation || it.pendingRefresh == null) {
                            it
                        } else {
                            it.copy(
                                isBridgingRefresh = false,
                                error = e,
                            )
                        }
                    }
                }
                return
            }

            if (refreshGeneration.get() != generation) return

            val pageRefs = acceptVisibleItems(page.data)
            val overlapIndex = pageRefs.indexOfFirst { it in existingRefSet }

            if (overlapIndex >= 0) {
                pageRefs.take(overlapIndex).forEach { ref ->
                    if (bridgedRefSet.add(ref)) bridgedRefs += ref
                }
                val mergedRefs = (bridgedRefs + existingRefs).distinct()

                while (refreshGeneration.get() == generation) {
                    val currentState = state.value
                    if (currentState.pendingRefresh == null) return
                    val mergedState = currentState.copy(
                        itemRefs = mergedRefs,
                        nextPageKey = existingNextPageKey,
                        pendingRefresh = null,
                        isBridgingRefresh = false,
                        newItemsAvailableCount = bridgedRefs.size,
                        newItemsCountIsLowerBound = false,
                        error = null,
                    )
                    if (state.compareAndSet(currentState, mergedState)) {
                        persistTimeline(
                            itemRefs = mergedRefs,
                            nextPageKey = existingNextPageKey,
                        )
                        return
                    }
                }
                return
            }

            pageRefs.forEach { ref ->
                if (bridgedRefSet.add(ref)) bridgedRefs += ref
            }
            // The bridge has no page limit, so a source that keeps reporting a next page while
            // repeating items it already returned would otherwise be paged forever.
            val pageAdvanced = page.data.map(source::itemRef).count(loadedRefs::add) > 0
            nextPageKey = page.nextKey.takeIf { pageAdvanced }
            state.update {
                if (refreshGeneration.get() != generation || it.pendingRefresh == null) {
                    it
                } else {
                    it.copy(
                        pendingRefresh = PendingRefresh(
                            itemRefs = bridgedRefs.toList(),
                            nextPageKey = nextPageKey,
                        ),
                        isBridgingRefresh = nextPageKey != null,
                        newItemsAvailableCount = bridgedRefs.size,
                        newItemsCountIsLowerBound = nextPageKey != null,
                        error = null,
                    )
                }
            }
        }
    }

    private suspend fun resumePendingRefresh(
        currentState: State,
        pendingRefresh: PendingRefresh,
    ) {
        val generation = refreshGeneration.incrementAndGet()
        state.update {
            if (it.pendingRefresh != pendingRefresh) {
                it
            } else {
                it.copy(
                    isBridgingRefresh = true,
                    error = null,
                )
            }
        }

        val pagingSource = try {
            newPagingSource()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            state.update {
                if (refreshGeneration.get() != generation || it.pendingRefresh == null) {
                    it
                } else {
                    it.copy(
                        isBridgingRefresh = false,
                        error = e,
                    )
                }
            }
            return
        }

        bridgePendingRefresh(
            pagingSource = pagingSource,
            generation = generation,
            existingRefs = currentState.itemRefs,
            existingNextPageKey = currentState.nextPageKey,
            initialRefresh = pendingRefresh,
        )
    }

    private suspend fun refreshCurrentPageInternal(manual: Boolean) {
        var error: Throwable? = null

        state.update {
            it.copy(
                isRefreshing = true,
                isManualRefresh = manual,
                newItemsAvailableCount = 0,
                error = null,
            )
        }

        val pagingSource = try {
            newPagingSource()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            state.update {
                it.copy(
                    isRefreshing = false,
                    isManualRefresh = false,
                    hasLoaded = true,
                    error = e,
                )
            }
            return
        }

        val page = try {
            loadPage(pagingSource, null)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            error = e
            null
        }

        val refs = page?.data?.let(::acceptVisibleItems).orEmpty()
        val nextPageKey = page?.nextKey

        if (page != null) {
            persistTimeline(
                itemRefs = refs,
                nextPageKey = nextPageKey,
            )
        }

        state.update {
            it.copy(
                itemRefs = if (page != null) refs else it.itemRefs,
                nextPageKey = if (page != null) nextPageKey else it.nextPageKey,
                isRefreshing = false,
                isManualRefresh = false,
                newItemsAvailableCount = 0,
                hasLoaded = true,
                error = error,
            )
        }
    }

    private suspend fun appendInternal() {
        val currentState = state.value
        var currentPageKey = currentState.nextPageKey ?: return
        val currentRefs = currentState.itemRefs.toMutableList()
        val currentRefSet = currentRefs.toHashSet()
        var nextPageKey = currentState.nextPageKey
        var error: Throwable? = null
        var pagesScanned = 0

        state.update {
            it.copy(
                isAppending = true,
                error = null,
            )
        }

        val pagingSource = try {
            newPagingSource()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            state.update {
                it.copy(
                    isAppending = false,
                    hasLoaded = true,
                    error = e,
                )
            }
            return
        }

        while (pagesScanned < MAX_APPEND_PAGE_SCANS) {
            val page = try {
                loadPage(pagingSource, currentPageKey)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                error = e
                break
            }

            pagesScanned++

            val newRefs = acceptVisibleItems(page.data).filter(currentRefSet::add)
            nextPageKey = page.nextKey

            if (newRefs.isNotEmpty()) {
                currentRefs += newRefs
                break
            }

            currentPageKey = page.nextKey ?: break
        }

        persistTimeline(
            itemRefs = currentRefs,
            nextPageKey = nextPageKey,
        )

        state.update {
            it.copy(
                itemRefs = currentRefs,
                nextPageKey = nextPageKey,
                isAppending = false,
                hasLoaded = true,
                error = error,
            )
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
        val timeline = browseFeedService.timelineSnapshot(feedId)
        val refs = timeline.resolvedItems()
        if (refs.isEmpty()) return

        val anchor = browseFeedService.anchorSnapshot(feedId).resolvedAnchor()
        val normalizedRefs = source.filterNonLibraryRefs(refs)

        val normalizedAnchor = anchor.takeIf {
            it.resolvedItem() == null || it.resolvedItem() in normalizedRefs
        } ?: SourceFeedAnchor()

        if (normalizedRefs == refs && normalizedAnchor == anchor) return

        persistTimeline(
            itemRefs = normalizedRefs,
            nextPageKey = timeline.nextPageKey,
        )

        if (normalizedAnchor != anchor) {
            currentSavedAnchor = normalizedAnchor
            persistedAnchor = normalizedAnchor
            browseFeedService.saveAnchor(feedId, normalizedAnchor)
        }

        state.update {
            it.copy(
                itemRefs = normalizedRefs,
                nextPageKey = timeline.nextPageKey,
                savedAnchor = normalizedAnchor,
                hasLoaded = normalizedRefs.isNotEmpty(),
            )
        }
    }

    private fun persistTimeline(itemRefs: List<FeedItemRef>, nextPageKey: Long?) {
        browseFeedService.saveTimeline(
            feedId = feedId,
            timeline = SourceFeedTimeline.fromItems(itemRefs, nextPageKey),
        )
    }

    @Immutable
    data class State(
        val itemRefs: List<FeedItemRef> = emptyList(),
        val nextPageKey: Long? = null,
        val savedAnchor: SourceFeedAnchor = SourceFeedAnchor(),
        val isRefreshing: Boolean = false,
        val isManualRefresh: Boolean = false,
        val isAppending: Boolean = false,
        val isBridgingRefresh: Boolean = false,
        val newItemsAvailableCount: Int = 0,
        val newItemsCountIsLowerBound: Boolean = false,
        val pendingRefresh: PendingRefresh? = null,
        val hasLoaded: Boolean = false,
        val error: Throwable? = null,
    )

    @Immutable
    data class PendingRefresh(
        val itemRefs: List<FeedItemRef>,
        val nextPageKey: Long?,
    )

    companion object {
        private const val PAGE_SIZE = 25
        private const val MAX_APPEND_PAGE_SCANS = 10

        private fun initialState(
            browseFeedService: BrowseFeedService,
            feedId: String,
        ): State {
            val timeline = browseFeedService.timelineSnapshot(feedId)
            return State(
                itemRefs = timeline.resolvedItems(),
                nextPageKey = timeline.nextPageKey,
                savedAnchor = browseFeedService.anchorSnapshot(feedId).resolvedAnchor(),
                hasLoaded = timeline.resolvedItems().isNotEmpty(),
            )
        }
    }
}

private fun SourceFeedAnchor.resolvedAnchor(): SourceFeedAnchor {
    return SourceFeedAnchor.fromItem(resolvedItem(), scrollOffset)
}
