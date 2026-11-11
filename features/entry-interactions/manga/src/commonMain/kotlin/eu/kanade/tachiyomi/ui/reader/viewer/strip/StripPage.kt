package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import app.katari.core.image.progressive.rememberProgressivePreview
import app.katari.entry.interactions.manga.reader.image.animation.AnimatedPageImage
import app.katari.entry.interactions.manga.reader.image.layout.PageBitmap
import app.katari.entry.interactions.manga.reader.image.layout.displayedSize
import app.katari.entry.interactions.manga.reader.image.profile.DisplayProfile
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import app.katari.entry.interactions.manga.reader.image.strips.StripPageImage
import app.katari.entry.interactions.manga.reader.text.surface.PageTextLayer
import app.katari.presentation.kit.progress.CombinedCircularProgressIndicator
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageContent
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageProblem
import eu.kanade.tachiyomi.ui.reader.viewer.page.rememberReaderPageContent
import kotlin.math.roundToInt

/**
 * One page of the long strip in the slot [slotKey], [width] pixels wide: as tall as the page once its size is known,
 * or was known when it showed before, and [placeholderHeight] pixels tall until then, with its download progress a
 * quarter of the way down; the progressive preview while it arrives; the page itself; or why it failed with a retry.
 */
@Composable
internal fun StripPage(
    page: ReaderPage,
    slotKey: String,
    viewer: StripViewer,
    width: Int,
    placeholderHeight: Int,
    modifier: Modifier = Modifier,
) {
    val settingsKey = viewer.imageSettingsKey()
    val contentState = rememberReaderPageContent(page, settingsKey, viewer::pageRequest, viewer::onPageShown)
    val textSurface = viewer.textSurfaces.rememberSurface(page)
    val content = contentState.content

    val progressive by page.progressiveImageState.collectAsState(null)
    val preview = rememberProgressivePreview(progressive)
    val shown = content as? PageContent.Shown
    LaunchedEffect(shown) { if (shown != null) page.releaseProgressivePreviewAfterFinalImageReady() }
    val previewImage = preview.image?.takeIf { shown == null || preview.keepsAnimation }
    val previewSize = progressive?.metadata?.let { IntSize(it.width, it.height) }
        ?: previewImage?.let { IntSize(it.width, it.height) }
    val previewLayout = previewSize?.let(viewer::previewLayout)

    val displayedSize = when {
        previewImage != null && previewSize != null && previewLayout != null -> previewLayout.displayedSize(previewSize)
        shown != null -> shown.layout.displayedSize(shown.image.size)
        else -> null
    }?.takeIf { it.width > 0 && it.height > 0 }
    val size = displayedSize ?: viewer.pageSizes[slotKey, settingsKey]
    val height = size?.let { (width.toDouble() * it.height / it.width).roundToInt() } ?: placeholderHeight
    viewer.FollowPageBackdrop(page, shown, height)

    val composedHeight = remember { ComposedHeight(height) }
    // The height changes only with state set between frames, so this runs in a frame's composition, before the strip
    // measures this page at the new height
    SideEffect {
        if (displayedSize != null) viewer.pageSizes[slotKey, settingsKey] = displayedSize
        val delta = height - composedHeight.pixels
        composedHeight.pixels = height
        if (delta != 0) viewer.slotAnchor.onSlotResized(slotKey, delta, viewer.listState)
    }

    val density = LocalDensity.current
    Box(modifier.height(with(density) { height.toDp() })) {
        when {
            previewImage != null && previewSize != null && previewLayout != null ->
                PageBitmap(previewImage, previewSize, previewLayout, Modifier.fillMaxSize())
            shown != null -> when (val image = shown.image) {
                is PageImage.Still -> {
                    StripPageImage(
                        image = image,
                        layout = shown.layout,
                        width = width,
                        onFailed = contentState::fail,
                        modifier = Modifier.fillMaxSize(),
                    )
                    PageTextLayer(
                        surface = textSurface,
                        image = image,
                        layout = shown.layout,
                        cropBorders = shown.cropBorders,
                        magnification = { viewer.zoom.scale },
                        onChanged = viewer::onTextSurfacesChanged,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                is PageImage.Animated -> AnimatedPageImage(
                    image = image,
                    layout = shown.layout,
                    displayProfile = DisplayProfile.bytes,
                    onFailed = contentState::fail,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            content is PageContent.Loading -> CombinedCircularProgressIndicator(
                progress = { (contentState.content as? PageContent.Loading)?.progress ?: 0f },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = with(density) { (placeholderHeight / 4).toDp() }),
            )
            content is PageContent.Failed -> PageProblem(
                page = page,
                error = content.error,
                onOpenInWebView = viewer.pageWebView(page),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/** The height a page was last composed at, in pixels. */
private class ComposedHeight(var pixels: Int)
