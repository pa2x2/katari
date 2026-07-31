package mihon.entry.interactions.book.document.reader

import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import mihon.entry.interactions.viewer.EntryChildWindow
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class BookDocumentSection<T>(
    val key: String,
    val owner: T,
    val document: PreparedBookDocument,
    val initialPosition: BookDocumentPosition,
    val resourceLoader: BookPublicationResourceLoader?,
) {
    init {
        require(key.isNotBlank()) { "document section key must not be blank" }
        require(document.document.contains(initialPosition)) { "initial position must belong to the section document" }
    }
}

internal sealed interface BookDocumentViewerItem<T> {
    val key: String

    data class Block<T>(
        val section: BookDocumentSection<T>,
        val content: PreparedBookDocumentBlock,
    ) : BookDocumentViewerItem<T> {
        override val key = "document:${section.key}:${section.document.document.resourceId}:${content.block.id.value}"
    }

    data class Transition<T>(
        val transition: EntryChildTransition<T>,
        override val key: String,
    ) : BookDocumentViewerItem<T>
}

internal fun <T, K> buildBookDocumentViewerItems(
    window: EntryChildWindow<T>,
    loaded: Map<K, BookDocumentSection<T>>,
    keyOf: (T) -> K,
): List<BookDocumentViewerItem<T>> = buildList {
    window.previous?.let { previous ->
        loaded[keyOf(previous)]?.let(::addDocument)
    }
    add(window.previousTransition().toViewerItem(keyOf))
    addDocument(
        requireNotNull(loaded[keyOf(window.current)]) {
            "The current document section must be loaded"
        },
    )
    add(window.nextTransition().toViewerItem(keyOf))
    window.next?.let { next ->
        loaded[keyOf(next)]?.let(::addDocument)
    }
}

private fun <T> MutableList<BookDocumentViewerItem<T>>.addDocument(
    section: BookDocumentSection<T>,
) {
    section.document.blocks.mapTo(this) { block -> BookDocumentViewerItem.Block(section, block) }
}

private fun <T, K> EntryChildTransition<T>.toViewerItem(
    keyOf: (T) -> K,
): BookDocumentViewerItem.Transition<T> {
    val fromKey = keyOf(from).toString()
    val toKey = to?.let(keyOf)?.toString()
    val key = if (toKey == null) {
        "document-transition:$direction:$fromKey:terminal"
    } else {
        "document-transition:${listOf(fromKey, toKey).sorted().joinToString(":")}"
    }
    return BookDocumentViewerItem.Transition(this, key)
}

internal data class BookDocumentVisibleItemLayout(
    val index: Int,
    val key: Any,
    val offset: Int,
    val size: Int,
)

internal data class BookDocumentViewerDatasetAnchor(
    val index: Int,
    val scrollOffset: Int,
)

internal fun <T> bookDocumentViewerDatasetAnchor(
    items: List<BookDocumentViewerItem<T>>,
    visibleItems: List<BookDocumentVisibleItemLayout>,
    viewportStartOffset: Int,
): BookDocumentViewerDatasetAnchor? {
    visibleItems.forEach { layout ->
        val index = items.indexOfFirst { it.key == layout.key }
        if (index >= 0) {
            return BookDocumentViewerDatasetAnchor(
                index = index,
                scrollOffset = viewportStartOffset - layout.offset,
            )
        }
    }
    return null
}

internal data class BookDocumentViewerLocation<T>(
    val section: BookDocumentSection<T>,
    val position: BookDocumentPosition,
    val progression: Float,
)

