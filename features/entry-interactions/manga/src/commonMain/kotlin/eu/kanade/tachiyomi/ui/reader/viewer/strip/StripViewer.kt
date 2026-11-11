package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.text.surface.MangaPageTextSurface
import app.katari.entry.interactions.manga.reader.text.surface.ViewerTextSurfaces
import app.katari.entry.interactions.reader.settings.ChapterTransitionMode
import app.katari.entry.interactions.reader.settings.MangaReaderSettings
import app.katari.entry.interactions.viewer.EntryChildDirection
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.model.automaticTransitionLoadDestination
import eu.kanade.tachiyomi.ui.reader.model.automaticTransitionLoadItemAtAnchor
import eu.kanade.tachiyomi.ui.reader.screen.MangaReaderController
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderViewer
import eu.kanade.tachiyomi.ui.reader.viewer.key.ReaderKeyCommand
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageRequest
import eu.kanade.tachiyomi.ui.reader.viewer.page.pageWebViewAction
import eu.kanade.tachiyomi.ui.reader.viewer.tap.ReaderTapZones
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.system.logcat
import kotlin.math.abs
import kotlin.math.min

/**
 * The long strip reading modes: every page one under the other at the strip's width, with the transitions between
 * chapters, scrolled continuously; [continuous] leaves no gap between pages. It follows the page the reader has
 * reached, scrolls by most of a screen from taps and keys, scrolls by itself while auto scroll runs, and zooms the whole
 * strip. What it draws is [Content]. [openPageInWebView] opens a page's image address where the platform can, or is
 * null where it can't.
 */
