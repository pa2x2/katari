package eu.kanade.tachiyomi.ui.browse.feed

import androidx.paging.PagingSource
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import kotlinx.coroutines.flow.Flow

/**
 * What a feed lists and where its items come from. [FeedViewModel] starts loading while it is constructed, so it
 * is handed a finished source instead of calling into a subclass whose own properties are not assigned yet.
 */
interface FeedItemSource<T : Any> {
    suspend fun observeItems(refs: List<FeedItemRef>): Flow<List<T>>

    /** Whether [candidate] should replace [current], the same item observed earlier. */
    fun supersedes(candidate: T, current: T): Boolean

    suspend fun resolveFilters(): EntryFilterList

    fun createPagingSource(filters: EntryFilterList): PagingSource<Long, T>

    fun itemRef(item: T): FeedItemRef

    fun isItemInLibrary(item: T): Boolean

    suspend fun filterNonLibraryRefs(refs: List<FeedItemRef>): List<FeedItemRef>
}
