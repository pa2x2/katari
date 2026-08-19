package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.Stable
import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.entry.interactions.viewer.EntryChildTransition
import mihon.entry.interactions.viewer.EntryChildWindow
import mihon.entry.interactions.viewer.entryChildTransitionItemAtAnchor
import kotlin.math.abs
import kotlin.math.roundToInt

@Stable
internal data class BookDocumentSection<T>(
    val key: String,
    val owner: T,
    val document: PreparedBookDocument,
    val initialPosition: BookDocumentPosition,
    val resourceLoader: BookPublicationResourceLoader?,
) {
    /**
     * The surrounding viewer window changes while a section stays loaded. Keep its block rows and
     * their stable keys with the prepared section, instead of allocating the full projection again
     * every time an adjacent chapter becomes current.
     */
    val viewerBlocks: List<BookDocumentViewerItem.Block<T>> by lazy(LazyThreadSafetyMode.NONE) {
        document.blocks.map { block -> BookDocumentViewerItem.Block(this, block) }
    }

    private val viewerBlockIndices by lazy(LazyThreadSafetyMode.NONE) {
        buildMap(document.blocks.size) {
            document.blocks.forEachIndexed { index, block -> put(block.id, index) }
        }
    }

    fun viewerBlockIndex(blockId: mihon.book.api.document.BookDocumentBlockId): Int =
        viewerBlockIndices[blockId] ?: -1

    init {
        require(key.isNotBlank()) { "document section key must not be blank" }
        require(document.document.contains(initialPosition)) { "initial position must belong to the section document" }
    }
}

@Stable
internal sealed interface BookDocumentViewerItem<T> {
    val key: String

