package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import eu.kanade.tachiyomi.data.coil.TachiyomiImageDecoder
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageContent
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageProblem
import eu.kanade.tachiyomi.ui.reader.viewer.page.rememberReaderPageContent
import mihon.core.image.progressive.rememberProgressivePreview
import mihon.entry.interactions.manga.reader.image.animation.AnimatedPageImage
import mihon.entry.interactions.manga.reader.image.layout.PageBitmap
import mihon.entry.interactions.manga.reader.image.layout.displayedSize
import mihon.entry.interactions.manga.reader.image.source.PageImage
import mihon.entry.interactions.manga.reader.image.strips.StripPageImage
import mihon.entry.interactions.manga.reader.text.surface.PageTextLayer
import mihon.presentation.kit.progress.CombinedCircularProgressIndicator

/**
 * One page of the long strip, [width] pixels wide: as tall as the page once its size is known, and as tall as
 * [placeholderHeight] before, with its download progress a quarter of the way down; the progressive preview while it
 * arrives; the page itself; or why it failed with a retry.
 */
@Composable
internal fun StripPage(
    page: ReaderPage,
    viewer: StripViewer,
    width: Int,
    placeholderHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val contentState = rememberReaderPageContent(page, viewer.imageSettingsKey(), viewer::pageRequest)
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
    }
    val sized = if (displayedSize != null && displayedSize.width > 0 && displayedSize.height > 0) {
        Modifier.aspectRatio(displayedSize.width.toFloat() / displayedSize.height)
    } else {
        Modifier.height(placeholderHeight)
    }

    Box(modifier.then(sized)) {
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
                    displayProfile = TachiyomiImageDecoder.displayProfile,
                    onFailed = contentState::fail,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            content is PageContent.Loading -> CombinedCircularProgressIndicator(
                progress = { (contentState.content as? PageContent.Loading)?.progress ?: 0f },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = placeholderHeight / 4),
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
