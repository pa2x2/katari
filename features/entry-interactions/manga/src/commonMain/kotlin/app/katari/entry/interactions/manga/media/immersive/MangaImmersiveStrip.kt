package app.katari.entry.interactions.manga.media.immersive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import app.katari.core.image.progressive.rememberProgressivePreview
import app.katari.entry.interactions.manga.reader.image.animation.AnimatedPageImage
import app.katari.entry.interactions.manga.reader.image.layout.PageBitmap
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.displayedSize
import app.katari.entry.interactions.manga.reader.image.profile.DisplayProfile
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import app.katari.entry.interactions.manga.reader.image.strips.StripPageImage
import app.katari.entry.viewer.chrome.ui.loading.ReaderMediaLoadOverlay
import app.katari.entry.viewer.chrome.ui.loading.ReaderMediaLoadState
import app.katari.entry.viewer.chrome.ui.loading.nextTitleSkip
import app.katari.presentation.kit.layout.PageStrip
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.gesture.detectReaderTaps
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageContent
import eu.kanade.tachiyomi.ui.reader.viewer.page.rememberReaderPageContent
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A chapter of long strips in the immersive feed: its pages one after another in a reading column, scrolled by the
 * wheel or a swipe. Once the strip can't scroll further, the wheel goes on to the feed, which moves to the next or
 * previous title. A tap shows or hides the feed's controls on a touch screen.
 */
@Composable
internal fun MangaImmersiveStrip(
    pages: List<ReaderPage>,
    state: LazyListState,
    onTap: () -> Unit,
    onNextTitle: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val latestOnTap by rememberUpdatedState(onTap)
    PageStrip(
        state = state,
        maxWidth = KatariTheme.sizes.readableWidth,
        modifier = modifier.pointerInput(Unit) {
            detectReaderTaps(onTap = { latestOnTap() }, onDoubleTap = null, onLongPress = {}, onSecondaryClick = {})
        },
    ) { columnWidth ->
        itemsIndexed(pages, key = { index, _ -> index }) { _, page ->
            val widthPx = with(LocalDensity.current) { columnWidth.roundToPx() }
            StripSlice(page = page, width = widthPx, onNextTitle = onNextTitle, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * One page of the strip, [width] pixels wide: as tall as the page once its size is known and a screen tall before,
 * its progressive preview while it arrives, the page itself, or why it failed with a retry and the next title.
 */
@Composable
private fun LazyItemScope.StripSlice(
    page: ReaderPage,
    width: Int,
    onNextTitle: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val contentState = rememberReaderPageContent(page, settingsKey = null, request = { ImmersivePageRequest })
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
    val sized = if (displayedSize != null && displayedSize.width > 0 && displayedSize.height > 0) {
        Modifier.aspectRatio(displayedSize.width.toFloat() / displayedSize.height)
    } else {
        Modifier.fillParentMaxHeight()
    }
    Box(modifier.then(sized)) {
        when {
            previewImage != null && previewSize != null ->
                PageBitmap(previewImage, previewSize, PageImageLayout.Whole, Modifier.fillMaxSize())
            shown != null -> when (val image = shown.image) {
                is PageImage.Still -> StripPageImage(
                    image = image,
                    layout = shown.layout,
                    width = width,
                    onFailed = contentState::fail,
                    modifier = Modifier.fillMaxSize(),
                )
                is PageImage.Animated -> AnimatedPageImage(
                    image = image,
                    layout = shown.layout,
                    displayProfile = DisplayProfile.bytes,
                    onFailed = contentState::fail,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        val unknownError = stringResource(MR.strings.unknown_error)
        ReaderMediaLoadOverlay(
            state = when (content) {
                is PageContent.Shown -> ReaderMediaLoadState.Ready
                is PageContent.Failed -> ReaderMediaLoadState.Failed(content.error?.message ?: unknownError)
                is PageContent.Loading -> ReaderMediaLoadState.Loading(progress = content.progress)
            },
            showBackground = previewImage == null,
            onRetry = { page.chapter.pageLoader?.retryPage(page) },
            skip = nextTitleSkip(onNextTitle),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