    data class Block<T>(
        val section: BookDocumentSection<T>,
        val content: mihon.book.api.document.BookDocumentBlock,
    ) : BookDocumentViewerItem<T> {
        override val key = "document:${section.key}:${section.document.document.resourceId}:${content.id.value}"
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
): BookDocumentViewerDataset<T> = BookDocumentViewerDataset(
    previous = window.previous?.let { previous -> loaded[keyOf(previous)] },
    previousTransition = window.previousTransition().toViewerItem(keyOf),
    current = requireNotNull(loaded[keyOf(window.current)]) {
        "The current document section must be loaded"
    },
    nextTransition = window.nextTransition().toViewerItem(keyOf),
    next = window.next?.let { next -> loaded[keyOf(next)] },
)

/**
 * A virtual list over the retained BOOK window. Chapter changes only shift three section
 * references, so they must not allocate or compare a row for every paragraph in those sections.
 */
@Stable
internal class BookDocumentViewerDataset<T>(
    val previous: BookDocumentSection<T>?,
    val previousTransition: BookDocumentViewerItem.Transition<T>,
    val current: BookDocumentSection<T>,
    val nextTransition: BookDocumentViewerItem.Transition<T>,
    val next: BookDocumentSection<T>?,
) : AbstractList<BookDocumentViewerItem<T>>() {
    val identity = BookDocumentViewerDatasetIdentity(
        previous = previous?.let(System::identityHashCode),
        current = System.identityHashCode(current),
        next = next?.let(System::identityHashCode),
        previousTransitionKey = previousTransition.key,
        nextTransitionKey = nextTransition.key,
    )

    override val size = sectionItemCount(previous) + 1 + sectionItemCount(current) + 1 + sectionItemCount(next)

    override fun get(index: Int): BookDocumentViewerItem<T> {
        if (index !in indices) throw IndexOutOfBoundsException("Index: $index, size: $size")
        var offset = 0
        previous?.let { section ->
            if (index < offset + section.viewerBlocks.size) return section.viewerBlocks[index - offset]
            offset += section.viewerBlocks.size
        }
        if (index == offset) return previousTransition
        offset += 1
        if (index < offset + current.viewerBlocks.size) return current.viewerBlocks[index - offset]
        offset += current.viewerBlocks.size
        if (index == offset) return nextTransition
        offset += 1
        next?.let { section ->
            if (index < offset + section.viewerBlocks.size) return section.viewerBlocks[index - offset]
        }
        throw IndexOutOfBoundsException("Index: $index, size: $size")
    }

    fun indexOfSection(sectionKey: String): Int = sectionOffset(sectionKey)

    fun indexOfPosition(sectionKey: String, position: BookDocumentPosition): Int {
        val section = sections().firstOrNull { it.key == sectionKey } ?: return -1
        val blockIndex = section.viewerBlockIndex(position.blockId)
        return if (blockIndex < 0) -1 else sectionOffset(sectionKey) + blockIndex
    }

    fun indexOfKey(key: Any): Int {
        for (section in sections()) {
            val blockIndex = section.viewerBlocks.indexOfFirst { it.key == key }
            if (blockIndex >= 0) return sectionOffset(section.key) + blockIndex
        }
        return when (key) {
            previousTransition.key -> sectionItemCount(previous)
            nextTransition.key -> sectionItemCount(previous) + 1 + current.viewerBlocks.size
            else -> -1
        }
    }

    fun transitionKeysChangingDirection(other: BookDocumentViewerDataset<T>): Set<String> {
        val otherDirections = listOf(other.previousTransition, other.nextTransition)
            .associate { transition -> transition.key to transition.transition.direction }
        return buildSet(2) {
            listOf(previousTransition, nextTransition).forEach { transition ->
                val otherDirection = otherDirections[transition.key] ?: return@forEach
                if (transition.transition.direction != otherDirection) add(transition.key)
            }
        }
    }

    /**
     * A newly prepared next section only extends the dataset tail. Every existing item keeps its
     * index and stable key, so the lazy list can consume this update without interrupting a scroll.
     */
    fun isStablePrefixOf(other: BookDocumentViewerDataset<T>): Boolean =
        next == null &&
            other.next != null &&
            identity.previous == other.identity.previous &&
            identity.current == other.identity.current &&
            identity.previousTransitionKey == other.identity.previousTransitionKey &&
            identity.nextTransitionKey == other.identity.nextTransitionKey

    /**
     * Advancing into the already appended next section only removes content before that section
     * and adds its new forward boundary. Once the shared boundary is offscreen, every visible
     * block keeps its stable key and order, so the lazy list can adopt the new window mid-scroll.
     */
    fun advancesToLoadedNext(other: BookDocumentViewerDataset<T>): Boolean =
        next != null &&
            other.previous === current &&
            other.current === next &&
            identity.nextTransitionKey == other.identity.previousTransitionKey

    /**
     * Moving back into the already prepended previous section only removes content after that
     * section and adds its new backward boundary. Once the shared boundary is offscreen, every
     * visible block keeps its stable key and order, so the lazy list can adopt the new window
     * mid-scroll.
     */
    fun retreatsToLoadedPrevious(other: BookDocumentViewerDataset<T>): Boolean =
        previous != null &&
            other.current === previous &&
            other.next === current &&
            identity.previousTransitionKey == other.identity.nextTransitionKey

    private fun sectionOffset(sectionKey: String): Int = when (sectionKey) {
        previous?.key -> 0
        current.key -> sectionItemCount(previous) + 1
        next?.key -> sectionItemCount(previous) + 1 + current.viewerBlocks.size + 1
        else -> -1
    }

    private fun sections(): List<BookDocumentSection<T>> = listOfNotNull(previous, current, next)

    private fun sectionItemCount(section: BookDocumentSection<T>?) = section?.viewerBlocks?.size ?: 0
}

internal data class BookDocumentViewerDatasetIdentity(
    val previous: Int?,
    val current: Int,
    val next: Int?,
    val previousTransitionKey: String,
    val nextTransitionKey: String,
)

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
    items: BookDocumentViewerDataset<T>,
    visibleItems: List<BookDocumentVisibleItemLayout>,
    viewportStartOffset: Int,
): BookDocumentViewerDatasetAnchor? {
    visibleItems.forEach { layout ->
        val index = items.indexOfKey(layout.key)
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
    val topLayout = visibleItems.firstOrNull { it.offset == viewportStartOffset }
    val topItem = topLayout?.let { layout ->
        (
            items.getOrNull(layout.index)?.takeIf { it.key == layout.key }
                ?: items.firstOrNull { it.key == layout.key }
            ) as? BookDocumentViewerItem.Block
    }
    if (
        topItem != null &&
        topItem.content.id == topItem.section.document.document.blocks.firstOrNull()?.id
    ) {
        val position = BookDocumentPosition(topItem.content.id, 0)
        val progression = topItem.section.document.document.progressionAt(position)
        return BookDocumentViewerLocation(
            section = topItem.section,
            position = position,
            progression = progression,
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
        blockId = item.content.id,
        offsetWithinBlock = (item.content.logicalLength * fraction).roundToInt(),
    )
    val progression = item.section.document.document.progressionAt(position)
    return BookDocumentViewerLocation(
        section = item.section,
        position = position,
        progression = progression,
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
    val centeredItem = centeredLayout?.resolveViewerItem(items)
    val firstVisibleItem = visibleItems.firstOrNull()?.resolveViewerItem(items)
    val lastVisibleItem = visibleItems.lastOrNull()?.resolveViewerItem(items)
    return entryChildTransitionItemAtAnchor(
        centeredItem = centeredItem,
        firstVisibleItem = firstVisibleItem,
        lastVisibleItem = lastVisibleItem,
        canScrollBackward = canScrollBackward,
        canScrollForward = canScrollForward,
        transitionOf = { (it as? BookDocumentViewerItem.Transition)?.transition },
        isActionable = { _, transition ->
            transition.to == null || items.none { item ->
                item is BookDocumentViewerItem.Block && item.section.owner == transition.to
            }
        },
    )
        ?.let { it as? BookDocumentViewerItem.Transition }
        ?.transition
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
        item.content.id == position.blockId
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
    val block = document.block(position.blockId) ?: return 0
    return blockScrollOffset(
        itemSize = itemSize,
        blockLength = block.logicalLength,
        offsetWithinBlock = position.offsetWithinBlock,
        viewportStartOffset = viewportStartOffset,
        viewportEndOffset = viewportEndOffset,
    )
}
