package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import app.katari.entry.viewer.chrome.ui.tap.ReaderTapAction
import app.katari.entry.viewer.chrome.ui.tap.readerTapAction
import app.katari.presentation.kit.layout.PageStrip
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.presentation.reader.ChapterTransitionPlacement
import eu.kanade.presentation.reader.ViewerChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.ui.reader.viewer.gesture.detectReaderTaps
import eu.kanade.tachiyomi.ui.reader.viewer.gesture.mouseDrags
import eu.kanade.tachiyomi.ui.reader.viewer.gesture.quickZoomGesture
import eu.kanade.tachiyomi.ui.reader.viewer.key
import eu.kanade.tachiyomi.ui.reader.viewer.tap.FollowTapZoneSettings
import eu.kanade.tachiyomi.ui.reader.viewer.tap.ReaderTapZonesOverlay
import eu.kanade.tachiyomi.ui.reader.viewer.tap.hidesTapZones
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The long strip on screen: its slots in a lazy column at the strip's width, magnified as [StripViewer.zoom] says. A
 * tap or click goes to the tap zones of the whole strip, a double tap zooms, a long press or right click opens the
 * actions of the page under it, and the wheel and drags scroll, a mouse's as a finger's ([stripWheel] adds Ctrl to
 * zoom, and [stripTouchpadPinch] zooms with a touchpad pinch).
 */
@Composable
internal fun StripViewer.Content(modifier: Modifier = Modifier, tapZonesHeld: Boolean = false) {
    val viewer = this
    val scope = rememberCoroutineScope()
    DisposableEffect(viewer, scope) {
        composeScope = scope
        onDispose { if (composeScope === scope) composeScope = null }
    }

    val bindings = settings.bindings
    zoom.zoomOutDisabled = bindings.webtoonDisableZoomOut.state.collectAsState().value.effectiveValue
    val doubleTapZoom = bindings.webtoonDoubleTapZoom.state.collectAsState().value.effectiveValue
    val sidePadding = bindings.webtoonSidePadding.state.collectAsState().value.effectiveValue
    val tapMode = bindings.webtoonNavigationMode.state.collectAsState().value.effectiveValue
    val tapInversion = bindings.webtoonNavigationInverted.state.collectAsState().value.effectiveValue.ordinal
    FollowTapZoneSettings(tapZones, tapMode, tapInversion)
    ReadNeighbourBackdrops()

    val state = listState
    LaunchedEffect(state) { snapshotFlow { state.layoutInfo }.collect(::onLayout) }
    LaunchedEffect(state) {
        state.interactionSource.interactions.collect { if (it is DragInteraction.Start) onDirectManipulation() }
    }
    val menuHider = remember(viewer) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                onScrolledBy(consumed.y * zoom.scale)
                return Offset.Zero
            }
        }
    }

    val haptics = LocalHapticFeedback.current
    val fling = ScrollableDefaults.flingBehavior()
    val density = LocalDensity.current
    val root = remember(viewer) { StripRoot() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onGloballyPositioned {
                root.coordinates = it
                viewportHeight = it.size.height
            }
            .hidesTapZones(tapZones)
            .quickZoomGesture(
                key = zoom,
                enabled = { settings.doubleTapZoom },
                onStart = { onDirectManipulation() },
                onZoom = { factor, focus -> zoom.zoomBy(factor, focus, ::scrollBy) },
            )
            .stripZoomGestures(zoom, scope, ::scrollBy, onZoomStart = ::onDirectManipulation)
            .stripWheel(zoom, ::scrollBy)
            .stripTouchpadPinch(zoom, ::scrollBy, onZoomStart = ::onDirectManipulation)
            .mouseDrags(
                key = viewer,
                onStart = ::onDirectManipulation,
                onDrag = { move ->
                    scrollBy(-move.y / zoom.scale)
                    onScrolledBy(move.y)
                },
                onEnd = { velocity ->
                    scope.launch { state.scroll { with(fling) { performFling(-velocity.y / zoom.scale) } } }
                },
            )
            .pointerInput(viewer, doubleTapZoom) {
                detectReaderTaps(
                    onTap = { position -> onTap(position, size, root.coordinates) },
                    onDoubleTap = if (doubleTapZoom) {
                        { position -> scope.launch { zoom.toggle(position, DOUBLE_TAP_ZOOM_MILLIS, ::scrollBy) } }
                    } else {
                        null
                    },
                    onLongPress = { position ->
                        val page = pageAt(position) ?: return@detectReaderTaps
                        if (onPageLongPress(page)) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onSecondaryClick = { position ->
                        val page = pageAt(position) ?: return@detectReaderTaps
                        val window = root.coordinates?.takeIf { it.isAttached }?.localToWindow(position)
                            ?: return@detectReaderTaps
                        onPageSecondaryClick(page, window)
                    },
                )
            },
    ) {
        val width = constraints.maxWidth
        val maxStripWidth = with(density) { KatariTheme.sizes.stripWidth.roundToPx() }
        val inset = ((width - maxStripWidth) / 2).coerceAtLeast(0) + width * sidePadding / 100
        zoom.setViewport(width.toFloat(), inset.toFloat())
        val insetDp = with(density) { inset.toDp() }
        val pageWidth = width - 2 * with(density) { insetDp.roundToPx() }
        val placeholderHeight = constraints.maxHeight

        PageStrip(
            state = state,
            sideInset = insetDp,
            modifier = Modifier
                .paintedBehind(viewer)
                .zoomedBy(zoom)
                .followedBy(slotAnchor, state)
                .nestedScroll(menuHider),
        ) {
            items(items, key = { it.key }, contentType = { it::class }) { item ->
                when (item) {
                    is ReaderViewerItem.Page -> StripPage(
                        page = item.page,
                        slotKey = item.key,
                        viewer = viewer,
                        width = pageWidth,
                        placeholderHeight = placeholderHeight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (continuous) KatariTheme.sizes.none else KatariTheme.spacing.l),
                    )
                    is ReaderViewerItem.Transition -> Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.widthIn(max = KatariTheme.sizes.readableWidth).padding(KatariTheme.spacing.xxl)) {
                            ViewerChapterTransition(
                                transition = item.transition,
                                downloadManager = downloadManager,
                                displayMode = bindings.chapterTransition.state.collectAsState().value.effectiveValue,
                                placement = ChapterTransitionPlacement.WEBTOON,
                                onRetry = ::requestTransitionChapterLoad,
                            )
                        }
                    }
                }
            }
        }
        ReaderTapZonesOverlay(tapZones, tapMode, vertical = true, tapInversion, tapZonesHeld, Modifier.fillMaxSize())
    }
}

