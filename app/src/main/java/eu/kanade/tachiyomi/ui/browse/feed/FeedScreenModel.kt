package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.runtime.Immutable
import androidx.paging.PagingSource
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedAnchor
import eu.kanade.domain.source.model.SourceFeedTimeline
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class FeedScreenModel<T : Any>(
    private val feedId: String,
    protected val listingQuery: String?,
    protected val initialFilterSnapshot: List<eu.kanade.domain.source.model.FilterStateNode>,
    private val chronological: Boolean = true,
    private val browseFeedService: BrowseFeedService,
    private val hideInLibraryItems: Boolean,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StateScreenModel<FeedScreenModel.State>(initialState(browseFeedService, feedId)) {

    private var currentSavedAnchor = browseFeedService.anchorSnapshot(feedId).resolvedAnchor()
    private var persistedAnchor = currentSavedAnchor
    private var resolvedFilters: EntryFilterList? = null

    init {
        screenModelScope.launch(workerDispatcher) {
            browseFeedService.timeline(feedId).collectLatest { timeline ->
                val items = timeline.resolvedItems()
                mutableState.update {
                    it.copy(
                        itemRefs = items,
                        nextPageKey = timeline.nextPageKey,
                        hasLoaded = it.hasLoaded || items.isNotEmpty(),
                    )
                }
            }
        }

        screenModelScope.launch(workerDispatcher) {
            if (hideInLibraryItems) {
                normalizeTimeline()
            }

            if (browseFeedService.timelineSnapshot(feedId).resolvedItems().isEmpty()) {
                refreshInternal(manual = false)
            }
        }
    }

    fun refresh(manual: Boolean = false) {
        if (state.value.isRefreshing) return
        screenModelScope.launch(workerDispatcher) {
            refreshInternal(manual = manual)
        }
    }

    fun loadMore() {
        val currentState = state.value
        if (currentState.isRefreshing || currentState.isAppending || currentState.nextPageKey == null) return

        screenModelScope.launch(workerDispatcher) {
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
        mutableState.update {
            if (it.newItemsAvailableCount == 0) {
                it
            } else {
                it.copy(newItemsAvailableCount = 0)
            }
        }
    }

    abstract suspend fun subscribeItem(ref: FeedItemRef): Flow<T>

    protected abstract suspend fun resolveFilters(): EntryFilterList

    protected abstract fun createPagingSource(filters: EntryFilterList): PagingSource<Long, T>

    protected abstract fun itemRef(item: T): FeedItemRef

    protected abstract fun isItemInLibrary(item: T): Boolean

    protected abstract suspend fun filterNonLibraryRefs(refs: List<FeedItemRef>): List<FeedItemRef>

    private suspend fun refreshInternal(manual: Boolean) {
        if (!chronological) {
            refreshCurrentPageInternal(manual = manual)
            return
        }

        val currentState = state.value
        val existingRefs = currentState.itemRefs
        val existingRefSet = existingRefs.toHashSet()
        val prependedRefs = mutableListOf<FeedItemRef>()
        var nextPageKey: Long? = null
        var currentPageKey: Long? = null
        var pageCount = 0
        var error: Throwable? = null

        mutableState.update {
            it.copy(
                isRefreshing = true,
                isManualRefresh = manual,
                newItemsAvailableCount = if (manual) 0 else it.newItemsAvailableCount,
                error = null,
            )
        }

        val pagingSource = try {
            newPagingSource()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            mutableState.update {
                it.copy(
                    isRefreshing = false,
                    isManualRefresh = false,
                    hasLoaded = true,
                    error = e,
                )
            }
            return
        }

        while (pageCount < MAX_REFRESH_PAGES) {
            val page = try {
                loadPage(pagingSource, currentPageKey)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                error = e
                break
            }

            pageCount++
            val pageRefs = visibleRefs(page.data)
            nextPageKey = page.nextKey

            if (existingRefs.isEmpty()) {
                prependedRefs += pageRefs
                break
            }

            val overlapIndex = pageRefs.indexOfFirst { it in existingRefSet }
            if (overlapIndex >= 0) {
                prependedRefs += pageRefs.take(overlapIndex)
                break
            }

            prependedRefs += pageRefs.filterNot(existingRefSet::contains)

            currentPageKey = page.nextKey
            if (currentPageKey == null) {
                break
            }
        }

        val mergedRefs = if (existingRefs.isEmpty()) {
            prependedRefs
        } else {
            (prependedRefs + existingRefs).distinct()
        }

        persistTimeline(
            itemRefs = mergedRefs,
            nextPageKey = nextPageKey,
        )

        mutableState.update {
            it.copy(
                itemRefs = mergedRefs,
                nextPageKey = nextPageKey,
                isRefreshing = false,
                isManualRefresh = false,
                newItemsAvailableCount = if (manual && error == null) prependedRefs.size else 0,
                hasLoaded = true,
                error = error,
            )
        }
    }

    private suspend fun refreshCurrentPageInternal(manual: Boolean) {
        var error: Throwable? = null

        mutableState.update {
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
            mutableState.update {
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

        val refs = page?.data?.let(::visibleRefs).orEmpty()
        val nextPageKey = page?.nextKey

        if (page != null) {
            persistTimeline(
                itemRefs = refs,
                nextPageKey = nextPageKey,
            )
        }

        mutableState.update {
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

        mutableState.update {
            it.copy(
                isAppending = true,
                error = null,
            )
        }

        val pagingSource = try {
            newPagingSource()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            mutableState.update {
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

            val newRefs = visibleRefs(page.data).filter(currentRefSet::add)
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

        mutableState.update {
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
        return createPagingSource(ensureFiltersLoaded())
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
        val filters = resolveFilters()
        resolvedFilters = filters
        return filters
    }

    private fun visibleRefs(items: List<T>): List<FeedItemRef> {
        if (!hideInLibraryItems) return items.map(::itemRef)

        return items.filterNot(::isItemInLibrary).map(::itemRef)
    }

    private suspend fun normalizeTimeline() {
        val timeline = browseFeedService.timelineSnapshot(feedId)
        val refs = timeline.resolvedItems()
        if (refs.isEmpty()) return

        val anchor = browseFeedService.anchorSnapshot(feedId).resolvedAnchor()
        val normalizedRefs = filterNonLibraryRefs(refs)

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

        mutableState.update {
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
        val newItemsAvailableCount: Int = 0,
        val hasLoaded: Boolean = false,
        val error: Throwable? = null,
    )

    companion object {
        private const val PAGE_SIZE = 25
        private const val MAX_REFRESH_PAGES = 10
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
