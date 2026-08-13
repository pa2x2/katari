package mihon.entry.interactions.book.document.reader

import mihon.book.api.document.BookDocumentPosition
import kotlin.math.roundToInt

/** Maps the active document section onto its visual scroll range without changing its resume locator. */
internal fun <T> bookDocumentVisualProgress(
    section: BookDocumentSection<T>,
    items: List<BookDocumentViewerItem<T>>,
    visibleItems: List<BookDocumentVisibleItemLayout>,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
): Float? {
    val visibleBlocks = visibleItems.mapNotNull { layout ->
        val item = (
            items.getOrNull(layout.index)?.takeIf { it.key == layout.key }
                ?: items.firstOrNull { it.key == layout.key }
            ) as? BookDocumentViewerItem.Block ?: return@mapNotNull null
        if (item.section.key != section.key) return@mapNotNull null
        VisibleDocumentBlock(item, layout)
    }
    if (visibleBlocks.isEmpty()) return null

    val document = section.document.document
    val firstBlockId = document.blocks.firstOrNull()?.id ?: return null
    val lastBlockId = document.blocks.lastOrNull()?.id ?: return null
    val firstBlock = visibleBlocks.firstOrNull { it.item.content.id == firstBlockId }
    if (firstBlock != null && firstBlock.layout.offset >= viewportStartOffset) return 0f

    val lastBlock = visibleBlocks.firstOrNull { it.item.content.id == lastBlockId }
    if (lastBlock != null && lastBlock.layout.endOffset <= viewportEndOffset) return 1f

    val visibleStart = progressionAtViewportOffset(
        section = section,
        visibleBlocks = visibleBlocks,
        viewportOffset = viewportStartOffset,
    )
    val visibleEnd = progressionAtViewportOffset(
        section = section,
        visibleBlocks = visibleBlocks,
        viewportOffset = viewportEndOffset,
    )
    val visibleExtent = (visibleEnd - visibleStart).coerceIn(0f, 1f)
    val scrollableExtent = 1f - visibleExtent
    if (scrollableExtent <= MIN_SCROLLABLE_EXTENT) {
        return visibleStart.coerceIn(0f, 1f)
    }
    return (visibleStart / scrollableExtent).coerceIn(0f, 1f)
}

private fun <T> progressionAtViewportOffset(
    section: BookDocumentSection<T>,
    visibleBlocks: List<VisibleDocumentBlock<T>>,
    viewportOffset: Int,
): Float {
    val visibleBlock = visibleBlocks.firstOrNull { viewportOffset in it.layout.offset until it.layout.endOffset }
        ?: visibleBlocks.minBy { block ->
            when {
                viewportOffset < block.layout.offset -> block.layout.offset - viewportOffset
                viewportOffset > block.layout.endOffset -> viewportOffset - block.layout.endOffset
                else -> 0
            }
        }
    val fraction = (viewportOffset - visibleBlock.layout.offset).toFloat()
        .div(visibleBlock.layout.size.coerceAtLeast(1))
        .coerceIn(0f, 1f)
    val position = BookDocumentPosition(
        blockId = visibleBlock.item.content.id,
        offsetWithinBlock = (visibleBlock.item.content.logicalLength * fraction).roundToInt(),
    )
    return section.document.document.progressionAt(position)
}

private data class VisibleDocumentBlock<T>(
    val item: BookDocumentViewerItem.Block<T>,
    val layout: BookDocumentVisibleItemLayout,
)

private val BookDocumentVisibleItemLayout.endOffset: Int
    get() = offset + size

private const val MIN_SCROLLABLE_EXTENT = 0.0001f
