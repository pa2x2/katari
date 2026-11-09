package app.katari.entry.interactions.manga.media.immersive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import app.katari.core.image.progressive.ProgressiveImageVisual
import app.katari.core.image.progressive.rememberProgressivePreview
import app.katari.entry.interactions.manga.reader.image.animation.AnimatedPageImage
import app.katari.entry.interactions.manga.reader.image.layout.PageBitmap
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.displayedSize
import app.katari.entry.interactions.manga.reader.image.profile.DisplayProfile
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import app.katari.entry.interactions.manga.reader.image.tiles.TiledPageImage
import app.katari.entry.viewer.chrome.ui.loading.ReaderMediaLoadOverlay
import app.katari.entry.viewer.chrome.ui.loading.ReaderMediaLoadState
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.gesture.detectReaderTaps
import eu.kanade.tachiyomi.ui.reader.viewer.gesture.quickZoomGesture
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageContent
import eu.kanade.tachiyomi.ui.reader.viewer.page.rememberReaderPageContent
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PageScaleType
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PageStartPosition
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPageZoom
import eu.kanade.tachiyomi.ui.reader.viewer.pager.ZoomedBox
import eu.kanade.tachiyomi.ui.reader.viewer.pager.pagerPageGestures
import eu.kanade.tachiyomi.ui.reader.viewer.pager.takeWheel
import eu.kanade.tachiyomi.ui.reader.viewer.pager.tileView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One page of a chapter in the immersive feed, drawn as the paged reader draws its pages: loading with its progress
 * and its progressive preview, then fitted to the screen and zoomable by pinch, double tap or click, quick zoom and
 * Ctrl with the wheel, or why it failed with a retry. A tap or click shows or hides the feed's controls. While the page
 * is zoomed, [onZoomedChange] is told, so the feed holds its paging and the wheel and drags move the page instead.
 * [onDisplayedSize] is told the page's size once it is known, so a chapter of long strips can be shown as one.
 */
@Composable
internal fun MangaImmersivePage(
    page: ReaderPage,
    onToggleControls: () -> Unit,
    onZoomedChange: (Boolean) -> Unit,
    onDisplayedSize: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(page) { page.chapter.pageLoader?.selectPage(page) }
    // The feed preloads the pages after the one shown itself
    val contentState =
        rememberReaderPageContent(page, settingsKey = null, request = { ImmersivePageRequest }, preloadCount = 0)
    val content = contentState.content

    val progressive by page.progressiveImageState.collectAsState(null)
    val preview = rememberProgressivePreview(progressive)
    val shown = content as? PageContent.Shown
    LaunchedEffect(shown) { if (shown != null) page.releaseProgressivePreviewAfterFinalImageReady() }
    val previewImage = preview.image?.takeIf { shown == null || preview.keepsAnimation }
    val previewSize = progressive?.metadata?.let { IntSize(it.width, it.height) }
        ?: previewImage?.let { IntSize(it.width, it.height) }
    val displayedSize = when {
        previewImage != null && previewSize != null -> previewSize
        shown != null -> shown.layout.displayedSize(shown.image.size)
        else -> null
    }

    val latestOnDisplayedSize by rememberUpdatedState(onDisplayedSize)
    LaunchedEffect(displayedSize) { displayedSize?.let { latestOnDisplayedSize(it) } }

    val zoom = remember(page) { PagerPageZoom() }
    var slotSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(slotSize, displayedSize) {
        if (displayedSize != null) zoom.fit(slotSize, displayedSize, PageScaleType.FitScreen, PageStartPosition.Center)
    }
    val latestOnZoomedChange by rememberUpdatedState(onZoomedChange)
    LaunchedEffect(zoom) { snapshotFlow { zoom.isZoomed }.distinctUntilChanged().collect { latestOnZoomedChange(it) } }

    val scope = rememberCoroutineScope()
    val latestOnToggleControls by rememberUpdatedState(onToggleControls)
    Box(
        modifier = modifier
            .onSizeChanged { slotSize = it }
            .pointerInput(zoom) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        val change = event.changes.firstOrNull() ?: continue
                        val ctrl = event.keyboardModifiers.isCtrlPressed
                        if (zoom.takeWheel(change.scrollDelta, change.position, ctrl, onZoomed = {})) change.consume()
                    }
                }
            }
            .quickZoomGesture(key = zoom, enabled = { true }, onStart = {}, onZoom = zoom::zoomBy)
            .pagerPageGestures(zoom, scope, onZoomed = {})
            .pointerInput(zoom) {
                detectReaderTaps(
                    onTap = { latestOnToggleControls() },
                    onDoubleTap = { position -> scope.launch { zoom.toggleZoom(position, DOUBLE_TAP_ZOOM_MILLIS) } },
                    onLongPress = {},
                    onSecondaryClick = {},
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            previewImage != null && previewSize != null && zoom.isLaidOut ->
                ZoomedBox(zoom) { PageBitmap(previewImage, previewSize, PageImageLayout.Whole) }
            shown != null && zoom.isLaidOut -> ZoomedBox(zoom) {
                when (val image = shown.image) {
                    is PageImage.Still -> TiledPageImage(image, shown.layout, view = { zoom.tileView() })
                    is PageImage.Animated -> AnimatedPageImage(
                        image = image,
                        layout = shown.layout,
                        displayProfile = DisplayProfile.bytes,
                        onFailed = contentState::fail,
                    )
                }
            }
        }
        val unknownError = stringResource(MR.strings.unknown_error)
        val hasPreview = progressive?.visual is ProgressiveImageVisual.Still ||
            progressive?.animation?.frames?.isNotEmpty() == true
        ReaderMediaLoadOverlay(
            state = when (content) {
                is PageContent.Shown -> ReaderMediaLoadState.Ready
                is PageContent.Failed -> ReaderMediaLoadState.Failed(content.error?.message ?: unknownError)
                is PageContent.Loading -> ReaderMediaLoadState.Loading(progress = content.progress)
            },
            showBackground = !hasPreview,
            onBackgroundClick = onToggleControls,
            onRetry = { page.chapter.pageLoader?.retryPage(page) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val DOUBLE_TAP_ZOOM_MILLIS = 500
