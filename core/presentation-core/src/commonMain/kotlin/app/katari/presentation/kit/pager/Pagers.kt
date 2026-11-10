package app.katari.presentation.kit.pager

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.HorizontalPager as FoundationHorizontalPager
import androidx.compose.foundation.pager.VerticalPager as FoundationVerticalPager

/**
 * Pages side by side, swiped through by touch or turned one at a time by a wheel or a touchpad swipe (see [pageWheel]).
 * [userScrollEnabled] covers both.
 *
 * [onUserTurn] is told the page a person's drag, wheel or touchpad swipe left the pager on, but not a page [state] was
 * moved to from code, so a screen that keeps the selected page itself, such as Browse's feed switcher, can tell a turn
 * it has to follow from one it made.
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
    onUserTurn: ((page: Int) -> Unit)? = null,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    val turns = remember(state) { WheelTurns() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val currentOnUserTurn by rememberUpdatedState(onUserTurn)
    if (onUserTurn != null) ReportDraggedTurns(state, onUserTurn)
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
        WheelTurnedPage(
            state = state,
            turns = turns,
            orientation = Orientation.Horizontal,
            reverse = rtl,
            enabled = userScrollEnabled,
            onTurned = { currentOnUserTurn?.invoke(it) },
        ) {
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
        WheelTurnedPage(
            state = state,
            turns = turns,
            orientation = Orientation.Vertical,
            reverse = false,
            enabled = userScrollEnabled,
            onTurned = {},
        ) {
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
    onTurned: (page: Int) -> Unit,
    content: @Composable PagerScope.() -> Unit,
) {
    Box(
        modifier = if (enabled) Modifier.pageWheel(state, turns, orientation, reverse, onTurned) else Modifier,
        propagateMinConstraints = true,
    ) {
        content()
    }
}

/**
 * Hands [onTurn] the page a drag left the pager on once it comes to rest. The pager also comes to rest after being
 * moved from code or clamped to fewer pages, so only a rest that follows a drag counts.
 */
@Composable
private fun ReportDraggedTurns(state: PagerState, onTurn: (page: Int) -> Unit) {
    val currentOnTurn by rememberUpdatedState(onTurn)
    LaunchedEffect(state) {
        var dragged = false
        launch {
            state.interactionSource.interactions.collect {
                if (it is DragInteraction.Start) dragged = true
            }
        }
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .collect {
                if (!dragged) return@collect
                dragged = false
                currentOnTurn(state.currentPage)
            }
    }
}
