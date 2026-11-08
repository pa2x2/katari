package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.reader.viewer.gesture.mouseDrags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Mouse drags that move [state]'s slots as a finger does, along the pager's axis ([vertical] or not), which the
 * foundation's pager does only for touch. Let go, the pager settles on the next or previous slot when the mouse was
 * thrown, as a fling turns the page, and otherwise on the one showing most; [scope] runs that settling. Drags begun
 * while [enabled] answers false are left alone.
 */
internal fun Modifier.pagerMouseDrags(
    state: PagerState,
    vertical: Boolean,
    scope: CoroutineScope,
    enabled: () -> Boolean = { true },
): Modifier {
    var from = 0
    return mouseDrags(
        key = state,
        onStart = { from = state.currentPage },
        onDrag = { move -> state.dispatchRawDelta(-(if (vertical) move.y else move.x)) },
        onEnd = { velocity ->
            val target = state.thrownTarget(from, if (vertical) velocity.y else velocity.x)
            scope.launch { state.animateScrollToPage(target) }
        },
        enabled = enabled,
    )
}

/** The slot a drag that began on slot [from] settles on, let go at [velocity] pixels a second along the pager. */
private fun PagerState.thrownTarget(from: Int, velocity: Float): Int {
    val threshold = layoutInfo.pageSize * THROW_PAGES_PER_SECOND
    val target = when {
        velocity <= -threshold -> from + 1
        velocity >= threshold -> from - 1
        else -> currentPage
    }
    return target.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
}

// A mouse let go faster than this many slots a second throws the page on to the next one
private const val THROW_PAGES_PER_SECOND = 0.5f
