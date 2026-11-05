package mihon.entry.interactions.manga.media.immersive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.ui.reader.viewer.pager.pagerMouseDrags
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mihon.entry.interactions.media.EntryImmersiveActiveSessionEffect
import mihon.entry.interactions.media.EntryImmersiveProgress
import mihon.entry.interactions.media.EntryImmersiveRenderer
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.presentation.kit.pager.WheelTurns
import mihon.presentation.kit.shortcut.KeyboardShortcut
import mihon.presentation.kit.shortcut.KeyboardShortcutHandler
import mihon.presentation.kit.shortcut.ShortcutGroup
import mihon.presentation.kit.shortcut.ShortcutKeys
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.background
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs
import kotlin.time.TimeSource

/**
 * A chapter in the immersive feed: its pages side by side, turned by a swipe, a mouse drag, a sideways scroll or ← and
 * →, with a page slider while the controls show. The feed itself moves on ↑, ↓ and the wheel, so the chapter leaves
 * those to it. While a page is zoomed or the slider is held, the feed holds its paging.
 */
internal class MangaImmersiveRenderer(
    private val media: MangaImmersiveMedia,
    private val keepScreenOn: ViewerSettingBinding<Boolean>,
    private val platform: MangaImmersivePlatform,
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
        val scope = rememberCoroutineScope()
        var zoomedPageIndex by remember { mutableStateOf<Int?>(null) }
        var isScrubbing by remember { mutableStateOf(false) }
        val isZoomed by remember { derivedStateOf { zoomedPageIndex == pagerState.currentPage } }
        val latestProgress by rememberUpdatedState(onProgress)
        val keepScreenOnSetting by keepScreenOn.state.collectAsState()
        var lastProgressAt by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
        MangaImmersivePagePreloader(
            pages = pages,
            currentPage = pagerState.settledPage,
            active = active,
        )
        LaunchedEffect(active, isZoomed, isScrubbing) {
            if (active) onPagingBlockedChange(isZoomed || isScrubbing)
        }
        LaunchedEffect(active, pagerState, pages.size) {
            if (!active) return@LaunchedEffect
            lastProgressAt = TimeSource.Monotonic.markNow()
            snapshotFlow { pagerState.settledPage }
                .distinctUntilChanged()
                .collect { pageIndex ->
                    val now = TimeSource.Monotonic.markNow()
                    latestProgress(
                        EntryImmersiveProgress.ImagePage(
                            pageIndex = pageIndex,
                            pageCount = pages.size,
                            sessionDurationMs = (now - lastProgressAt).inWholeMilliseconds.coerceAtLeast(0L),
                        ),
                    )
                    lastProgressAt = now
                }
        }

        platform.KeepAwake(active && keepScreenOnSetting.effectiveValue)
        val turns = active && !isZoomed && !isScrubbing
        val latestTurns by rememberUpdatedState(turns)
        val turnBy: (Int) -> Unit = { by ->
            val target = (pagerState.targetPage + by).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            scope.launch { pagerState.animateScrollToPage(target) }
        }
        PageKeys(enabled = turns, onTurn = turnBy)
        // Attached whether or not pages turn: detaching them mid-gesture would cancel the gesture of everything under
        // them, such as the scrubber being dragged or a page being zoomed, which are what stop the turning.
        Box(
            modifier = modifier
                .background(SurfaceContainer.Media)
                .sidewaysScrollTurns(enabled = { latestTurns }, onTurn = turnBy)
                .pagerMouseDrags(pagerState, vertical = false, scope = scope, enabled = { latestTurns }),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = turns,
                beyondViewportPageCount = 0,
            ) { pageIndex ->
                MangaImmersivePage(
                    page = pages[pageIndex],
                    onToggleControls = { if (active) onToggleControls() },
                    onZoomedChange = { zoomed ->
                        if (active) {
                            zoomedPageIndex = if (zoomed) pageIndex else zoomedPageIndex.takeIf { it != pageIndex }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (active && controlsVisible) {
                MangaImmersivePageScrubber(
                    currentPageIndex = pagerState.settledPage,
                    pageCount = pages.size,
                    onPageSelected = { pageIndex ->
                        scope.launch { pagerState.scrollToPage(pageIndex) }
                    },
                    onScrubbingChange = { isScrubbing = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = controlsBottomInset + KatariTheme.spacing.xs),
                )
            }
        }

        EntryImmersiveActiveSessionEffect(active, onPagingBlockedChange) {
            val now = TimeSource.Monotonic.markNow()
            latestProgress(
                EntryImmersiveProgress.ImagePage(
                    pageIndex = pagerState.settledPage,
                    pageCount = pages.size,
                    sessionDurationMs = (now - lastProgressAt).inWholeMilliseconds.coerceAtLeast(0L),
                ),
            )
            lastProgressAt = now
        }
    }
}

/** ← and → turn to the page on that side, where the window has keyboard shortcuts, while [enabled]. */
@Composable
private fun PageKeys(enabled: Boolean, onTurn: (Int) -> Unit) {
    KeyboardShortcutHandler(
        shortcut = pageShortcut(
            id = "immersive.manga.pageLeft",
            label = stringResource(MR.strings.reader_shortcut_page_left),
            key = ShortcutKeys.Chord(Key.DirectionLeft, "←"),
        ),
        enabled = enabled,
    ) { onTurn(-1) }
    KeyboardShortcutHandler(
        shortcut = pageShortcut(
            id = "immersive.manga.pageRight",
            label = stringResource(MR.strings.reader_shortcut_page_right),
            key = ShortcutKeys.Chord(Key.DirectionRight, "→"),
        ),
        enabled = enabled,
    ) { onTurn(1) }
}

private fun pageShortcut(id: String, label: String, key: ShortcutKeys) = KeyboardShortcut(
    id = id,
    label = label,
    keys = listOf(key),
    group = ShortcutGroup.CurrentScreen,
    whileTyping = false,
    beforeFocus = true,
)

/**
 * A sideways scroll, by a touchpad's two fingers or a tilted wheel, turns the chapter's pages as [WheelTurns] counts
 * them, taken ahead of the pager, which would move by the scrolled distance, while [enabled] answers true. An upright
 * scroll is left to the feed.
 */
private fun Modifier.sidewaysScrollTurns(enabled: () -> Boolean, onTurn: (Int) -> Unit): Modifier = pointerInput(Unit) {
    val turns = WheelTurns()
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type != PointerEventType.Scroll) continue
            val change = event.changes.firstOrNull() ?: continue
            val delta = change.scrollDelta
            if (abs(delta.x) <= abs(delta.y)) continue
            if (!enabled()) {
                turns.reset()
                continue
            }
            change.consume()
            turns.onScroll(Offset(delta.x, 0f), change.uptimeMillis) { notches ->
                val whole = notches.x.toInt()
                if (whole != 0) onTurn(whole)
                Offset(whole.toFloat(), 0f)
            }
        }
    }
}
