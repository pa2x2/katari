package mihon.entry.interactions.book.document.reader

import mihon.book.api.document.BookDocumentPosition
import kotlin.math.roundToInt

internal data class BookDocumentViewerVisualProgress<T>(
    val section: BookDocumentSection<T>,
    val progression: Float,
)

internal class BookDocumentVisualProgressTracker {
    private val chapterDistances = mutableMapOf<String, Int>()
    private val transitionEntryProgressions = mutableMapOf<String, Float>()

    fun recordChapterDistance(transitionKey: String, distance: Int) {
        if (distance > 0) chapterDistances[transitionKey] = distance
    }

    fun chapterDistance(transitionKey: String): Int? = chapterDistances[transitionKey]

    fun recordTransitionEntryProgression(transitionKey: String, progression: Float) {
        transitionEntryProgressions[transitionKey] = progression
    }

    fun transitionEntryProgression(transitionKey: String): Float? =
        transitionEntryProgressions[transitionKey]
}

/**
 * Maps the rendered chapter stream onto chapter-local visual progress.
 *
 * Resume locations deliberately stop at transition rows. Visual progress instead keeps the
 * preceding chapter active until the following chapter reaches the viewport start, so the final
 * viewport and the transition row remain part of the distance represented by the indicator.
 */
internal fun <T> bookDocumentVisualProgress(
    items: List<BookDocumentViewerItem<T>>,
    visibleItems: List<BookDocumentVisibleItemLayout>,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
    canScrollForward: Boolean,
    tracker: BookDocumentVisualProgressTracker,
): BookDocumentViewerVisualProgress<T>? {
    val topVisibleItem = visibleItems
        .firstOrNull { viewportStartOffset in it.offset until it.endOffset }
        ?: visibleItems.minByOrNull { layout -> distanceTo(layout, viewportStartOffset) }
        ?: return null
    val (topItemIndex, topItem) = topVisibleItem.resolveViewerItem(items) ?: return null
    val section = when (topItem) {
        is BookDocumentViewerItem.Block -> topItem.section
        is BookDocumentViewerItem.Transition ->
            (items.getOrNull(topItemIndex - 1) as? BookDocumentViewerItem.Block)?.section
                ?: (items.getOrNull(topItemIndex + 1) as? BookDocumentViewerItem.Block)?.section
                ?: return null
    }
    val visibleBlocks = visibleItems.mapNotNull { layout ->
        val item = layout.resolveViewerItem(items)?.second as? BookDocumentViewerItem.Block
            ?: return@mapNotNull null
        if (item.section.key != section.key) return@mapNotNull null
        VisibleDocumentBlock(item, layout)
    }
    val document = section.document.document
    val firstBlock = document.blocks.firstOrNull()?.id?.let { firstBlockId ->
        visibleBlocks.firstOrNull { it.item.content.id == firstBlockId }
    }
    val nextTransition = visibleItems.firstNotNullOfOrNull { layout ->
        val (index, item) = layout.resolveViewerItem(items) ?: return@firstNotNullOfOrNull null
        if (item !is BookDocumentViewerItem.Transition) return@firstNotNullOfOrNull null
        val precedingSection = (items.getOrNull(index - 1) as? BookDocumentViewerItem.Block)?.section
        VisibleTransition(layout, item).takeIf { precedingSection?.key == section.key }
    }
    val isTerminalTransition = nextTransition != null && nextTransition.item.transition.to == null
    val terminalBoundaryReached = !canScrollForward && isTerminalTransition
    val measuredChapterDistance = if (firstBlock != null && nextTransition != null) {
        val distanceFromStart = (viewportStartOffset - firstBlock.layout.offset).coerceAtLeast(0)
        val distanceToEnd = if (isTerminalTransition) {
            nextTransition.layout.endOffset - viewportEndOffset
        } else {
            nextTransition.layout.endOffset - viewportStartOffset
        }.coerceAtLeast(0)
        (distanceFromStart + distanceToEnd).coerceAtLeast(1).also { chapterDistance ->
            tracker.recordChapterDistance(nextTransition.item.key, chapterDistance)
        }
    } else {
        null
    }
    val progression = when {
        terminalBoundaryReached -> 1f
        firstBlock != null && firstBlock.layout.offset == viewportStartOffset -> 0f
        firstBlock != null && nextTransition != null && measuredChapterDistance != null -> {
            val distanceFromStart = (viewportStartOffset - firstBlock.layout.offset).coerceAtLeast(0)
            distanceFromStart.toFloat().div(measuredChapterDistance)
        }
        nextTransition != null -> transitionAwareEndProgress(
            section = section,
            visibleBlocks = visibleBlocks,
            transition = nextTransition,
            viewportStartOffset = viewportStartOffset,
            viewportEndOffset = viewportEndOffset,
            isTerminal = isTerminalTransition,
            tracker = tracker,
        )
        visibleBlocks.isNotEmpty() -> progressionAtViewportOffset(
            section = section,
            visibleBlocks = visibleBlocks,
            viewportOffset = viewportStartOffset,
        )
        else -> return null
    }
    return BookDocumentViewerVisualProgress(section, progression.coerceIn(0f, 1f))
}