/**
 * Lays the strip out [StripZoom.scale] times as tall as the screen and magnifies it from its top-left corner to fill
 * the screen, moved across by [StripZoom.panX].
 */
private fun Modifier.zoomedBy(zoom: StripZoom): Modifier = layout { measurable, constraints ->
    val height = (constraints.maxHeight / zoom.scale).roundToInt()
    val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.placeWithLayer(0, 0) {
            transformOrigin = TransformOrigin(0f, 0f)
            scaleX = zoom.scale
            scaleY = zoom.scale
            translationX = -zoom.panX
        }
    }
}

/** Where the strip sits in the window, read by taps for the text features. */
private class StripRoot {
    var coordinates: LayoutCoordinates? = null
}

/** The page under [position], a point of the screen. */
private fun StripViewer.pageAt(position: Offset): ReaderPage? {
    val stripY = position.y / zoom.scale
    val info = listState.layoutInfo
    val slot = info.visibleItemsInfo.firstOrNull { stripY >= it.offset && stripY < it.offset + it.size }
    return (slot?.let { items.getOrNull(it.index) } as? ReaderViewerItem.Page)?.page
}

/** Uses a tap at [position] in the strip, [size] large: for the text features if they take it, else its tap zone. */
private fun StripViewer.onTap(position: Offset, size: IntSize, root: LayoutCoordinates?) {
    val window = root?.takeIf { it.isAttached }?.localToWindow(position)
    if (window != null && onTextTap(window.x, window.y)) return
    if (size.width <= 0 || size.height <= 0) return
    val action = readerTapAction(
        mode = settings.tapZones,
        vertical = true,
        inversion = settings.tapInversion.ordinal,
        x = position.x / size.width,
        y = position.y / size.height,
    )
    when (action) {
        ReaderTapAction.MENU -> toggleMenu()
        ReaderTapAction.NEXT, ReaderTapAction.RIGHT -> scrollDown()
        ReaderTapAction.PREV, ReaderTapAction.LEFT -> scrollUp()
    }
}

private const val DOUBLE_TAP_ZOOM_MILLIS = 200
