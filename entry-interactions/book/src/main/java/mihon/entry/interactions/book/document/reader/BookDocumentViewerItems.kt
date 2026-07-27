package mihon.entry.interactions.book.document.reader

import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import mihon.entry.interactions.viewer.EntryChildTransition
import mihon.entry.interactions.viewer.EntryChildWindow
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class BookDocumentSection<T>(
    val key: String,
    val owner: T,
    val document: PreparedBookDocument,
    val initialPosition: BookDocumentPosition,
    val resourceLoader: BookDocumentResourceLoader?,
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

    data class Loading<T>(
        val owner: T,
        override val key: String,
    ) : BookDocumentViewerItem<T>
}

internal fun <T, K> buildBookDocumentViewerItems(
    window: EntryChildWindow<T>,
    loaded: Map<K, BookDocumentSection<T>>,
    keyOf: (T) -> K,
): List<BookDocumentViewerItem<T>> = buildList {
    window.previous?.let { previous ->
        addDocumentOrLoading(previous, loaded[keyOf(previous)], keyOf(previous))
    }
    add(window.previousTransition().toViewerItem(keyOf))
    addDocumentOrLoading(window.current, loaded[keyOf(window.current)], keyOf(window.current))
    add(window.nextTransition().toViewerItem(keyOf))
    window.next?.let { next ->
        addDocumentOrLoading(next, loaded[keyOf(next)], keyOf(next))
    }
}

private fun <T> MutableList<BookDocumentViewerItem<T>>.addDocumentOrLoading(
    owner: T,
    section: BookDocumentSection<T>?,
    ownerKey: Any?,
) {
    if (section == null) {
        add(BookDocumentViewerItem.Loading(owner, "document-loading:$ownerKey"))
    } else {
        section.document.blocks.mapTo(this) { block -> BookDocumentViewerItem.Block(section, block) }
    }
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
