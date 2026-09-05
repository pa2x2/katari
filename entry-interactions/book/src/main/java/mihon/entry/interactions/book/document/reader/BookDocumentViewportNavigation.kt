package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.book.document.render.PreparedBookDocument

/** Both main reading and contextual previews restore the block and the position within it. */
internal suspend fun LazyListState.scrollToBookDocumentPosition(
    document: PreparedBookDocument,
    position: BookDocumentPosition,
    index: Int,
) {
    if (index < 0) return
    scrollToItem(index)
    val layout = snapshotFlow {
        val info = layoutInfo
        info.visibleItemsInfo.firstOrNull { it.index == index }?.let { item ->
            Triple(item.size, info.viewportStartOffset, info.viewportEndOffset)
        }
    }.filterNotNull().first()
    scrollToItem(
        index,
        bookDocumentScrollOffset(document, position, layout.first, layout.second, layout.third),
    )
}
