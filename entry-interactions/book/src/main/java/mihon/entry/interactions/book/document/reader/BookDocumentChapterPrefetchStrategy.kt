package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListPrefetchScope
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope

/** Prefetches the first viewport of a prepared adjacent chapter without widening the scrolling cache. */
@OptIn(ExperimentalFoundationApi::class)
internal class BookDocumentChapterPrefetchStrategy : LazyListPrefetchStrategy {
    private val defaultStrategy = LazyListPrefetchStrategy()
    private var target: Target? = null
    private var scheduledTargetKey: String? = null
    private val chapterPrefetchHandles = mutableListOf<LazyLayoutPrefetchState.PrefetchHandle>()

    fun updateTarget(key: String?, index: Int) {
        val updated = key?.let { Target(it, index) }
        if (updated == target) return
        target = updated
        scheduledTargetKey = null
        chapterPrefetchHandles.forEach(LazyLayoutPrefetchState.PrefetchHandle::cancel)
        chapterPrefetchHandles.clear()
    }

    override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
        with(defaultStrategy) {
            this@onScroll.onScroll(delta, layoutInfo)
        }
        scheduleChapterViewportIfNeeded(layoutInfo)
    }

    override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) {
        with(defaultStrategy) {
            this@onVisibleItemsUpdated.onVisibleItemsUpdated(layoutInfo)
        }
        scheduleChapterViewportIfNeeded(layoutInfo)
    }

    override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) {
        with(defaultStrategy) {
            this@onNestedPrefetch.onNestedPrefetch(firstVisibleItemIndex)
        }
    }

    private fun LazyListPrefetchScope.scheduleChapterViewportIfNeeded(layoutInfo: LazyListLayoutInfo) {
        val requested = target ?: return
        if (requested.key == scheduledTargetKey) return
        if (requested.index !in 0 until layoutInfo.totalItemsCount) return
        val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        if (viewportSize <= 0) return
        scheduledTargetKey = requested.key
        scheduleChapterItem(
            target = requested,
            index = requested.index,
            remainingViewport = viewportSize,
            itemCount = layoutInfo.totalItemsCount,
        )
    }

    private fun LazyListPrefetchScope.scheduleChapterItem(
        target: Target,
        index: Int,
        remainingViewport: Int,
        itemCount: Int,
    ) {
        if (target != this@BookDocumentChapterPrefetchStrategy.target) return
        if (index !in 0 until itemCount || remainingViewport <= 0) return
        val prefetchScope = this
        chapterPrefetchHandles += schedulePrefetch(index) {
            if (target != this@BookDocumentChapterPrefetchStrategy.target) return@schedulePrefetch
            prefetchScope.scheduleChapterItem(
                target = target,
                index = index + 1,
                remainingViewport = remainingViewport - mainAxisSize,
                itemCount = itemCount,
            )
        }
    }

    private data class Target(
        val key: String,
        val index: Int,
    )
}
