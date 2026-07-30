package mihon.entry.interactions.book.prose

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.filter
import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.viewer.EntryChildTransition
import mihon.entry.interactions.viewer.EntryChildWindow
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.math.roundToInt
internal fun buildPaginatedItems(
    window: EntryChildWindow<EntryChapter>,
    pages: Map<Long, List<HtmlProsePage>>,
): List<ProsePagerItem> = buildList {
    window.previous?.let { previous ->
        pages[previous.id]?.takeIf(List<*>::isNotEmpty)?.mapTo(this) { ProsePagerItem.Page(it) }
    }
    add(ProsePagerItem.Transition(window.previousTransition()))
    requireNotNull(pages[window.current.id]?.takeIf(List<*>::isNotEmpty)) {
        "The current prose chapter must have at least one page"
    }.mapTo(this) { ProsePagerItem.Page(it) }
    add(ProsePagerItem.Transition(window.nextTransition()))
    window.next?.let { next ->
        pages[next.id]?.takeIf(List<*>::isNotEmpty)?.mapTo(this) { ProsePagerItem.Page(it) }
    }
}

internal fun prosePagerDatasetAnchor(
    previousItemKeys: List<String>,
    items: List<ProsePagerItem>,
    settledPage: Int,
): Int? {
    val settledKey = previousItemKeys.getOrNull(settledPage) ?: return null
    return items.indexOfFirst { it.key == settledKey }.takeIf { it >= 0 }
}

internal fun initialPaginatedItemIndex(
    items: List<ProsePagerItem>,
    chapterId: Long,
    progression: Float,
    sourceOffset: Int? = null,
): Int {
    val chapterPages = items.withIndex().filter { (_, item) ->
        item is ProsePagerItem.Page && item.page.chapter.id == chapterId
    }
    if (chapterPages.isEmpty()) return 0
    val documentEnd = chapterPages.maxOf { (_, item) ->
        (item as ProsePagerItem.Page).page.sourceEndExclusive
    }
    val targetOffset = sourceOffset?.coerceIn(0, documentEnd)
        ?: (documentEnd * progression.coerceIn(0f, 1f)).roundToInt()
    return chapterPages.firstOrNull { (_, item) ->
        val page = (item as ProsePagerItem.Page).page
        targetOffset >= page.sourceStart &&
            (targetOffset < page.sourceEndExclusive || page.index == page.total - 1)
    }?.index ?: chapterPages.last().index
}

internal fun structuredBlockPositionOffset(
    blockLength: Int,
    scrollValue: Int,
    maxScrollValue: Int,
    contentFullyVisible: Boolean = true,
): Int {
    if (blockLength <= 0) return 0
    if (maxScrollValue <= 0) return if (contentFullyVisible) blockLength else 0
    return (blockLength * scrollValue.coerceIn(0, maxScrollValue).toFloat() / maxScrollValue)
        .roundToInt()
        .coerceIn(0, blockLength)
}

internal fun structuredBlockScrollValue(
    offsetWithinBlock: Int,
    blockLength: Int,
    maxScrollValue: Int,
): Int {
    if (blockLength <= 0 || maxScrollValue <= 0) return 0
    return (maxScrollValue * offsetWithinBlock.coerceIn(0, blockLength).toFloat() / blockLength)
        .roundToInt()
        .coerceIn(0, maxScrollValue)
}

internal sealed interface ProsePagerItem {
    val key: String

    data class Page(val page: HtmlProsePage) : ProsePagerItem {
        override val key = "page:${page.chapter.id}:${page.index}"
    }

    data class Transition(val transition: EntryChildTransition<EntryChapter>) : ProsePagerItem {
        override val key = "transition:${transitionKey(transition)}"
    }
}

internal data class ProseViewerPosition(
    val chapterId: Long,
    val progression: Float,
    val currentPage: Int,
    val totalPages: Int,
    val documentPosition: BookDocumentPosition?,
)

internal data class ProseViewerWindowAnchor(
    val destinationChapterId: Long,
    val itemKey: String,
    val scrollOffset: Int,
)

internal data class PendingBookDocumentAnchor(
    val chapterId: Long,
    val position: BookDocumentPosition,
)

internal data class ProseViewerRestorationLayout(
    val itemSize: Int,
    val viewportStartOffset: Int,
    val viewportEndOffset: Int,
)

internal data class ProseViewerActions(
    val seekPage: (Int) -> Unit = {},
    val seekProgress: (Float) -> Unit = {},
    val previousSection: () -> Unit = {},
    val nextSection: () -> Unit = {},
    val onTapFraction: (Float) -> Unit = {},
)
