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
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderMediaLoadOverlay
import tachiyomi.presentation.core.components.reader.ReaderMediaLoadState
import tachiyomi.presentation.core.i18n.stringResource
import java.io.File

@Composable
internal fun MangaImmersiveImage(
    page: MangaImmersivePage,
    onToggleControls: () -> Unit,
    onPagingBlockedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewRef = remember { mutableStateOf<ReaderPageImageView?>(null) }
    val progress by page.progress.collectAsStateWithLifecycle()
    val unknownError = stringResource(MR.strings.unknown_error)
    var retryKey by remember(page) { mutableIntStateOf(0) }
    var loadedImage by remember(page) { mutableStateOf<MangaImmersiveLoadedImage?>(null) }
    var errorMessage by remember(page) { mutableStateOf<String?>(null) }
    var imageReady by remember(page) { mutableStateOf(false) }

    LaunchedEffect(page, retryKey) {
        loadedImage = null
        imageReady = false
        errorMessage = null
        try {
            val imageFile = page.loadImage()
            val isAnimated = withContext(Dispatchers.IO) {
                imageFile.source().buffer().use(ImageUtil::isAnimatedAndSupported)
            }
            loadedImage = MangaImmersiveLoadedImage(imageFile, isAnimated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            errorMessage = e.message ?: unknownError
        }
    }

    val previewRequest = loadedImage?.file?.let { file ->
        remember(context, file) {
            ImageRequest.Builder(context)
                .data(file)
                .size(Size(PREVIEW_SIZE_PX, PREVIEW_SIZE_PX))
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { viewContext ->
                ReaderPageImageView(viewContext).apply {
                    onScaleChanged = { onPagingBlockedChange(isZoomed()) }
                    onViewClicked = onToggleControls
                    viewRef.value = this
                }
            },
            update = { view ->
                view.onScaleChanged = { onPagingBlockedChange(view.isZoomed()) }
                view.onViewClicked = onToggleControls
                val image = loadedImage
                if (image == null) {
                    if (view.tag != null) {
                        view.tag = null
                        view.onImageLoaded = null
                        view.onImageLoadError = null
                        view.recycle()
                    }
                    return@AndroidView
                }
                val requestTag = "${image.file.absolutePath}:$retryKey"
                if (view.tag != requestTag) {
                    view.tag = requestTag
                    view.recycle()
                    imageReady = false
                    errorMessage = null
                    view.onImageLoaded = {
                        if (view.tag == requestTag) imageReady = true
                    }
                    view.onImageLoadError = {
                        if (view.tag == requestTag) errorMessage = it?.message ?: unknownError
                    }
                    try {
                        view.setImage(
                            file = image.file,
                            isAnimated = image.isAnimated,
                            config = ReaderPageImageView.Config(zoomDuration = 500),
                        )
                    } catch (e: Throwable) {
                        errorMessage = e.message ?: unknownError
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        ReaderMediaLoadOverlay(
            state = when {
                errorMessage != null -> ReaderMediaLoadState.Failed(errorMessage.orEmpty())
                imageReady -> ReaderMediaLoadState.Ready
                else -> ReaderMediaLoadState.Loading(progress.fraction)
            },
            previewModel = previewRequest,
            onBackgroundClick = onToggleControls,
            onRetry = { retryKey++ },
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
    val file: File,
    val isAnimated: Boolean,
)

private const val PREVIEW_SIZE_PX = 384
