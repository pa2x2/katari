package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import app.katari.core.image.progressive.rememberProgressivePreview
import app.katari.entry.interactions.manga.reader.image.animation.AnimatedPageImage
import app.katari.entry.interactions.manga.reader.image.background.brush
import app.katari.entry.interactions.manga.reader.image.layout.PageBitmap
import app.katari.entry.interactions.manga.reader.image.layout.displayedSize
import app.katari.entry.interactions.manga.reader.image.profile.DisplayProfile
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import app.katari.entry.interactions.manga.reader.image.tiles.TiledPageImage
import app.katari.entry.interactions.manga.reader.text.surface.PageTextLayer
import app.katari.presentation.kit.progress.CombinedCircularProgressIndicator
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.gesture.quickZoomGesture
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageContent
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageProblem
import eu.kanade.tachiyomi.ui.reader.viewer.page.rememberReaderPageContent
import kotlinx.coroutines.delay
import kotlin.math.ceil

/**
 * One page in its slot: loading with its download progress, the progressive preview while it arrives, the page itself
 * fitted and zoomable, or why it failed with a retry. With [narrowedAgainst], the page narrows to its width once its
 * size is known and sits against that side of the slot, so two facing pages meet; the background that follows the page
 * still fills the whole slot. [onPlaced] hears where the page itself is, narrowed or not.
 */
@Composable
internal fun PagerPage(
    page: ReaderPage,
    viewer: PagedViewer,
    narrowedAgainst: Alignment?,
    onPlaced: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentState =
        rememberReaderPageContent(page, viewer.imageSettingsKey(), viewer::pageRequest, viewer::onPageShown)
    val content = contentState.content

    val progressive by page.progressiveImageState.collectAsState(null)
    val preview = rememberProgressivePreview(progressive)
    val shown = content as? PageContent.Shown
    LaunchedEffect(shown) { if (shown != null) page.releaseProgressivePreviewAfterFinalImageReady() }
    val previewImage = preview.image?.takeIf { shown == null || preview.keepsAnimation }
    val previewSize = progressive?.metadata?.let { IntSize(it.width, it.height) }
        ?: previewImage?.let { IntSize(it.width, it.height) }
    val previewLayout = previewSize?.let { viewer.previewLayout(page, it) }

    val displayedSize = when {
        previewImage != null && previewSize != null && previewLayout != null -> previewLayout.displayedSize(previewSize)
        shown != null -> shown.layout.displayedSize(shown.image.size)
        else -> null
    }

    val textSurface = viewer.textSurfaces.rememberSurface(page)
    val zoom = remember(page) { PagerPageZoom() }
    DisposableEffect(page, zoom) {
        viewer.registerZoom(page, zoom)
        onDispose { viewer.unregisterZoom(page, zoom) }
    }
    var slotSize by remember { mutableStateOf(IntSize.Zero) }
    val scaleType = PageScaleType.fromSetting(
        viewer.settings.bindings.imageScaleType.state.collectAsState().value.effectiveValue,
    )
    val startPosition = viewer.startPositionState()
    LaunchedEffect(slotSize, displayedSize, scaleType, startPosition) {
        if (displayedSize != null) zoom.fit(slotSize, displayedSize, scaleType, startPosition)
    }

    val selection = viewer.selection
    LaunchedEffect(selection, shown, zoom.isLaidOut) {
        if (selection?.page == page && shown != null && zoom.isLaidOut && viewer.settings.landscapeZoom) {
            delay(LANDSCAPE_ZOOM_DELAY_MILLIS)
            zoom.zoomLandscapePage(selection.forward)
        }
    }

    val scope = rememberCoroutineScope()
    val background = shown?.background
    Box(
        modifier = modifier.then(if (background != null) Modifier.background(background.brush()) else Modifier),
        contentAlignment = narrowedAgainst ?: Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .narrowedToImage(if (narrowedAgainst != null) displayedSize else null)
                .onGloballyPositioned(onPlaced)
                .onSizeChanged { slotSize = it }
                .quickZoomGesture(
                    key = zoom,
                    enabled = { true },
                    onStart = { viewer.onPageZoomed() },
                    onZoom = { factor, focus -> zoom.zoomBy(factor, focus) },
                )
                .pagerPageGestures(zoom, scope, onZoomed = viewer::onPageZoomed),
            contentAlignment = Alignment.Center,
        ) {
            when {
                previewImage != null && previewSize != null && previewLayout != null && zoom.isLaidOut ->
                    ZoomedBox(zoom) { PageBitmap(previewImage, previewSize, previewLayout) }
                shown != null && zoom.isLaidOut -> ZoomedBox(zoom) {
                    when (val image = shown.image) {
                        // The tile canvas has no size of its own, so it takes the fitted size from the box
                        is PageImage.Still -> Box(propagateMinConstraints = true) {
                            TiledPageImage(image, shown.layout, view = { zoom.tileView() })
                            PageTextLayer(
                                surface = textSurface,
                                image = image,
                                layout = shown.layout,
                                cropBorders = shown.cropBorders,
                                magnification = { zoom.scale / zoom.fittedScale },
                                onChanged = viewer::onTextSurfacesChanged,
                            )
                        }
                        is PageImage.Animated -> AnimatedPageImage(
                            image = image,
                            layout = shown.layout,
                            displayProfile = DisplayProfile.bytes,
                            onFailed = contentState::fail,
                        )
                    }
                }
                content is PageContent.Loading -> CombinedCircularProgressIndicator(
                    progress = { (contentState.content as? PageContent.Loading)?.progress ?: 0f },
                )
                content is PageContent.Failed -> PageProblem(
                    page = page,
                    error = content.error,
                    onOpenInWebView = viewer.pageWebView(page),
                )
            }
        }
    }
}

// Narrower than the slot when the page is: a page fitted to the screen is no wider than its height allows
private fun Modifier.narrowedToImage(displayedSize: IntSize?): Modifier = layout { measurable, constraints ->
    val width = if (displayedSize != null && displayedSize.height > 0 && constraints.hasBoundedHeight) {
        ceil(displayedSize.width.toFloat() / displayedSize.height * constraints.maxHeight).toInt()
            .coerceIn(constraints.minWidth, constraints.maxWidth)
    } else {
        constraints.maxWidth
    }
    val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
    layout(width, constraints.maxHeight) { placeable.place(0, 0) }
}

private const val LANDSCAPE_ZOOM_DELAY_MILLIS = 500L
