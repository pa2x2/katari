package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import app.katari.entry.viewer.chrome.ui.tap.ReaderTapAction
import app.katari.entry.viewer.chrome.ui.tap.readerTapAction
import app.katari.presentation.kit.layout.PageSpread
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.window.currentWindowLayout
import eu.kanade.presentation.reader.ChapterTransitionPlacement
import eu.kanade.presentation.reader.ViewerChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.ui.reader.viewer.gesture.detectReaderTaps
import eu.kanade.tachiyomi.ui.reader.viewer.key
import eu.kanade.tachiyomi.ui.reader.viewer.tap.FollowTapZoneSettings
import eu.kanade.tachiyomi.ui.reader.viewer.tap.ReaderTapZonesOverlay
import eu.kanade.tachiyomi.ui.reader.viewer.tap.hidesTapZones
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * The paged viewer on screen: the slots in a pager turning [PagedViewer.direction], each a page, two facing pages or a
 * chapter transition. A tap or click anywhere goes to the tap zones of the whole pager, a double tap zooms the page
 * under it, a long press or right click opens its actions, the wheel scrolls and turns pages ([pagerWheel]), and a
 * mouse drags pages as a finger does.
 */
@Composable
internal fun PagedViewer.Content(modifier: Modifier = Modifier, tapZonesHeld: Boolean = false) {
    val viewer = this
    val scope = rememberCoroutineScope()
    DisposableEffect(viewer, scope) {
        composeScope = scope
        onDispose { if (composeScope === scope) composeScope = null }
    }

    val usesTwoPanes = currentWindowLayout().usesTwoPanes
    val pageLayout = settings.bindings.pagerPageLayout.state.collectAsState().value.effectiveValue
    LaunchedEffect(usesTwoPanes, pageLayout) { onSpreadConditionsChanged(usesTwoPanes) }
    val split = settings.bindings.dualPageSplitPaged.state.collectAsState().value.effectiveValue
    LaunchedEffect(split) { if (!split) onSplitTurnedOff() }
    LaunchedEffect(pagerState) { snapshotFlow { pagerState.settledPage }.collect(::onPageSettled) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }.filter { !it }.collect { onIdle() }
    }

    val tapMode = settings.bindings.pagerNavigationMode.state.collectAsState().value.effectiveValue
    val tapInversion = settings.bindings.pagerNavigationInverted.state.collectAsState().value.effectiveValue.ordinal
    FollowTapZoneSettings(tapZones, tapMode, tapInversion)

    val placements = remember(viewer) { PagerPlacements() }
    val haptics = LocalHapticFeedback.current

    fun pageAt(position: Offset): Pair<ReaderPage, Rect>? {
        val root = placements.root?.takeIf { it.isAttached } ?: return null
        val slot = currentSlot as? ReaderViewerItem.Page ?: return null
        val bounds = listOfNotNull(slot.page, slot.facing).mapNotNull { page ->
            placements.pages[page]?.takeIf { it.isAttached }?.let { page to root.localBoundingBoxOf(it) }
        }
        if (bounds.size < 2) return bounds.firstOrNull()
        // In the gap between facing pages, the nearer one
        val (left, right) = bounds.sortedBy { it.second.left }
        return if (position.x < (left.second.right + right.second.left) / 2f) left else right
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                placements.root = it
                landscape = it.size.width > it.size.height
            }
            .hidesTapZones(tapZones)
            .pagerWheel(viewer) { pageAt(it) }
            .pagerMouseDrags(pagerState, vertical, scope)
            .pointerInput(viewer) {
                detectReaderTaps(
                    onTap = { position -> onTap(position, size, placements.root) },
                    onDoubleTap = { position ->
                        val (page, bounds) = pageAt(position) ?: return@detectReaderTaps
                        val zoom = zoomOf(page) ?: return@detectReaderTaps
                        scope.launch { zoom.toggleZoom(position - bounds.topLeft, settings.doubleTapMillis) }
                    },
                    onLongPress = { position ->
                        val page = pageAt(position)?.first ?: return@detectReaderTaps
                        if (onPageLongPress(page)) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onSecondaryClick = { position ->
                        val page = pageAt(position)?.first ?: return@detectReaderTaps
                        val window = placements.root?.takeIf { it.isAttached }?.localToWindow(position)
                            ?: return@detectReaderTaps
                        onPageSecondaryClick(page, window)
                    },
                )
            },
    ) {
        val slot: @Composable (Int) -> Unit = { index ->
            items.getOrNull(index)?.let { item ->
                Slot(item, onPagePlaced = { page, coordinates -> placements.pages[page] = coordinates })
            }
        }
        if (vertical) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { items.getOrNull(it)?.key ?: it },
            ) { slot(it) }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { items.getOrNull(it)?.key ?: it },
            ) { slot(it) }
        }
        ReaderTapZonesOverlay(tapZones, tapMode, vertical, tapInversion, tapZonesHeld, Modifier.fillMaxSize())
    }
}