internal class StripViewer(
    private val controller: MangaReaderController,
    val continuous: Boolean,
    private val openPageInWebView: ((url: String, sourceId: Long?) -> Unit)?,
) : ReaderViewer {
    private val viewModel get() = controller.viewModel

    val settings = StripSettings(viewModel.readerSettings)

    val tapZones = ReaderTapZones(settings.bindings)

    var items by mutableStateOf<List<ReaderViewerItem>>(emptyList())
        private set

    /** Replaced once, when the first chapter is set, by one that starts at the page reading starts from. */
    var listState by mutableStateOf(newListState(0))
        private set

    val zoom = StripZoom()

    internal val slotAnchor = StripSlotAnchor()

    /** The scope of Content's composition, which animated scrolls and auto scroll run in. */
    internal var composeScope: CoroutineScope? = null

    /** The strip's height on screen in pixels, which Content reports. */
    internal var viewportHeight = 0

    private var laidOut = false
    private var currentItem: ReaderViewerItem? = null
    private var anchoredItem: ReaderViewerItem.Transition? = null
    private var autoScroll: Job? = null
    private var autoScrollLevel = MangaReaderSettings.AUTO_SCROLL_LEVEL_DEFAULT

    internal val backdrops = StripBackdrops(items = { items })

    internal val textSurfaces = ViewerTextSurfaces(decorations = { viewModel.textSession.decoration(it) })

    override fun visibleTextSurfaces(): List<MangaPageTextSurface> = textSurfaces.visible()

    override fun preloadedTextSurfaces(): List<MangaPageTextSurface> = textSurfaces.preloaded()

    /** A page started or stopped showing an image, so text features look at the pages again. */
    internal fun onTextSurfacesChanged() = controller.onPageTextSurfacesChanged()

    override fun setChapters(chapters: ViewerChapters) {
        val forceTransition = when (settings.chapterTransitionMode) {
            ChapterTransitionMode.ALWAYS -> true
            ChapterTransitionMode.WHEN_NEEDED -> currentItem is ReaderViewerItem.Transition
            // Hidden mode drops the transition slot as soon as the destination chapter is loaded, so the slot the
            // reader is parked on turns into pages instead of waiting for another scroll
            ChapterTransitionMode.HIDDEN -> false
        }
        items = stripItems(chapters, forceTransition)
        if (laidOut) return
        // The strip keeps its place by the keys of the slots shown; only the first chapter decides where it starts
        val pages = chapters.current.pages ?: return
        laidOut = true
        val start = items.indexOf(ReaderViewerItem.Page(pages[min(chapters.current.requestedPage, pages.lastIndex)]))
        listState = newListState(start.coerceAtLeast(0))
    }

    override fun moveToPage(page: ReaderPage) {
        val position = items.indexOf(ReaderViewerItem.Page(page))
        if (position == -1) {
            logcat { "Page $page not found in the strip" }
            return
        }
        listState.requestScrollToItem(position)
    }

    /** Called by Content each time the strip lays out, to follow the page reached and load chapters it nears. */
    internal fun onLayout(info: LazyListLayoutInfo) {
        if (info.visibleItemsInfo.isEmpty()) return
        val reached = info.lastItemEndingOnScreen()?.let { items.getOrNull(it) }
        loadChapterNearAnchor(info, reached)
        if (reached != null && reached != currentItem) {
            currentItem = reached
            when (reached) {
                is ReaderViewerItem.Page -> {
                    reached.page.chapter.pageLoader?.selectPage(reached.page)
                    controller.onPageSelected(reached.page)
                }
                is ReaderViewerItem.Transition -> logcat { "onTransitionSelected: ${reached.transition}" }
            }
        }
        val last = reached as? ReaderViewerItem.Transition
        if (last != null && last.transition.direction == EntryChildDirection.NEXT && last.transition.to == null) {
            // No more chapters: the reader is probably about to close it
            controller.setMenuVisibility(true)
        }
    }

    // The transition in the middle of the screen, or at an end the strip can't scroll past, asks for its chapter
    private fun loadChapterNearAnchor(info: LazyListLayoutInfo, reached: ReaderViewerItem?) {
        val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
        val centred = info.visibleItemsInfo.firstOrNull { it.offset <= middle && it.offset + it.size > middle }
        val anchor = automaticTransitionLoadItemAtAnchor(
            centeredItem = centred?.let { items.getOrNull(it.index) },
            firstVisibleItem = items.getOrNull(info.visibleItemsInfo.first().index),
            lastVisibleItem = reached,
            canScrollBackward = listState.canScrollBackward,
            canScrollForward = listState.canScrollForward,
        )
        if (anchor == anchoredItem) return
        anchoredItem = anchor
        anchor?.automaticTransitionLoadDestination()?.let { destination ->
            logcat { "Request destination chapter because its transition reached the reading anchor" }
            controller.requestTransitionChapterLoad(destination)
        }
    }

    /** Called by Content as the reader scrolls the strip by [distance] screen pixels at once. */
    internal fun onScrolledBy(distance: Float) {
        if (abs(distance) > settings.menuHideDistance && viewModel.state.value.menuVisible) {
            controller.setMenuVisibility(false)
        }
    }

    /** Called by Content when the reader starts dragging or zooming the strip, which stops auto scroll. */
    internal fun onDirectManipulation() = controller.onViewerDirectManipulation()

    fun scrollDown() = scrollByScreen(1)

    fun scrollUp() = scrollByScreen(-1)

    fun toggleMenu() = controller.toggleMenu()

    override fun onPageKey(command: ReaderKeyCommand) {
        tapZones.hide()
        when (command) {
            ReaderKeyCommand.PageRight, ReaderKeyCommand.NextPage -> scrollDown()
            ReaderKeyCommand.PageLeft, ReaderKeyCommand.PreviousPage -> scrollUp()
            ReaderKeyCommand.ZoomIn -> zoomAboutMiddle(KEY_ZOOM_STEP)
            ReaderKeyCommand.ZoomOut -> zoomAboutMiddle(1f / KEY_ZOOM_STEP)
            ReaderKeyCommand.ZoomFit -> zoomAboutMiddle(1f / zoom.scale)
            else -> Unit
        }
    }

    private fun zoomAboutMiddle(factor: Float) {
        zoom.zoomBy(factor, Offset(zoom.viewportWidth / 2f, viewportHeight / 2f), ::scrollBy)
    }

    // Most of a screen, so the last lines read stay in sight
    private fun scrollByScreen(direction: Int) {
        val distance = direction * viewportHeight * SCREEN_SCROLL_SHARE / zoom.scale
        val scope = composeScope
        if (settings.pageTransitions && scope != null) {
            scope.launch { listState.animateScrollBy(distance) }
        } else {
            listState.dispatchRawDelta(distance)
        }
    }

    internal fun scrollBy(distance: Float) {
        listState.dispatchRawDelta(distance)
    }

    override fun supportsAutoScroll(): Boolean = true

    override fun startAutoScroll(speed: Int) {
        updateAutoScrollSpeed(speed)
        if (autoScroll?.isActive == true) return
        autoScroll = composeScope?.launch {
            var previousFrame = 0L
            var remainder = 0.0
            while (isActive) {
                withFrameNanos { frame ->
                    if (previousFrame != 0L && !viewModel.state.value.menuVisible) {
                        val seconds = (frame - previousFrame) / NANOS_PER_SECOND
                        val distance = autoScrollVelocity() * seconds + remainder
                        val whole = distance.toInt()
                        remainder = distance - whole
                        if (whole != 0) listState.dispatchRawDelta(whole.toFloat())
                    }
                    previousFrame = frame
                }
            }
        }
    }

    override fun updateAutoScrollSpeed(speed: Int) {
        autoScrollLevel = speed.coerceIn(MangaReaderSettings.AUTO_SCROLL_SPEED_RANGE)
    }

    override fun stopAutoScroll() {
        autoScroll?.cancel()
        autoScroll = null
    }

    override fun destroy() = stopAutoScroll()

    // In strip pixels per second, as a share of the screen's height
    private fun autoScrollVelocity(): Double {
        val screensPerSecond = when (autoScrollLevel) {
            0 -> 0.12
            1 -> 0.18
            2 -> 0.24
            3 -> 0.32
            4 -> 0.42
            5 -> 0.54
            else -> 0.70
        }
        return viewportHeight * screensPerSecond
    }

    /** A right click on [page] at [windowPosition], which opens its actions there whatever the long press setting. */
    internal fun onPageSecondaryClick(page: ReaderPage, windowPosition: Offset) =
        controller.onPageLongTap(page, menuAt = windowPosition)

    /** A long press on [page], which opens its actions when the setting allows or the menu is open. */
    internal fun onPageLongPress(page: ReaderPage): Boolean {
        if (!settings.longTapEnabled && !viewModel.state.value.menuVisible) return false
        controller.onPageLongTap(page)
        return true
    }

    internal fun requestTransitionChapterLoad(chapter: ReaderChapter) = controller.requestTransitionChapterLoad(chapter)

    internal val downloadManager get() = viewModel.downloadManager

    /** Whether the text features take a tap at ([x], [y]) in the window, as translate mode does. */
    internal fun onTextTap(x: Float, y: Float): Boolean = controller.textInteraction.onTap(x, y)

    internal fun onPageShown(page: ReaderPage) = controller.onPageShown(page)

    internal fun pageRequest(): PageRequest = PageRequest(
        cropBorders = settings.cropBorders,
        // The strip reads what continues each page beside it itself, over the pages around it too (StripBackdrops)
        automaticBackground = false,
        backgroundBesidePage = false,
        layout = { _, imageSize -> stripPageLayout(imageSize, settings.bindings) },
    )

    internal fun previewLayout(imageSize: IntSize): PageImageLayout = stripPageLayout(imageSize, settings.bindings)

    internal fun pageWebView(page: ReaderPage): (() -> Unit)? =
        pageWebViewAction(page, viewModel.manga?.source, openPageInWebView)

    /** Settings that change how pages open; a change opens the pages again. */
    @Composable
    internal fun imageSettingsKey(): Any {
        val bindings = settings.bindings
        return listOf(
            bindings.cropBordersWebtoon.state.collectAsState().value.effectiveValue,
            bindings.dualPageSplitWebtoon.state.collectAsState().value.effectiveValue,
            bindings.dualPageInvertWebtoon.state.collectAsState().value.effectiveValue,
            bindings.dualPageRotateToFitWebtoon.state.collectAsState().value.effectiveValue,
            bindings.dualPageRotateToFitInvertWebtoon.state.collectAsState().value.effectiveValue,
        )
    }
}

/**
 * The last slot whose end is on screen, or one reaching past both edges: the slot being read, which a page counts as
 * once its end comes into view.
 */
private fun LazyListLayoutInfo.lastItemEndingOnScreen(): Int? = visibleItemsInfo.lastOrNull {
    it.offset + it.size <= viewportEndOffset || it.offset < viewportStartOffset
}?.index

private fun newListState(firstItem: Int) = LazyListState(firstVisibleItemIndex = firstItem)

private const val SCREEN_SCROLL_SHARE = 0.75f
private const val NANOS_PER_SECOND = 1_000_000_000.0

private const val KEY_ZOOM_STEP = 1.25f
