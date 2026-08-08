package mihon.entry.interactions.manga.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.core.common.image.progressive.ProgressiveImageVisual
import okio.Buffer
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderMediaLoadOverlay
import tachiyomi.presentation.core.components.reader.ReaderMediaLoadState
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun MangaImmersiveImage(
    page: ReaderPage,
    onToggleControls: () -> Unit,
    onPagingBlockedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewRef = remember { mutableStateOf<ReaderPageImageView?>(null) }
    val status by page.statusFlow.collectAsStateWithLifecycle()
    val progress by page.progressFlow.collectAsStateWithLifecycle()
    val progressiveImage by page.progressiveImageState.collectAsStateWithLifecycle(initialValue = null)
    val hasProgressiveVisual = progressiveImage?.visual is ProgressiveImageVisual.Still ||
        progressiveImage?.animation?.frames?.isNotEmpty() == true
    val unknownError = stringResource(MR.strings.unknown_error)
    var retryKey by remember(page) { mutableIntStateOf(0) }
    var loadedImage by remember(page) { mutableStateOf<MangaImmersiveLoadedImage?>(null) }
    var decodeErrorMessage by remember(page) { mutableStateOf<String?>(null) }
    var imageReady by remember(page) { mutableStateOf(false) }

    LaunchedEffect(page) {
        try {
            page.chapter.pageLoader?.loadPage(page, preloadCount = 0)
        } catch (e: CancellationException) {
            throw e
        }
    }

    LaunchedEffect(page, status, retryKey) {
        loadedImage = null
        imageReady = false
        decodeErrorMessage = null
        if (status != Page.State.Ready) return@LaunchedEffect
        try {
            loadedImage = withContext(Dispatchers.IO) {
                val stream = checkNotNull(page.stream) { "No image stream available for page ${page.index + 1}" }
                val source = stream().use { input -> Buffer().readFrom(input) }
                MangaImmersiveLoadedImage(
                    source = source,
                    isAnimated = ImageUtil.isAnimatedAndSupported(source),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            decodeErrorMessage = e.message ?: unknownError
        }
    }

    val previewRequest = if (status == Page.State.Ready) {
        remember(context, page, retryKey) {
            ImageRequest.Builder(context)
                .data(page)
                .size(Size(PREVIEW_SIZE_PX, PREVIEW_SIZE_PX))
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        }
    } else {
        null
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { viewContext ->
                ReaderPageImageView(viewContext).apply {
                    onViewClicked = onToggleControls
                    viewRef.value = this
                }
            },
            update = { view ->
                view.onViewClicked = onToggleControls
                val image = loadedImage
                if (image == null) {
                    if (view.tag != null) {
                        view.tag = null
                        view.onImageLoaded = null
                        view.onImageLoadError = null
                        view.recycleFinalImage()
                    }
                    view.setProgressiveImage(progressiveImage)
                    return@AndroidView
                }
                view.setProgressiveImage(progressiveImage)
                val requestTag = "${page.chapter.chapter.id}:${page.index}:$retryKey"
                if (view.tag != requestTag) {
                    view.tag = requestTag
                    view.recycleFinalImage()
                    imageReady = false
                    decodeErrorMessage = null
                    view.onImageLoaded = {
                        if (view.tag == requestTag) imageReady = true
                    }
                    view.onImageLoadError = {
                        if (view.tag == requestTag) decodeErrorMessage = it?.message ?: unknownError
                    }
                    try {
                        view.setImage(
                            source = image.source,
                            isAnimated = image.isAnimated,
                            config = ReaderPageImageView.Config(zoomDuration = 500),
                        )
                    } catch (e: Throwable) {
                        decodeErrorMessage = e.message ?: unknownError
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        val loadError = decodeErrorMessage ?: (status as? Page.State.Error)?.error?.let { error ->
            error.message ?: unknownError
        }
        ReaderMediaLoadOverlay(
            state = when {
                loadError != null -> ReaderMediaLoadState.Failed(loadError)
                imageReady -> ReaderMediaLoadState.Ready
                else -> ReaderMediaLoadState.Loading(
                    progress = progress.takeIf { status == Page.State.DownloadImage && it in 1..100 }
                        ?.div(100f),
                )
            },
            previewModel = previewRequest,
            showBackground = !hasProgressiveVisual,
            onBackgroundClick = onToggleControls,
            onRetry = {
                loadedImage = null
                imageReady = false
                decodeErrorMessage = null
                retryKey++
                page.chapter.pageLoader?.retryPage(page)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            onPagingBlockedChange(false)
            viewRef.value?.let { view ->
                view.tag = null
                view.onImageLoaded = null
                view.onImageLoadError = null
                view.recycle()
            }
            viewRef.value = null
        }
    }
}

private data class MangaImmersiveLoadedImage(
    val source: BufferedSource,
    val isAnimated: Boolean,
)

private const val PREVIEW_SIZE_PX = 384
