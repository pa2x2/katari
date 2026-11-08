package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.model.automaticTransitionLoadDestination
import eu.kanade.tachiyomi.ui.reader.screen.MangaReaderController
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderViewer
import eu.kanade.tachiyomi.ui.reader.viewer.key.ReaderKeyCommand
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageRequest
import eu.kanade.tachiyomi.ui.reader.viewer.page.pageWebViewAction
import eu.kanade.tachiyomi.ui.reader.viewer.tap.ReaderTapZones
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.interactions.manga.reader.image.layout.PageImageLayout
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextSurface
import mihon.entry.interactions.manga.reader.text.surface.ViewerTextSurfaces
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.ReaderPageLayout
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import tachiyomi.core.common.util.system.logcat

/** The page that became current and whether reading moved forward to it. */
internal data class PagerSelection(val page: ReaderPage, val forward: Boolean)

/**
 * The paged reading modes: one page, or two facing pages while the window shows two panes, per slot, turning
 * [direction]. It groups the chapters' pages into slots, follows the page the reader settles on, turns pages from taps,
 * keys and the wheel, and pans a zoomed page before turning it when navigation asks to. What it draws is [Content].
 * [openPageInWebView] opens a page's image address where the platform can, or is null where it can't.
 */
internal class PagedViewer(
    private val controller: MangaReaderController,
    val direction: PagerDirection,
    private val openPageInWebView: ((url: String, sourceId: Long?) -> Unit)?,
) : ReaderViewer {
    private val viewModel get() = controller.viewModel

    val settings = PagerSettings(viewModel.readerSettings, direction)

    val tapZones = ReaderTapZones(settings.bindings)

    private val pairing = PagerSpreadPairing()
    private val pagerItems = PagerItems(direction, pairing)

    var items by mutableStateOf<List<ReaderViewerItem>>(emptyList())
        private set

    /**
     * Replaced once, when the first chapter is set, by one that starts at the page reading starts from: a pager moved
     * there before its first layout would report the first slot as settled on until then.
     */
    var pagerState by mutableStateOf(PagerState(currentPage = 0) { items.size })
        private set

    var selection by mutableStateOf<PagerSelection?>(null)
        private set

    /** Whether the window shows two panes, which Content reports. */
    private var usesTwoPanes = false

    /** Whether the pager is wider than tall, which Content reports; the automatic background has no gradient then. */
    internal var landscape = false

    /** The scope of Content's composition, which page turns animate in. */
    internal var composeScope: CoroutineScope? = null

    private var chapters: ViewerChapters? = null
    private var awaitingIdleChapters: ViewerChapters? = null
    private var spreadsGrouped = false
    private var laidOut = false
    private var currentItem: ReaderViewerItem? = null
    private val zooms = mutableMapOf<ReaderPage, PagerPageZoom>()
    private val spreads = mutableStateMapOf<ReaderPage, SpreadPlacements>()

    private val currentIndex: Int get() = pagerState.currentPage

    val vertical: Boolean get() = direction == PagerDirection.Vertical

    val showsSpreads: Boolean
        get() = direction != PagerDirection.Vertical &&
            settings.pageLayout == ReaderPageLayout.AUTOMATIC &&
            usesTwoPanes

    internal val textSurfaces = ViewerTextSurfaces(decorations = { viewModel.textSession.decoration(it) })

    override fun visibleTextSurfaces(): List<MangaPageTextSurface> = textSurfaces.visible()

    override fun preloadedTextSurfaces(): List<MangaPageTextSurface> = textSurfaces.preloaded()

    /** A page started or stopped showing an image, so text features look at the pages again. */
    internal fun onTextSurfacesChanged() = controller.onPageTextSurfacesChanged()

    override fun setChapters(chapters: ViewerChapters) {
        // Grouping the pages again while a page turns would jump to another page
        if (pagerState.isScrollInProgress) {
            awaitingIdleChapters = chapters
        } else {
            setChaptersInternal(chapters)
        }
    }

    /** Called by Content once a page turn has settled. */
    internal fun onIdle() {
        awaitingIdleChapters?.let {
            awaitingIdleChapters = null
            setChaptersInternal(it)
        }
    }

    private fun setChaptersInternal(chapters: ViewerChapters) {
        this.chapters = chapters
        val spreads = showsSpreads
        if (spreads != spreadsGrouped) {
            spreadsGrouped = spreads
            viewModel.onShowsTwoPagesChanged(spreads)
        }
        val shownBefore = items.getOrNull(currentIndex)
        val forceTransition = when (settings.chapterTransitionMode) {
            ChapterTransitionMode.ALWAYS -> true
            ChapterTransitionMode.WHEN_NEEDED -> shownBefore is ReaderViewerItem.Transition
            // Hidden mode drops the transition slot as soon as the destination chapter is loaded, so the slot the
            // reader is parked on turns into pages instead of waiting for another swipe
            ChapterTransitionMode.HIDDEN -> false
        }
        val insertedLastPage = pagerItems.setChapters(chapters, forceTransition, spreads)
        items = pagerItems.items

        // The pager itself only applies a move on its next layout, so the slot to report is worked out here
        val shown = if (!laidOut) {
            val pages = chapters.current.pages ?: return
            laidOut = true
            val start = pagerItems.positionShowing(pages[minOf(chapters.current.requestedPage, pages.lastIndex)])
                .coerceAtLeast(0)
            pagerState = PagerState(currentPage = start) { items.size }
            start
        } else {
            // The pager follows the slot shown by its key; this also finds a page that now shows within another slot
            val position = pagerItems.positionOf(shownBefore).takeIf { it != -1 } ?: currentIndex
            if (position != currentIndex) pagerState.requestScrollToPage(position)
            position
        }
        onPageChange(shown)
        insertedLastPage?.let(::moveToPage)
    }

    /** Called by Content when the window starts or stops showing two panes, or the series' page layout changed. */
    internal fun onSpreadConditionsChanged(usesTwoPanes: Boolean) {
        this.usesTwoPanes = usesTwoPanes
        if (showsSpreads == spreadsGrouped) return
        // Chapters waiting for the pager to settle are the newer ones
        val chapters = awaitingIdleChapters ?: chapters ?: return
        awaitingIdleChapters = null
        setChaptersInternal(chapters)
    }

    /** Called by Content when the reader settles on the slot at [position] by turning to it. */
    internal fun onPageSettled(position: Int) {
        if (items.getOrNull(position) != currentItem && !controller.isNavigatingFromControls) {
            controller.setMenuVisibility(false)
        }
        onPageChange(position)
    }

    private fun onPageChange(position: Int) {
        val item = items.getOrNull(position) ?: return
        if (currentItem == item) return
        val page = (item as? ReaderViewerItem.Page)?.page
        val currentPage = (currentItem as? ReaderViewerItem.Page)?.page
        val currentTransition = (currentItem as? ReaderViewerItem.Transition)?.transition
        val forward = when {
            // Both halves of a split page have its number; the inserted half always comes second
            currentPage != null && page != null && page.number == currentPage.number -> page is InsertPage
            currentPage != null && page != null -> page.number > currentPage.number
            currentTransition?.direction == EntryChildDirection.PREVIOUS && page != null -> false
            else -> true
        }
        currentItem = item
        when (item) {
            is ReaderViewerItem.Page -> onReaderPageSelected(item, forward)
            is ReaderViewerItem.Transition -> onTransitionSelected(item.transition)
        }
    }

    private fun onReaderPageSelected(item: ReaderViewerItem.Page, forward: Boolean) {
        val page = item.page
        page.chapter.pageLoader?.selectPage(page)
        controller.onPageSelected(page, item.facing)
        selection = PagerSelection(page, forward)
    }

    private fun onTransitionSelected(transition: EntryChildTransition<ReaderChapter>) {
        logcat { "onTransitionSelected: $transition" }
        val toChapter = ReaderViewerItem.Transition(transition).automaticTransitionLoadDestination()
        if (toChapter != null) {
            controller.requestTransitionChapterLoad(toChapter)
        } else if (transition.to == null && transition.direction == EntryChildDirection.NEXT) {
            // No more chapters: the reader is probably about to close it
            controller.setMenuVisibility(true)
        }
    }

    override fun moveToPage(page: ReaderPage) {
        val position = pagerItems.positionShowing(page)
        if (position == -1) {
            logcat { "Page $page not found in the pager" }
            return
        }
        if (position == currentIndex) {
            onPageChange(position)
        } else {
            scrollTo(position, animate = true)
        }
    }

    fun moveToNext() = if (direction == PagerDirection.RightToLeft) moveLeft() else moveRight()

    fun moveToPrevious() = if (direction == PagerDirection.RightToLeft) moveRight() else moveLeft()

    fun moveUp() = moveToPrevious()

    fun moveDown() = moveToNext()

    fun moveRight() {
        if (currentIndex >= items.lastIndex) return
        val zoom = panTarget { it.canPanHorizontally(1) }
        if (zoom != null && settings.navigateToPan) {
            composeScope?.launch { zoom.panScreen(1) }
        } else {
            scrollTo(currentIndex + 1, animate = settings.pageTransitions)
        }
    }

    fun moveLeft() {
        if (currentIndex <= 0) return
        val zoom = panTarget { it.canPanHorizontally(-1) }
        if (zoom != null && settings.navigateToPan) {
            composeScope?.launch { zoom.panScreen(-1) }
        } else {
            scrollTo(currentIndex - 1, animate = settings.pageTransitions)
        }
    }

    fun toggleMenu() = controller.toggleMenu()

    override fun onPageKey(command: ReaderKeyCommand) {
        tapZones.hide()
        when (command) {
            ReaderKeyCommand.PageRight -> moveRight()
            ReaderKeyCommand.PageLeft -> moveLeft()
            ReaderKeyCommand.NextPage -> moveToNext()
            ReaderKeyCommand.PreviousPage -> moveToPrevious()
            ReaderKeyCommand.ZoomIn -> zoomShown(KEY_ZOOM_STEP)
            ReaderKeyCommand.ZoomOut -> zoomShown(1f / KEY_ZOOM_STEP)
            ReaderKeyCommand.ZoomFit -> zoomShown(null)
            else -> Unit
        }
    }

    /** Zooms the pages shown about their middle by [factor], or back to fitting them when it is null. */
    private fun zoomShown(factor: Float?) {
        val item = items.getOrNull(currentIndex) as? ReaderViewerItem.Page ?: return
        listOfNotNull(item.page, item.facing).mapNotNull(zooms::get).forEach { zoom ->
            val middle = Offset(zoom.slotSize.width / 2f, zoom.slotSize.height / 2f)
            zoom.zoomBy(factor ?: (zoom.fittedScale / zoom.scale), middle)
        }
        if (factor != null) onPageZoomed()
    }

    private fun scrollTo(position: Int, animate: Boolean) {
        val scope = composeScope
        if (scope == null || !animate) {
            pagerState.requestScrollToPage(position)
            return
        }
        scope.launch { pagerState.animateScrollToPage(position) }
    }

    /** The page shown now that can pan the way [canPan] asks, a zoomed one first when two pages show. */
    private fun panTarget(canPan: (PagerPageZoom) -> Boolean): PagerPageZoom? {
        val item = items.getOrNull(currentIndex) as? ReaderViewerItem.Page ?: return null
        return listOfNotNull(item.page, item.facing)
            .mapNotNull(zooms::get)
            .filter(canPan)
            .maxByOrNull { it.isZoomed }
    }

    internal fun registerZoom(page: ReaderPage, zoom: PagerPageZoom) {
        zooms[page] = zoom
    }

    internal fun unregisterZoom(page: ReaderPage, zoom: PagerPageZoom) {
        if (zooms[page] === zoom) zooms.remove(page)
    }

    internal fun zoomOf(page: ReaderPage): PagerPageZoom? = zooms[page]

    /** The slot shown now. */
    internal val currentSlot: ReaderViewerItem? get() = items.getOrNull(currentIndex)

    internal fun onPageZoomed() = controller.setMenuVisibility(false)

    /** A right click on [page] at [windowPosition], which opens its actions there whatever the long press setting. */
    internal fun onPageSecondaryClick(page: ReaderPage, windowPosition: Offset) =
        controller.onPageLongTap(page, menuAt = windowPosition)

    /** A long press on [page], which opens its actions when the setting allows or the menu is open. */
    internal fun onPageLongPress(page: ReaderPage): Boolean {
        if (!settings.longTapEnabled && !viewModel.state.value.menuVisible) return false
        controller.onPageLongTap(page)
        return true
    }

    internal fun registerSpread(page: ReaderPage, spread: SpreadPlacements) {
        spreads[page] = spread
    }

    internal fun unregisterSpread(page: ReaderPage, spread: SpreadPlacements) {
        if (spreads[page] === spread) spreads.remove(page)
    }

    override fun spreadPageMiddles(page: ReaderPage): Pair<Float, Float>? = spreads[page]?.middles

    internal fun requestTransitionChapterLoad(chapter: ReaderChapter) = controller.requestTransitionChapterLoad(chapter)

    internal val downloadManager get() = viewModel.downloadManager

    /** Whether the text features take a tap at ([x], [y]) in the window, as translate mode does. */
    internal fun onTextTap(x: Float, y: Float): Boolean = controller.textInteraction.onTap(x, y)

    /** What opening [page]'s image needs now: the settings, and the layout rules that report on the pages around it. */
    internal fun pageRequest(): PageRequest = PageRequest(
        cropBorders = settings.cropBorders,
        automaticBackground = settings.automaticBackground,
        landscape = landscape,
        layout = ::layoutAndReport,
    )

    private fun layoutAndReport(page: ReaderPage, imageSize: IntSize): PageImageLayout {
        val result = pagerPageLayout(page, imageSize, direction, settings.spreadSettings(showsSpreads))
        if (result.isWideBesideOther) onWidePage(page)
        if (result.needsSecondHalf) onPageSplit(page)
        return result.layout
    }

    /** How [page] previews before its image is ready, by the same rules, without reporting on other pages. */
    internal fun previewLayout(page: ReaderPage, imageSize: IntSize): PageImageLayout =
        pagerPageLayout(page, imageSize, direction, settings.spreadSettings(showsSpreads)).layout

    /** Opens [page]'s image address in a web view, where the platform can and the page has one on the web. */
    internal fun pageWebView(page: ReaderPage): (() -> Unit)? =
        pageWebViewAction(page, viewModel.manga?.source, openPageInWebView)

    private fun onWidePage(page: ReaderPage) {
        // Only a wide page shown beside another moves; one already alone is shown whole as it is
        val shownBeside = items.any { it is ReaderViewerItem.Page && it.facing != null && it.shows(page) }
        if (pairing.markWide(page) && shownBeside) regroup()
    }

    private fun onPageSplit(page: ReaderPage) {
        if (pagerItems.onPageSplit(page, InsertPage(page))) items = pagerItems.items
    }

    override fun shiftPages() {
        val item = items.getOrNull(currentIndex) as? ReaderViewerItem.Page ?: return
        if (showsSpreads && pairing.shiftAt(item.page)) regroup()
    }

    private fun regroup() {
        chapters?.let(::setChapters)
    }

    /** Settings that change how pages open; a change opens the pages again. */
    @Composable
    internal fun imageSettingsKey(): Any {
        val bindings = settings.bindings
        return listOf(
            bindings.cropBorders.state.collectAsState().value.effectiveValue,
            bindings.readerTheme.state.collectAsState().value.effectiveValue,
            bindings.dualPageSplitPaged.state.collectAsState().value.effectiveValue,
            bindings.dualPageInvertPaged.state.collectAsState().value.effectiveValue,
            bindings.dualPageRotateToFit.state.collectAsState().value.effectiveValue,
            bindings.dualPageRotateToFitInvert.state.collectAsState().value.effectiveValue,
        )
    }

    @Composable
    internal fun startPositionState(): PageStartPosition {
        settings.bindings.zoomStart.state.collectAsState().value
        return settings.startPosition
    }

    /** Called by Content when splitting is turned off, which drops the split halves. */
    internal fun onSplitTurnedOff() {
        pagerItems.cleanupPageSplit()
        items = pagerItems.items
    }
}

private const val KEY_ZOOM_STEP = 1.25f
