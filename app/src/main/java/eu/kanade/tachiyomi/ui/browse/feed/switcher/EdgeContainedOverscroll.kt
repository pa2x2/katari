package eu.kanade.tachiyomi.ui.browse.feed.switcher

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Velocity

/**
 * Keeps a pager's drags past its first or last page to itself. Installed with `Modifier.nestedScroll` around the pager,
 * it withholds what the pager couldn't scroll from outer scrollables, such as the Browse tab pager. Passed as the
 * pager's overscroll effect, it reports that withheld delta as unconsumed, so the edge stretches under the finger.
 *
 * Both roles are needed because a scrollable only shows overscroll for what its nested scroll parents left over.
 */
internal class EdgeContainedOverscroll(
    private val overscroll: OverscrollEffect?,
) : OverscrollEffect, NestedScrollConnection {
    private var withheldScroll = Offset.Zero
    private var withheldVelocity = Velocity.Zero

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        withheldScroll += available
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        withheldVelocity += available
        return available
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        val scrollWithinPager: (Offset) -> Offset = { available ->
            withheldScroll = Offset.Zero
            performScroll(available) - withheldScroll
        }
        return overscroll?.applyToScroll(delta, source, scrollWithinPager) ?: scrollWithinPager(delta)
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        val flingWithinPager: suspend (Velocity) -> Velocity = { available ->
            withheldVelocity = Velocity.Zero
            performFling(available) - withheldVelocity
        }
        overscroll?.applyToFling(velocity, flingWithinPager) ?: flingWithinPager(velocity)
    }

    override val isInProgress: Boolean
        get() = overscroll?.isInProgress == true

    override val node: DelegatableNode = overscroll?.node ?: object : Modifier.Node() {}
}

@Composable
internal fun rememberEdgeContainedOverscroll(): EdgeContainedOverscroll {
    val overscroll = rememberOverscrollEffect()
    return remember(overscroll) { EdgeContainedOverscroll(overscroll) }
}
