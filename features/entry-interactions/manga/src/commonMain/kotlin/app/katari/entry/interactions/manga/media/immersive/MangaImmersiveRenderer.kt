package app.katari.entry.interactions.manga.media.immersive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import app.katari.entry.interactions.media.EntryImmersiveActiveSessionEffect
import app.katari.entry.interactions.media.EntryImmersiveMediaSwitches
import app.katari.entry.interactions.media.EntryImmersivePlace
import app.katari.entry.interactions.media.EntryImmersivePlaceTracker
import app.katari.entry.interactions.media.EntryImmersiveRenderer
import app.katari.entry.interactions.media.fittedMediaWidth
import app.katari.entry.viewer.chrome.ui.pointer.mediaControl
import app.katari.entry.viewer.settings.ViewerSettingBinding
import app.katari.presentation.kit.pager.WheelTurns
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import app.katari.presentation.kit.shortcut.ShortcutGroup
import app.katari.presentation.kit.shortcut.ShortcutKeys
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.reader.viewer.pager.pagerMouseDrags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs

/**
 * A chapter in the immersive feed: its pages side by side, turned by a swipe, a mouse drag, a sideways scroll or ← and
 * →, with a page slider while the controls show. A chapter of long strips, pages over [STRIP_ASPECT] times taller than
 * wide, reads down a column instead ([MangaImmersiveStrip]), which the wheel scrolls before it moves the feed. The feed itself moves on ↑, ↓ and the wheel, so the chapter leaves
 * those to it. While a page is zoomed or the slider is held, the feed holds its paging. It opens at the chapter's saved
 * page and follows it when the reader or another device moves it, but never saves a page of its own.
 */
internal class MangaImmersiveRenderer(
    private val media: MangaImmersiveMedia,
    private val savedPageIndex: Flow<Int>,
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
        place: EntryImmersivePlaceTracker,
        onMediaWidthChange: (Dp?) -> Unit,
        switches: EntryImmersiveMediaSwitches,
    ) {
        val pages = media.pages
        val pagerState = rememberPagerState(initialPage = media.initialPageIndex) { pages.size }
        val stripState = rememberLazyListState()
        // Known once a page shows: a chapter whose pages are long strips reads down a column instead
        var strip by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        var zoomedPageIndex by remember { mutableStateOf<Int?>(null) }
        var isScrubbing by remember { mutableStateOf(false) }
        val isZoomed by remember { derivedStateOf { zoomedPageIndex == pagerState.currentPage } }
        val keepScreenOnSetting by keepScreenOn.state.collectAsState()
        val pageSizes = remember { mutableStateMapOf<Int, IntSize>() }
        var size by remember { mutableStateOf(IntSize.Zero) }
        val currentPage = if (strip) stripState.firstVisibleItemIndex else pagerState.settledPage
        val showPage: suspend (Int) -> Unit = { pageIndex ->
            if (strip) stripState.scrollToItem(pageIndex) else pagerState.scrollToPage(pageIndex)
        }
        val density = LocalDensity.current
        val stripWidth = KatariTheme.sizes.readableWidth
        val mediaWidth = if (strip) {
            with(density) { size.width.toDp() }.takeIf { size.width > 0 }?.coerceAtMost(stripWidth)
        } else {
            pageSizes[currentPage]
                ?.let { fittedMediaWidth(size, it.width.toFloat() / it.height) }
                ?.let { with(density) { it.toDp() } }
        }
        val latestOnMediaWidthChange by rememberUpdatedState(onMediaWidthChange)
        LaunchedEffect(mediaWidth) { latestOnMediaWidthChange(mediaWidth) }
        MangaImmersivePagePreloader(
            pages = pages,
            currentPage = currentPage,
            active = active,
        )
        LaunchedEffect(active, isZoomed, isScrubbing) {
            if (active) onPagingBlockedChange(isZoomed || isScrubbing)
        }
        val latestShowPage by rememberUpdatedState(showPage)
        LaunchedEffect(pagerState, pages.size) {
            var followed = media.initialPageIndex
            savedPageIndex.collect { saved ->
                if (saved != followed && pages.isNotEmpty()) latestShowPage(saved.coerceIn(0, pages.lastIndex))
                followed = saved
            }
        }
        val latestCurrentPage by rememberUpdatedState(currentPage)
        DisposableEffect(place) {
            place.track { EntryImmersivePlace.ImagePage(latestCurrentPage) }
            onDispose { place.untrack() }
        }

        platform.KeepAwake(active && keepScreenOnSetting.effectiveValue)
        val turns = active && !isZoomed && !isScrubbing
        val latestTurns by rememberUpdatedState(turns && !strip)
        val turnBy: (Int) -> Unit = { by ->
            val last = (pages.size - 1).coerceAtLeast(0)
            if (strip) {
                val target = (stripState.firstVisibleItemIndex + by).coerceIn(0, last)
                scope.launch { stripState.animateScrollToItem(target) }
            } else {
                val target = (pagerState.targetPage + by).coerceIn(0, last)
                scope.launch { pagerState.animateScrollToPage(target) }
            }
        }
        PageKeys(enabled = turns, onTurn = turnBy)
        // Attached whether or not pages turn: detaching them mid-gesture would cancel the gesture of everything under
        // them, such as the scrubber being dragged or a page being zoomed, which are what stop the turning.
        Box(
            modifier = modifier
                .onSizeChanged { size = it }
                .sidewaysScrollTurns(enabled = { latestTurns }, onTurn = turnBy)
                .pagerMouseDrags(pagerState, vertical = false, scope = scope, enabled = { latestTurns }),
        ) {
            if (strip) {
                MangaImmersiveStrip(
                    pages = pages,
                    state = stripState,
                    onTap = { if (active) onToggleControls() },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
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
                        onDisplayedSize = { pageSize ->
                            if (pageSize.height > 0) pageSizes[pageIndex] = pageSize
                            if (!strip && pageSize.isStrip()) {
                                strip = true
                                scope.launch { stripState.scrollToItem(pageIndex) }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (active && controlsVisible) {
                MangaImmersivePageScrubber(
                    currentPageIndex = currentPage,
                    pageCount = pages.size,
                    onPageSelected = { pageIndex -> scope.launch { showPage(pageIndex) } },
                    onScrubbingChange = { isScrubbing = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = controlsBottomInset + KatariTheme.spacing.xs)
                        .widthIn(max = mediaWidth ?: Dp.Unspecified)
                        .mediaControl(),
                )
            }
        }

        EntryImmersiveActiveSessionEffect(active, onPagingBlockedChange)
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

/** A page this many times taller than wide is a slice of a long strip, as webtoons are cut. */
private const val STRIP_ASPECT = 3f

private fun IntSize.isStrip(): Boolean = width > 0 && height > width * STRIP_ASPECT
