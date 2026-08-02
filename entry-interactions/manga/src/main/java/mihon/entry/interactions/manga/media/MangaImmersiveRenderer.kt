package mihon.entry.interactions.manga.media

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import mihon.entry.interactions.media.EntryImmersiveActiveSessionEffect
import mihon.entry.interactions.media.EntryImmersiveProgress
import mihon.entry.interactions.media.EntryImmersiveRenderer
import tachiyomi.presentation.core.components.reader.ReaderPageIndicator

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
        onPagingBlockedChange: (Boolean) -> Unit,
        onProgress: (EntryImmersiveProgress) -> Unit,
    ) {
        val pages = media.pages
        val pagerState = rememberPagerState(initialPage = media.initialPageIndex) { pages.size }
        var zoomedPageIndex by remember { mutableStateOf<Int?>(null) }
        val isZoomed by remember { derivedStateOf { zoomedPageIndex == pagerState.currentPage } }
        val latestProgress by rememberUpdatedState(onProgress)
        var lastProgressAt by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
        MangaImmersivePagePreloader(
            pages = pages,
            currentPage = pagerState.settledPage,
            active = active,
        )
        LaunchedEffect(active, isZoomed) {
            if (active) onPagingBlockedChange(isZoomed)
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
            modifier = modifier.background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = active && !isZoomed,
                beyondViewportPageCount = 0,
            ) { pageIndex ->
                MangaImmersiveImage(
                    page = pages[pageIndex],
                    onToggleControls = { if (active) onToggleControls() },
                    onPagingBlockedChange = { zoomed ->
                        if (active) zoomedPageIndex = if (zoomed) pageIndex else null
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (active && controlsVisible) {
                ReaderPageIndicator(
                    currentPage = pagerState.currentPage + 1,
                    totalPages = pages.size,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = controlsBottomInset + 12.dp),
                )
            }
        }

        EntryImmersiveActiveSessionEffect(active, onPagingBlockedChange) {
            val now = SystemClock.elapsedRealtime()
            latestProgress(
                EntryImmersiveProgress.ImagePage(
                    pageIndex = pagerState.settledPage,
                    pageCount = pages.size,
                    sessionDurationMs = (now - lastProgressAt).coerceAtLeast(0L),
                ),
            )
            lastProgressAt = now
        }
    }
}
