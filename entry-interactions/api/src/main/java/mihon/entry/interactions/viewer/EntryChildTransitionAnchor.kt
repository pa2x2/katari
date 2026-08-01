package mihon.entry.interactions.viewer

/**
 * Resolves the actionable entry-child boundary at a scrolling viewer's reading anchor.
 *
 * A centered boundary wins while the viewport can move. At a hard edge, the visible edge
 * boundary wins instead. The latter also covers content shorter than the viewport, where both
 * edges are visible and a terminal boundary must not hide an actionable unloaded boundary.
 */
fun <T> entryChildTransitionItemAtAnchor(
    centeredItem: T?,
    firstVisibleItem: T?,
    lastVisibleItem: T?,
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
    transitionOf: (T) -> EntryChildTransition<*>?,
    isActionable: (T, EntryChildTransition<*>) -> Boolean,
): T? {
    fun T?.actionableTransitionItem(): T? {
        val item = this ?: return null
        val transition = transitionOf(item) ?: return null
        return item.takeIf { isActionable(item, transition) }
    }

    val centeredTransition = centeredItem?.let(transitionOf)
    if (centeredTransition != null && (canScrollBackward || canScrollForward)) {
        return centeredItem.actionableTransitionItem()
    }

    val boundaryItems = when {
        !canScrollBackward && !canScrollForward -> listOf(firstVisibleItem, lastVisibleItem)
        !canScrollBackward -> listOf(firstVisibleItem)
        !canScrollForward -> listOf(lastVisibleItem)
        else -> emptyList()
    }
    return boundaryItems.firstNotNullOfOrNull { it.actionableTransitionItem() }
        ?: centeredItem.actionableTransitionItem()
}
