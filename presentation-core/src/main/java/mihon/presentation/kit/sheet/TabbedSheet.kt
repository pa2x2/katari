package mihon.presentation.kit.sheet

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.launch
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.navigation.Tab
import mihon.presentation.kit.navigation.TabRow
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme

/**
 * A sheet of settings split into tabs, such as a library's filter, sort and display options. Each page scrolls on
 * its own; the sheet keeps the height of the tallest page it has shown, so nothing moves under the finger when a
 * page gets shorter. [menu] fills an overflow menu beside the tabs and receives a callback that closes it.
 */
@Composable
fun TabbedSheet(
    onDismissRequest: () -> Unit,
    tabTitles: List<String>,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState { tabTitles.size },
    menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
    backdrop: SheetBackdrop = SheetBackdrop.Dimmed,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable ColumnScope.(page: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerScrollLock = remember { TabbedSheetPagerScrollLock() }
    val contentBringIntoViewSpec = LocalBringIntoViewSpec.current
    val density = LocalDensity.current
    var tallestPagerHeight by remember { mutableStateOf(Dp.Unspecified) }

    Sheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        capHeight = true,
        backdrop = backdrop,
        snackbarHostState = snackbarHostState,
    ) {
        Column {
            Row {
                TabRow(
                    modifier = Modifier.weight(1f),
                    container = SurfaceContainer.High,
                ) {
                    tabTitles.fastForEachIndexed { index, title ->
                        Tab(
                            title = title,
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.scrollToPage(index) } },
                            modifier = Modifier.focusProperties { canFocus = pagerState.currentPage == index },
                        )
                    }
                }
                menu?.let { OverflowMenu(content = it) }
            }
            CompositionLocalProvider(
                LocalTabbedSheetPagerScrollLock provides pagerScrollLock,
                LocalBringIntoViewSpec provides if (pagerScrollLock.isLocked) {
                    NoOpBringIntoViewSpec
                } else {
                    contentBringIntoViewSpec
                },
            ) {
                HorizontalPager(
                    modifier = Modifier
                        .heightIn(min = tallestPagerHeight)
                        .onSizeChanged { size ->
                            val height = with(density) { size.height.toDp() }
                            if (tallestPagerHeight == Dp.Unspecified || height > tallestPagerHeight) {
                                tallestPagerHeight = height
                            }
                        }
                        .animateContentSize(),
                    state = pagerState,
                    beyondViewportPageCount = (tabTitles.size - 1).coerceAtLeast(0),
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = !pagerScrollLock.isLocked,
                ) { page ->
                    // Keep normal focus relocation inside the page's vertical scroller.
                    CompositionLocalProvider(LocalBringIntoViewSpec provides contentBringIntoViewSpec) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(top = KatariTheme.spacing.s, bottom = KatariTheme.spacing.m),
                        ) {
                            content(page)
                        }
                    }
                }
            }
        }
    }
}

private object NoOpBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) = 0f
}