internal fun <T> bookDocumentViewerLocation(
    items: List<BookDocumentViewerItem<T>>,
    visibleItems: List<BookDocumentVisibleItemLayout>,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
): BookDocumentViewerLocation<T>? {
    val terminalChapter = visibleItems
        .asReversed()
        .firstNotNullOfOrNull { layout ->
            if (
                layout.offset >= viewportEndOffset ||
                layout.offset + layout.size <= viewportStartOffset
            ) {
                return@firstNotNullOfOrNull null
            }
            (layout.resolveViewerItem(items) as? BookDocumentViewerItem.Transition)
                ?.transition
                ?.takeIf { it.direction == EntryChildDirection.NEXT && it.to == null }
                ?.from
        }
    val terminalSection = terminalChapter?.let { chapter ->
        items.firstNotNullOfOrNull { item ->
            (item as? BookDocumentViewerItem.Block)
                ?.section
                ?.takeIf { it.owner == chapter }
        }
    }
    if (terminalSection != null) {
        val position = terminalSection.document.document.positionAtProgression(1f)
        return BookDocumentViewerLocation(
            section = terminalSection,
            position = position,
            progression = 1f,
        )
    }
    val topLayout = visibleItems.firstOrNull { it.offset == viewportStartOffset }
    val topItem = topLayout?.let { layout ->
        (
            items.getOrNull(layout.index)?.takeIf { it.key == layout.key }
                ?: items.firstOrNull { it.key == layout.key }
            ) as? BookDocumentViewerItem.Block
    }
    if (
        topItem != null &&
        topItem.content.block.id == topItem.section.document.document.blocks.firstOrNull()?.id
    ) {
        val position = BookDocumentPosition(topItem.content.block.id, 0)
        return BookDocumentViewerLocation(
            section = topItem.section,
            position = position,
            progression = topItem.section.document.document.progressionAt(position),
        )
    }
    val viewportAnchor = (viewportStartOffset + viewportEndOffset) / 2
    val layout = visibleItems.firstOrNull {
        viewportAnchor >= it.offset && viewportAnchor < it.offset + it.size
    } ?: visibleItems.minByOrNull {
        abs((it.offset + it.size / 2) - viewportAnchor)
    }
        ?: return null
    // A chapter-window update can expose layout indexes from the previous item set for one frame.
    // Keep the index as the normal fast path, but use the stable key when that index has moved.
    val item = (
        items.getOrNull(layout.index)?.takeIf { it.key == layout.key }
            ?: items.firstOrNull { it.key == layout.key }
        ) as? BookDocumentViewerItem.Block ?: return null
    val fraction = (viewportAnchor - layout.offset).toFloat()
        .div(layout.size.coerceAtLeast(1))
        .coerceIn(0f, 1f)
    val position = BookDocumentPosition(
        blockId = item.content.block.id,
        offsetWithinBlock = (item.content.block.logicalLength * fraction).roundToInt(),
    )
    return BookDocumentViewerLocation(
        section = item.section,
        position = position,
        progression = item.section.document.document.progressionAt(position),
    )
}

internal fun <T> bookDocumentViewerTransitionAtAnchor(
    items: List<BookDocumentViewerItem<T>>,
    visibleItems: List<BookDocumentVisibleItemLayout>,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
    canScrollBackward: Boolean = true,
    canScrollForward: Boolean = true,
): EntryChildTransition<T>? {
    val viewportAnchor = (viewportStartOffset + viewportEndOffset) / 2
    val centeredLayout = visibleItems.firstOrNull {
        viewportAnchor >= it.offset && viewportAnchor < it.offset + it.size
    }
    val centeredTransition = centeredLayout?.resolveViewerItem(items)
        ?.let { it as? BookDocumentViewerItem.Transition }
        ?.transition
    if (centeredTransition != null && (canScrollBackward || canScrollForward)) {
        return centeredTransition
    }

    val boundaryLayout = when {
        !canScrollBackward && !canScrollForward -> visibleItems.firstOrNull { layout ->
            val transition = (layout.resolveViewerItem(items) as? BookDocumentViewerItem.Transition)?.transition
            transition?.to != null && items.none { item ->
                item is BookDocumentViewerItem.Block && item.section.owner == transition.to
            }
        }
        !canScrollBackward && canScrollForward -> visibleItems.firstOrNull()
        !canScrollForward && canScrollBackward -> visibleItems.lastOrNull()
        else -> null
    }
    return boundaryLayout
        ?.resolveViewerItem(items)
        ?.let { it as? BookDocumentViewerItem.Transition }
        ?.transition
        ?: centeredTransition
}

private fun <T> BookDocumentVisibleItemLayout.resolveViewerItem(
    items: List<BookDocumentViewerItem<T>>,
): BookDocumentViewerItem<T>? =
    items.getOrNull(index)?.takeIf { it.key == key }
        ?: items.firstOrNull { it.key == key }

internal fun <T> List<BookDocumentViewerItem<T>>.indexOfPosition(
    sectionKey: String,
    position: BookDocumentPosition,
): Int = indexOfFirst { item ->
    item is BookDocumentViewerItem.Block &&
        item.section.key == sectionKey &&
        item.content.block.id == position.blockId
}

internal fun blockScrollOffset(
    itemSize: Int,
    blockLength: Int,
    offsetWithinBlock: Int,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
): Int {
    if (itemSize <= 0 || blockLength <= 0) return 0
    val offsetWithinItem =
        (itemSize * offsetWithinBlock.coerceIn(0, blockLength).toFloat() / blockLength).roundToInt()
    val viewportAnchor = (viewportStartOffset + viewportEndOffset) / 2
    return offsetWithinItem - viewportAnchor
}

internal fun bookDocumentScrollOffset(
    document: PreparedBookDocument,
    position: BookDocumentPosition,
    itemSize: Int,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
): Int {
    if (document.document.logicalOffset(position) == 0) return 0
    val block = document.block(position.blockId)?.block ?: return 0
    return blockScrollOffset(
        itemSize = itemSize,
        blockLength = block.logicalLength,
        offsetWithinBlock = position.offsetWithinBlock,
        viewportStartOffset = viewportStartOffset,
        viewportEndOffset = viewportEndOffset,
    )
}
