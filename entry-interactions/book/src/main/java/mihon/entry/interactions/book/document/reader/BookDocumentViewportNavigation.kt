package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.position.BookDocumentViewportGeometry
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import tachiyomi.domain.entry.model.EntryChapter

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

/** Align a seek/return passage using measured text, clamped to the current section's scroll range. */
internal suspend fun LazyListState.scrollToBookDocumentPassage(
    section: BookDocumentSection<EntryChapter>,
    position: BookDocumentPosition,
    items: BookDocumentViewerDataset<EntryChapter>,
    geometry: BookDocumentViewportGeometry,
) {
    val index = items.indexOfPosition(section.key, position)
    scrollToBookDocumentPosition(section.document, position, index)
    withFrameNanos { }
    geometry.lineTop(section, position)?.let { scrollBy(it) }
    val document = section.document.document
    val lastIndex = items.indexOfPosition(section.key, document.positionAtProgression(1f))
    layoutInfo.visibleItemsInfo.firstOrNull { it.index == lastIndex }?.let { last ->
        val belowEnd = last.offset + last.size - layoutInfo.viewportEndOffset
        if (belowEnd < 0) scrollBy(belowEnd.toFloat())
    }
    val firstIndex = items.indexOfPosition(section.key, document.positionAtProgression(0f))
    layoutInfo.visibleItemsInfo.firstOrNull { it.index == firstIndex }?.let { first ->
        val aboveStart = first.offset - layoutInfo.viewportStartOffset
        if (aboveStart > 0) scrollBy(aboveStart.toFloat())
    }
}
