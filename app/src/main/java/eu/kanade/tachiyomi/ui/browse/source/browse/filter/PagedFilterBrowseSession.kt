package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterNavigationTarget
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageLoadReason
import eu.kanade.tachiyomi.source.entry.EntryFilterPageScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.util.IdentityHashMap

internal data class PagedFilterViewKey(
    val scope: EntryFilterPageScope,
    val query: String?,
)

internal data class PagedFilterViewport(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
)

class PagedFilterBrowseSession internal constructor(
    private val coroutineScope: CoroutineScope,
) {
    internal var scope by mutableStateOf(EntryFilterPageScope.AVAILABLE)
    internal var query by mutableStateOf("")

    private val pagerConfigurations = mutableStateMapOf<PagedFilterViewKey, PagerConfiguration>()
    private val pendingJumpTargetIds = mutableStateMapOf<PagedFilterViewKey, String>()
    private val viewports = mutableMapOf<PagedFilterViewKey, PagedFilterViewport>()
    private val pagerFlows = LinkedHashMap<PagerKey, Flow<PagingData<EntryFilterPageItem>>>(
        MAXIMUM_RETAINED_PAGERS,
        0.75f,
        true,
    )

    internal fun pagerConfiguration(viewKey: PagedFilterViewKey): PagerConfiguration {
        return pagerConfigurations[viewKey] ?: PagerConfiguration()
    }

    internal fun viewport(viewKey: PagedFilterViewKey): PagedFilterViewport {
        return viewports[viewKey] ?: PagedFilterViewport()
    }

    internal fun updateViewport(viewKey: PagedFilterViewKey, viewport: PagedFilterViewport) {
        viewports[viewKey] = viewport
    }

    internal fun refresh(viewKey: PagedFilterViewKey) {
        val current = pagerConfiguration(viewKey)
        pagerConfigurations[viewKey] = current.copy(refreshGeneration = current.refreshGeneration + 1)
        pendingJumpTargetIds.remove(viewKey)
        viewports[viewKey] = PagedFilterViewport()
        removePagerFlows(viewKey)
    }

    internal fun jump(viewKey: PagedFilterViewKey, target: EntryFilterNavigationTarget) {
        pagerConfigurations[viewKey] = PagerConfiguration(initialAnchor = target.anchor)
        pendingJumpTargetIds[viewKey] = target.id
        viewports[viewKey] = PagedFilterViewport()
        removePagerFlows(viewKey)
    }

    internal fun pendingJumpTargetId(viewKey: PagedFilterViewKey): String? {
        return pendingJumpTargetIds[viewKey]
    }

    internal fun consumePendingJumpTarget(viewKey: PagedFilterViewKey, targetId: String) {
        pendingJumpTargetIds.remove(viewKey, targetId)
    }

    internal fun pagingData(
        viewKey: PagedFilterViewKey,
        configuration: PagerConfiguration,
        pageSize: Int,
        pagingSourceFactory: (
            EntryFilterPageLoadReason,
            String?,
        ) -> PagingSource<String, EntryFilterPageItem>,
    ): Flow<PagingData<EntryFilterPageItem>> {
        val pagerKey = PagerKey(viewKey, configuration)
        pagerFlows[pagerKey]?.let { return it }

        val flow = Pager(
            config = PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize,
                prefetchDistance = (pageSize / 2).coerceAtLeast(1),
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                pagingSourceFactory(
                    if (configuration.refreshGeneration == 0) {
                        EntryFilterPageLoadReason.INITIAL
                    } else {
                        EntryFilterPageLoadReason.USER_REFRESH
                    },
                    configuration.initialAnchor,
                )
            },
        ).flow.cachedIn(coroutineScope)
        pagerFlows[pagerKey] = flow
        trimPagerFlows()
        return flow
    }

    private fun removePagerFlows(viewKey: PagedFilterViewKey) {
        pagerFlows.keys.removeAll { it.viewKey == viewKey }
    }

    private fun trimPagerFlows() {
        while (pagerFlows.size > MAXIMUM_RETAINED_PAGERS) {
            pagerFlows.entries.iterator().run {
                next()
                remove()
            }
        }
    }

    internal data class PagerConfiguration(
        val initialAnchor: String? = null,
        val refreshGeneration: Int = 0,
    )

    private data class PagerKey(
        val viewKey: PagedFilterViewKey,
        val configuration: PagerConfiguration,
    )

    private companion object {
        const val MAXIMUM_RETAINED_PAGERS = 8
    }
}

internal class PagedFilterBrowseSessionStore(
    private val coroutineScope: CoroutineScope,
) {
    private val sessions = IdentityHashMap<EntryFilter.PagedGroup<*>, PagedFilterBrowseSession>()

    @Synchronized
    fun session(filter: EntryFilter.PagedGroup<*>): PagedFilterBrowseSession {
        return sessions.getOrPut(filter) { PagedFilterBrowseSession(coroutineScope) }
    }

    @Synchronized
    fun retain(filters: Iterable<EntryFilter<*>>) {
        val retained = IdentityHashMap<EntryFilter.PagedGroup<*>, Unit>()
        filters.forEach { it.collectPagedGroups(retained) }
        sessions.keys.removeAll { !retained.containsKey(it) }
    }
}

private fun EntryFilter<*>.collectPagedGroups(destination: IdentityHashMap<EntryFilter.PagedGroup<*>, Unit>) {
    when (this) {
        is EntryFilter.PagedGroup<*> -> destination[this] = Unit
        is EntryFilter.Group<*> -> state.forEach { (it as? EntryFilter<*>)?.collectPagedGroups(destination) }
        else -> Unit
    }
}