/** Uses a tap at [position] in the pager, [size] large: for the text features if they take it, else its tap zone. */
private fun PagedViewer.onTap(position: Offset, size: IntSize, root: LayoutCoordinates?) {
    val window = root?.takeIf { it.isAttached }?.localToWindow(position)
    if (window != null && onTextTap(window.x, window.y)) return
    if (size.width <= 0 || size.height <= 0) return
    val action = readerTapAction(
        mode = settings.tapZones,
        vertical = vertical,
        inversion = settings.tapInversion.ordinal,
        x = position.x / size.width,
        y = position.y / size.height,
    )
    when (action) {
        ReaderTapAction.MENU -> toggleMenu()
        ReaderTapAction.NEXT -> moveToNext()
        ReaderTapAction.PREV -> moveToPrevious()
        ReaderTapAction.RIGHT -> moveRight()
        ReaderTapAction.LEFT -> moveLeft()
    }
}

@Composable
private fun PagedViewer.Slot(item: ReaderViewerItem, onPagePlaced: (ReaderPage, LayoutCoordinates) -> Unit) {
    when (item) {
        is ReaderViewerItem.Page -> {
            val facing = item.facing
            if (facing == null) {
                PagerPage(
                    page = item.page,
                    viewer = this,
                    narrowedAgainst = null,
                    onPlaced = { onPagePlaced(item.page, it) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                FacingPages(item, facing, onPagePlaced)
            }
        }
        is ReaderViewerItem.Transition -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.widthIn(max = KatariTheme.sizes.readableWidth).padding(KatariTheme.spacing.xxl)) {
                ViewerChapterTransition(
                    transition = item.transition,
                    downloadManager = downloadManager,
                    displayMode = settings.bindings.chapterTransition.state.collectAsState().value.effectiveValue,
                    placement = ChapterTransitionPlacement.PAGER,
                    onRetry = ::requestTransitionChapterLoad,
                )
            }
        }
    }
}

/**
 * Two facing pages, the earlier one on the reading direction's side. While the scale type fits whole pages, each is
 * as wide as its image at the slot's height and meets the other at the split, as a printed spread does. Each page's
 * background fills its whole side of the split.
 */
@Composable
private fun PagedViewer.FacingPages(
    item: ReaderViewerItem.Page,
    facing: ReaderPage,
    onPagePlaced: (ReaderPage, LayoutCoordinates) -> Unit,
) {
    val scaleType = PageScaleType.fromSetting(
        settings.bindings.imageScaleType.state.collectAsState().value.effectiveValue,
    )
    val narrow = scaleType == PageScaleType.FitScreen
    val rightToLeft = direction == PagerDirection.RightToLeft
    val (leftPage, rightPage) = if (rightToLeft) facing to item.page else item.page to facing
    val spread = remember(item) { SpreadPlacements() }
    DisposableEffect(item, spread) {
        registerSpread(item.page, spread)
        onDispose { unregisterSpread(item.page, spread) }
    }
    PageSpread(Modifier.fillMaxSize().onGloballyPositioned { spread.spread = it }) {
        Pages(
            left = {
                PagerPage(
                    page = leftPage,
                    viewer = this@FacingPages,
                    narrowedAgainst = if (narrow) AbsoluteAlignment.CenterRight else null,
                    onPlaced = {
                        onPagePlaced(leftPage, it)
                        spread.left = it
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            },
            right = {
                PagerPage(
                    page = rightPage,
                    viewer = this@FacingPages,
                    narrowedAgainst = if (narrow) AbsoluteAlignment.CenterLeft else null,
                    onPlaced = {
                        onPagePlaced(rightPage, it)
                        spread.right = it
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            },
            modifier = Modifier.fillMaxSize(),
            meetAtSplit = true,
        )
    }
}

/** Where the root of the pager and each page shown sit, read by taps. */
private class PagerPlacements {
    var root: LayoutCoordinates? = null
    val pages = mutableMapOf<ReaderPage, LayoutCoordinates>()
}

/** Where two facing pages sit in their spread, to report the middle of each as a fraction of its width. */
internal class SpreadPlacements {
    var spread: LayoutCoordinates? = null
        set(value) {
            field = value
            update()
        }
    var left: LayoutCoordinates? = null
        set(value) {
            field = value
            update()
        }
    var right: LayoutCoordinates? = null
        set(value) {
            field = value
            update()
        }
    var middles by mutableStateOf<Pair<Float, Float>?>(null)
        private set

    private fun update() {
        val spread = spread?.takeIf { it.isAttached && it.size.width > 0 } ?: return
        val left = left?.takeIf { it.isAttached } ?: return
        val right = right?.takeIf { it.isAttached } ?: return
        val width = spread.size.width.toFloat()
        middles = spread.localBoundingBoxOf(left).center.x / width to spread.localBoundingBoxOf(right).center.x / width
    }
}
