package mihon.presentation.kit.pager

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager as FoundationHorizontalPager
import androidx.compose.foundation.pager.VerticalPager as FoundationVerticalPager

/**
 * Pages side by side, swiped through by touch or turned one at a time by a wheel or a touchpad swipe (see [pageWheel]).
 * [userScrollEnabled] covers both.
 */
@Composable
fun HorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    key: ((index: Int) -> Any)? = null,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    val turns = remember(state) { WheelTurns() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    FoundationHorizontalPager(
        state = state,
        modifier = modifier,
        pageSpacing = pageSpacing,
        userScrollEnabled = userScrollEnabled,
        beyondViewportPageCount = beyondViewportPageCount,
        verticalAlignment = verticalAlignment,
        key = key,
        overscrollEffect = overscrollEffect,
    ) { page ->
        WheelTurnedPage(state, turns, Orientation.Horizontal, reverse = rtl, enabled = userScrollEnabled) {
            pageContent(page)
        }
    }
}

/**
 * Pages one above another, swiped through by touch or turned one at a time by a wheel or a touchpad swipe (see
 * [pageWheel]). [userScrollEnabled] covers both.
 */
@Composable
fun VerticalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    key: ((index: Int) -> Any)? = null,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    val turns = remember(state) { WheelTurns() }
    FoundationVerticalPager(
        state = state,
        modifier = modifier,
        userScrollEnabled = userScrollEnabled,
        beyondViewportPageCount = beyondViewportPageCount,
        key = key,
    ) { page ->
        WheelTurnedPage(state, turns, Orientation.Vertical, reverse = false, enabled = userScrollEnabled) {
            pageContent(page)
        }
    }
}

@Composable
private fun PagerScope.WheelTurnedPage(
    state: PagerState,
    turns: WheelTurns,
    orientation: Orientation,
    reverse: Boolean,
    enabled: Boolean,
    content: @Composable PagerScope.() -> Unit,
) {
    Box(
        modifier = if (enabled) Modifier.pageWheel(state, turns, orientation, reverse) else Modifier,
        propagateMinConstraints = true,
    ) {
        content()
    }
}
