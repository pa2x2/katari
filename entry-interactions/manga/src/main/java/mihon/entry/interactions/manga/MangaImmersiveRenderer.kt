package mihon.entry.interactions.manga

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import coil3.asDrawable
import coil3.imageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.size.Size
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import mihon.entry.interactions.EntryImmersiveProgress
import mihon.entry.interactions.EntryImmersiveRenderer
import okhttp3.Headers
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal data class MangaImmersiveMedia(
    val pages: List<MangaImmersivePage>,
    val initialPageIndex: Int,
    val entryId: Long,
    val sourceId: Long,
    val chapterNumber: Double,
    val context: android.content.Context,
)

internal data class MangaImmersivePage(
    val index: Int,
    val imageUrl: String,
    val headers: Headers,
)

internal class MangaImmersiveRenderer(
    private val media: MangaImmersiveMedia,
) : EntryImmersiveRenderer {

    @Composable
    override fun Content(
        modifier: Modifier,
        active: Boolean,
        controlsVisible: Boolean,
        controlsBottomInset: Dp,
        onToggleControls: () -> Unit,
        onZoomStateChange: (Boolean) -> Unit,
        onProgress: (EntryImmersiveProgress) -> Unit,
    ) {
        val pages = media.pages
        val pagerState = rememberPagerState(initialPage = media.initialPageIndex) { pages.size }
        var zoomedPageIndex by remember { mutableStateOf<Int?>(null) }
        var loadedPageIndexes by remember { mutableStateOf<Set<Int>>(emptySet()) }
        val isZoomed by remember { derivedStateOf { zoomedPageIndex == pagerState.currentPage } }
        val latestProgress by rememberUpdatedState(onProgress)
        var lastProgressAt by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(active, isZoomed) {
            if (active) onZoomStateChange(isZoomed)
        }
        LaunchedEffect(active, pagerState, pages.size) {
            if (!active) return@LaunchedEffect
            lastProgressAt = SystemClock.elapsedRealtime()
            snapshotFlow { pagerState.settledPage }
                .distinctUntilChanged()
                .collect { pageIndex ->
                    val now = SystemClock.elapsedRealtime()
                    latestProgress(
                        EntryImmersiveProgress.ImagePage(
                            pageIndex = pageIndex,
                            pageCount = pages.size,
                            sessionDurationMs = (now - lastProgressAt).coerceAtLeast(0L),
                        ),
                    )
                    lastProgressAt = now
                }
        }

        Box(
            modifier = modifier.background(
                if (pagerState.currentPage in loadedPageIndexes) Color.Black else Color.Transparent,
            ),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = active && !isZoomed,
                beyondViewportPageCount = if (active) 1 else 0,
            ) { pageIndex ->
                MangaImmersiveImage(
                    page = pages[pageIndex],
                    onImageReady = { loadedPageIndexes = loadedPageIndexes + pageIndex },
                    onToggleControls = { if (active) onToggleControls() },
                    onZoomStateChange = { zoomed ->
                        if (active) zoomedPageIndex = if (zoomed) pageIndex else null
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (active && controlsVisible) {
                Text(
                    text = "${pagerState.currentPage + 1}/${pages.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 16.dp,
                            bottom = controlsBottomInset + 12.dp,
                        )
                        .background(Color.Black.copy(alpha = 0.48f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        DisposableEffect(active, pages.size) {
            onDispose {
                if (active) {
                    val now = SystemClock.elapsedRealtime()
                    latestProgress(
                        EntryImmersiveProgress.ImagePage(
                            pageIndex = pagerState.settledPage,
                            pageCount = pages.size,
                            sessionDurationMs = (now - lastProgressAt).coerceAtLeast(0L),
                        ),
                    )
                    lastProgressAt = now
                    onZoomStateChange(false)
                }
            }
        }
    }
}

@Composable
private fun MangaImmersiveImage(
    page: MangaImmersivePage,
    onImageReady: () -> Unit,
    onToggleControls: () -> Unit,
    onZoomStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewRef = remember { mutableStateOf<ReaderPageImageView?>(null) }
    val requestRef = remember { mutableStateOf<Disposable?>(null) }
    var retryKey by remember(page.imageUrl) { mutableIntStateOf(0) }
    var loadState by remember(page.imageUrl) { mutableStateOf(MangaImageLoadState.Loading) }
    var showLoading by remember(page.imageUrl) { mutableStateOf(false) }

    LaunchedEffect(loadState) {
        if (loadState == MangaImageLoadState.Loading) {
            delay(250L)
            showLoading = true
        } else {
            showLoading = false
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { viewContext ->
                ReaderPageImageView(viewContext).apply {
                    onScaleChanged = { onZoomStateChange(isZoomed()) }
                    onViewClicked = onToggleControls
                    viewRef.value = this
                }
            },
            update = { view ->
                view.onScaleChanged = { onZoomStateChange(view.isZoomed()) }
                view.onViewClicked = onToggleControls
                val requestTag = "${page.imageUrl}:$retryKey"
                if (view.tag != requestTag) {
                    view.tag = requestTag
                    requestRef.value?.dispose()
                    view.recycle()
                    requestRef.value = context.imageLoader.enqueue(
                        ImageRequest.Builder(context)
                            .data(page.imageUrl)
                            .httpHeaders(page.headers.toNetworkHeaders())
                            .size(Size.ORIGINAL)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .target(
                                onStart = { loadState = MangaImageLoadState.Loading },
                                onError = { loadState = MangaImageLoadState.Error },
                                onSuccess = { image ->
                                    val drawable = image.asDrawable(context.resources)
                                    val copy = (drawable as? BitmapDrawable)
                                        ?.bitmap
                                        ?.copy(Bitmap.Config.HARDWARE, false)
                                        ?.toDrawable(context.resources)
                                        ?: drawable
                                    view.setImage(copy, ReaderPageImageView.Config(zoomDuration = 500))
                                    loadState = MangaImageLoadState.Ready
                                    onImageReady()
                                },
                            )
                            .build(),
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (showLoading && loadState == MangaImageLoadState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(40.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.2f),
            )
        }
        if (loadState == MangaImageLoadState.Error) {
            Button(
                onClick = { retryKey++ },
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(stringResource(MR.strings.action_retry))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onZoomStateChange(false)
            requestRef.value?.dispose()
            requestRef.value = null
            viewRef.value?.recycle()
            viewRef.value = null
        }
    }
}

private enum class MangaImageLoadState {
    Loading,
    Ready,
    Error,
}

private fun Headers.toNetworkHeaders(): NetworkHeaders {
    return NetworkHeaders.Builder().apply {
        toMultimap().forEach { (name, values) ->
            values.forEach { value -> add(name, value) }
        }
    }.build()
}