private fun <T> transitionAwareEndProgress(
    section: BookDocumentSection<T>,
    visibleBlocks: List<VisibleDocumentBlock<T>>,
    transition: VisibleTransition<T>,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
    isTerminal: Boolean,
    tracker: BookDocumentVisualProgressTracker,
): Float {
    val measuredChapterDistance = tracker.chapterDistance(transition.item.key)
    if (measuredChapterDistance != null && !isTerminal) {
        val distanceToNextStart = (transition.layout.endOffset - viewportStartOffset).coerceAtLeast(0)
        return (measuredChapterDistance - distanceToNextStart).toFloat()
            .div(measuredChapterDistance)
            .coerceIn(0f, 1f)
    }

    val distanceSinceTransitionEntered = (viewportEndOffset - transition.layout.offset).coerceAtLeast(0)
    val entryViewportOffset = viewportStartOffset - distanceSinceTransitionEntered
    val observedEntryProgression = if (visibleBlocks.isNotEmpty()) {
        progressionAtViewportOffset(section, visibleBlocks, entryViewportOffset)
    } else {
        null
    }
    if (observedEntryProgression != null) {
        tracker.recordTransitionEntryProgression(transition.item.key, observedEntryProgression)
    }
    val entryProgression = observedEntryProgression
        ?: tracker.transitionEntryProgression(transition.item.key)
        ?: section.document.document.blocks.last().let { lastBlock ->
            section.document.document.progressionAt(BookDocumentPosition(lastBlock.id, 0))
        }
    val transitionDistance = if (isTerminal) {
        transition.layout.size
    } else {
        viewportEndOffset - viewportStartOffset + transition.layout.size
    }.coerceAtLeast(1)
    val transitionFraction = distanceSinceTransitionEntered.toFloat()
        .div(transitionDistance)
        .coerceIn(0f, 1f)
    return entryProgression + (1f - entryProgression) * transitionFraction
}

private fun <T> progressionAtViewportOffset(
    section: BookDocumentSection<T>,
    visibleBlocks: List<VisibleDocumentBlock<T>>,
    viewportOffset: Int,
): Float {
    val visibleBlock = visibleBlocks.firstOrNull { viewportOffset in it.layout.offset until it.layout.endOffset }
        ?: visibleBlocks.minBy { block -> distanceTo(block.layout, viewportOffset) }
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

private data class VisibleTransition<T>(
    val layout: BookDocumentVisibleItemLayout,
    val item: BookDocumentViewerItem.Transition<T>,
)

private val BookDocumentVisibleItemLayout.endOffset: Int
    get() = offset + size

private fun <T> BookDocumentVisibleItemLayout.resolveViewerItem(
    items: List<BookDocumentViewerItem<T>>,
): Pair<Int, BookDocumentViewerItem<T>>? {
    items.getOrNull(index)?.takeIf { it.key == key }?.let { return index to it }
    val resolvedIndex = items.indexOfFirst { it.key == key }
    return items.getOrNull(resolvedIndex)?.let { resolvedIndex to it }
}

private fun distanceTo(layout: BookDocumentVisibleItemLayout, offset: Int): Int = when {
    offset < layout.offset -> layout.offset - offset
    offset >= layout.endOffset -> offset - layout.endOffset
    else -> 0
}
