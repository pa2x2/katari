package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.PointF
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.viewpager.widget.ViewPager
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.model.automaticTransitionLoadDestination
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import mihon.entry.interactions.manga.R
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextSurface
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.ReaderPageLayout
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import tachiyomi.core.common.util.system.logcat
import kotlin.math.min

/**
 * Implementation of a [Viewer] to display pages with a [ViewPager].
 */
@Suppress("LeakingThis")
internal abstract class PagerViewer(val activity: ReaderActivity) : Viewer {

    internal val downloadManager: DownloadManager
        get() = activity.viewModel.downloadManager

    private val scope = MainScope()

    /**
     * View pager used by this viewer. It's abstract to implement L2R, R2L and vertical pagers on
     * top of this class.
     */
    val pager = createPager()

    /**
     * Configuration used by the pager, like allow taps, scale mode on images, page transitions...
     */
    val config = PagerConfig(this, activity.readerSettings, scope)

    val pairing = PagerSpreadPairing()

    /**
     * Whether pages pair up two at a time: horizontal pages, in a window wide enough for two panes, unless the series
     * asks for one page.
     */
    val showsSpreads: Boolean
        get() = this !is VerticalPagerViewer &&
            config.pageLayout == ReaderPageLayout.AUTOMATIC &&
            activity.windowLayout?.usesTwoPanes == true

    /** What the adapter's items were last grouped for. */
    private var spreadsGrouped = false

    /**
     * Adapter of the pager.
     */
    private val adapter = PagerViewerAdapter(this)

    /** The chapters last set, to group their pages again. */
    private var chapters: ViewerChapters? = null

    /**
     * Currently active item. It can be a chapter page or a chapter transition.
     */
    private var currentItem: ReaderViewerItem? = null

    /**
     * Viewer chapters to set when the pager enters idle mode. Otherwise, if the view was settling
     * or dragging, there'd be a noticeable and annoying jump.
     */
    private var awaitingIdleViewerChapters: ViewerChapters? = null

    /**
     * Whether the view pager is currently in idle mode. It sets the awaiting chapters if setting
     * this field to true.
     */
    private var isIdle = true
        set(value) {
            field = value
            if (value) {
                awaitingIdleViewerChapters?.let { viewerChapters ->
                    setChaptersInternal(viewerChapters)
                    awaitingIdleViewerChapters = null
                }
            }
        }

    private val pagerListener = object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            if (!activity.isNavigatingFromControls) {
                activity.hideMenu()
            }
            onPageChange(position)
        }

        override fun onPageScrollStateChanged(state: Int) {
            isIdle = state == ViewPager.SCROLL_STATE_IDLE
        }
    }

    init {
        pager.isVisible = false // Don't layout the pager yet
        pager.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        pager.isFocusable = false
        pager.offscreenPageLimit = 1
        pager.id = R.id.reader_pager
        pager.adapter = adapter
        pager.addOnPageChangeListener(pagerListener)
        pager.tapListener = tap@{ event ->
            if (activity.onTextTap(event)) return@tap
            val viewPosition = IntArray(2)
            pager.getLocationOnScreen(viewPosition)
            val viewPositionRelativeToWindow = IntArray(2)
            pager.getLocationInWindow(viewPositionRelativeToWindow)
            val pos = PointF(
                (event.rawX - viewPosition[0] + viewPositionRelativeToWindow[0]) / pager.width,
                (event.rawY - viewPosition[1] + viewPositionRelativeToWindow[1]) / pager.height,
            )
            when (config.navigator.getAction(pos)) {
                NavigationRegion.MENU -> activity.toggleMenu()
                NavigationRegion.NEXT -> moveToNext()
                NavigationRegion.PREV -> moveToPrevious()
                NavigationRegion.RIGHT -> moveRight()
                NavigationRegion.LEFT -> moveLeft()
            }
        }
        pager.longTapListener = f@{ event ->
            if (activity.viewModel.state.value.menuVisible || config.longTapEnabled) {
                val item = adapter.items.getOrNull(pager.currentItem)
                if (item is ReaderViewerItem.Page) {
                    val page = item.facing?.let { spreadHolder(item)?.pageAt(event.x) } ?: item.page
                    activity.onPageLongTap(page)
                    return@f true
                }
            }
            false
        }

        config.dualPageSplitChangedListener = { enabled ->
            if (!enabled) {
                cleanupPageSplit()
            }
        }

        config.imagePropertyChangedListener = {
            refreshAdapter()
        }

        config.pageLayoutChangedListener = {
            onSpreadConditionsChanged()
        }

        config.navigationModeChangedListener = {
            val showOnStart = config.navigationOverlayOnStart || config.forceNavigationOverlay
            activity.binding.navigationOverlay.setNavigation(config.navigator, showOnStart)
        }
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
    }

    /**
     * Creates a new ViewPager.
     */
    abstract fun createPager(): Pager

    /**
     * Returns the view this viewer uses.
     */
    override fun getView(): View {
        return pager
    }

    override fun visibleTextSurfaces(): List<MangaPageTextSurface> =
        currentPageHolders().map { it.textSurface }

    override fun preloadedTextSurfaces(): List<MangaPageTextSurface> {
        val current = adapter.items.getOrNull(pager.currentItem) as? ReaderViewerItem.Page
        return pageHolders()
            .filterNot { current?.shows(it.page) == true }
            .map { it.textSurface }
            .toList()
    }

    /** Every page view the pager holds, those of two-page slots included. */
    private fun pageHolders(): Sequence<PagerPageHolder> =
        pager.children.flatMap { view ->
            when (view) {
                is PagerPageHolder -> sequenceOf(view)
                is PagerSpreadHolder -> view.pageHolders.asSequence()
                else -> emptySequence()
            }
        }

    /**
     * Returns the PagerPageHolder for the provided page
     */
    private fun getPageHolder(page: ReaderPage): PagerPageHolder? =
        pageHolders().firstOrNull { it.item == page }

    /** The views of the pages shown now. */
    private fun currentPageHolders(): List<PagerPageHolder> {
        val item = adapter.items.getOrNull(pager.currentItem) as? ReaderViewerItem.Page ?: return emptyList()
        if (item.facing == null) return listOfNotNull(getPageHolder(item.page))
        return spreadHolder(item)?.pageHolders.orEmpty()
    }

    private fun spreadHolder(item: ReaderViewerItem.Page): PagerSpreadHolder? =
        pager.children.filterIsInstance<PagerSpreadHolder>().firstOrNull { it.item == item }

    /**
     * Called when a new page (either a [ReaderPage] or [EntryChildTransition]) is marked as active
     */
    private fun onPageChange(position: Int) {
        val item = adapter.items.getOrNull(position)
        if (item != null && currentItem != item) {
            val page = (item as? ReaderViewerItem.Page)?.page
            val currentPage = (currentItem as? ReaderViewerItem.Page)?.page
            val currentTransition = (currentItem as? ReaderViewerItem.Transition)?.transition
            val forward = when {
                currentPage != null && page != null -> {
                    // if both pages have the same number, it's a split page with an InsertPage
                    if (page.number == currentPage.number) {
                        // the InsertPage is always the second in the reading direction
                        page is InsertPage
                    } else {
                        page.number > currentPage.number
                    }
                }
                currentTransition?.direction == EntryChildDirection.PREVIOUS && page != null ->
                    false
                else -> true
            }
            currentItem = item
            when (item) {
                is ReaderViewerItem.Page -> onReaderPageSelected(item, forward)
                is ReaderViewerItem.Transition -> onTransitionSelected(item.transition)
            }
        }
    }

    /**
     * Called when a [ReaderPage] is marked as active.
     */
    private fun onReaderPageSelected(item: ReaderViewerItem.Page, forward: Boolean) {
        val page = item.page
        val pages = page.chapter.pages ?: return
        logcat { "onReaderPageSelected: ${page.number}/${pages.size}" }
        page.chapter.pageLoader?.selectPage(page)
        activity.onPageSelected(page, item.facing)

        // Notify holder of page change
        getPageHolder(page)?.onPageSelected(forward)
        item.facing?.let(::getPageHolder)?.onPageSelected(forward)
        if (item.facing != null) spreadHolder(item)?.let(::reportPageMiddles)
    }

    fun onSpreadLaidOut(holder: PagerSpreadHolder) {
        if (adapter.items.getOrNull(pager.currentItem) == holder.item) reportPageMiddles(holder)
    }

    private fun reportPageMiddles(holder: PagerSpreadHolder) {
        holder.pageMiddles()?.let { activity.onSpreadPageMiddles(holder.item.page.number, it) }
    }

    /**
     * Called when an [EntryChildTransition] is marked as active.
     */
    private fun onTransitionSelected(transition: EntryChildTransition<ReaderChapter>) {
        logcat { "onTransitionSelected: $transition" }
        val toChapter = ReaderViewerItem.Transition(transition).automaticTransitionLoadDestination()
        if (toChapter != null) {
            logcat { "Request destination chapter because its transition is active" }
            activity.requestTransitionChapterLoad(toChapter)
        } else if (transition.to == null && transition.direction == EntryChildDirection.NEXT) {
            // No more chapters, show menu because the user is probably going to close the reader
            activity.showMenu()
        }
    }

    /**
     * Tells this viewer to set the given [chapters] as active. If the pager is currently idle,
     * it sets the chapters immediately, otherwise they are saved and set when it becomes idle.
     */
    override fun setChapters(chapters: ViewerChapters) {
        if (isIdle) {
            setChaptersInternal(chapters)
        } else {
            awaitingIdleViewerChapters = chapters
        }
    }

    /**
     * Sets the active [chapters] on this pager.
     */
    private fun setChaptersInternal(chapters: ViewerChapters) {
        this.chapters = chapters
        // Remove listener so the change in item doesn't trigger it
        pager.removeOnPageChangeListener(pagerListener)
        val spreads = showsSpreads
        val spreadsChanged = spreads != spreadsGrouped
        if (spreadsChanged) {
            spreadsGrouped = spreads
            activity.viewModel.onShowsTwoPagesChanged(spreads)
        }

        val forceTransition = when (config.chapterTransitionMode) {
            ChapterTransitionMode.ALWAYS -> true
            ChapterTransitionMode.WHEN_NEEDED -> adapter.items.getOrNull(
                pager.currentItem,
            ) is ReaderViewerItem.Transition
            // Hidden mode drops the transition slot as soon as the destination chapter is loaded so
            // the slot the user is parked on turns into pages instead of waiting for another swipe.
            ChapterTransitionMode.HIDDEN -> false
        }
        adapter.setChapters(chapters, forceTransition)
        // Pages processed for one mode (split or turned when alone, whole when paired) are wrong in the other
        if (spreadsChanged && !pager.isGone) refreshAdapter()

        // Layout the pager once a chapter is being set
        if (pager.isGone) {
            logcat { "Pager first layout" }
            val pages = chapters.current.pages ?: return
            moveToPage(pages[min(chapters.current.requestedPage, pages.lastIndex)])
            pager.isVisible = true
        }

        pager.addOnPageChangeListener(pagerListener)
        // Manually call onPageChange to update the UI
        onPageChange(pager.currentItem)
    }

    /**
     * Tells this viewer to move to the given [page].
     */
    override fun moveToPage(page: ReaderPage) {
        val position = adapter.positionShowing(page)
        if (position != -1) {
            val currentPosition = pager.currentItem
            pager.setCurrentItem(position, true)
            // manually call onPageChange since ViewPager listener is not triggered in this case
            if (currentPosition == position) {
                onPageChange(position)
            }
        } else {
            logcat { "Page $page not found in adapter" }
        }
    }

    /**
     * Moves to the next page.
     */
    open fun moveToNext() {
        moveRight()
    }

    /**
     * Moves to the previous page.
     */
    open fun moveToPrevious() {
        moveLeft()
    }

    /**
     * Moves to the page at the right.
     */
    protected open fun moveRight() {
        if (pager.currentItem != adapter.count - 1) {
            val holder = panTarget { it.canPanRight() }
            if (holder != null && config.navigateToPan) {
                holder.panRight()
            } else {
                pager.setCurrentItem(pager.currentItem + 1, config.usePageTransitions)
            }
        }
    }

    /**
     * Moves to the page at the left.
     */
    protected open fun moveLeft() {
        if (pager.currentItem != 0) {
            val holder = panTarget { it.canPanLeft() }
            if (holder != null && config.navigateToPan) {
                holder.panLeft()
            } else {
                pager.setCurrentItem(pager.currentItem - 1, config.usePageTransitions)
            }
        }
    }

    /** The page shown now that can pan the way [canPan] asks, a zoomed one first when two pages show. */
    private fun panTarget(canPan: (PagerPageHolder) -> Boolean): PagerPageHolder? =
        currentPageHolders().filter(canPan).maxByOrNull { it.isZoomed() }

    /**
     * Moves to the page at the top (or previous).
     */
    protected open fun moveUp() {
        moveToPrevious()
    }

    /**
     * Moves to the page at the bottom (or next).
     */
    protected open fun moveDown() {
        moveToNext()
    }

    /**
     * Resets the adapter in order to recreate all the views. Used when a image configuration is
     * changed.
     */
    private fun refreshAdapter() {
        val currentItem = pager.currentItem
        adapter.refresh()
        pager.adapter = adapter
        pager.setCurrentItem(currentItem, false)
    }

    /**
     * Called from the containing activity when a key [event] is received. It should return true
     * if the event was handled, false otherwise.
     */
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP
        val ctrlPressed = event.metaState.and(KeyEvent.META_CTRL_ON) > 0

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!config.volumeKeysEnabled || activity.viewModel.state.value.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveDown() else moveUp()
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!config.volumeKeysEnabled || activity.viewModel.state.value.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveUp() else moveDown()
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isUp) {
                    if (ctrlPressed) moveToNext() else moveRight()
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isUp) {
                    if (ctrlPressed) moveToPrevious() else moveLeft()
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_DPAD_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_PAGE_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_PAGE_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_MENU -> if (isUp) activity.toggleMenu()
            else -> return false
        }
        return true
    }

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    override fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
            when (event.action) {
                MotionEvent.ACTION_SCROLL -> {
                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                        moveDown()
                    } else {
                        moveUp()
                    }
                    return true
                }
            }
        }
        return false
    }

    /** Called from a page view that found its image to be wide while pages pair up. */
    fun onWidePage(page: ReaderPage) {
        activity.runOnUiThread {
            // Only a wide page shown beside another moves; one already alone is shown whole as it is
            val shownBeside = adapter.items.any { it is ReaderViewerItem.Page && it.facing != null && it.shows(page) }
            if (pairing.markWide(page) && shownBeside) regroup()
        }
    }

    /** Moves the pairing by one page from the pages shown now to the end of their chapter. */
    fun shiftPages() {
        val item = adapter.items.getOrNull(pager.currentItem) as? ReaderViewerItem.Page ?: return
        if (showsSpreads && pairing.shiftAt(item.page)) regroup()
    }

    /** Called when the window or the series' page layout changes, which may pair pages up or split them apart. */
    fun onSpreadConditionsChanged() {
        if (showsSpreads == spreadsGrouped) {
            // A fold may have moved under pages already paired
            pager.children.filterIsInstance<PagerSpreadHolder>().forEach { it.requestLayout() }
            return
        }
        // Chapters waiting for the pager to settle are the newer ones
        val chapters = awaitingIdleViewerChapters ?: chapters ?: return
        awaitingIdleViewerChapters = null
        setChaptersInternal(chapters)
    }

    private fun regroup() {
        chapters?.let(::setChapters)
    }

    fun onPageSplit(currentPage: ReaderPage, newPage: InsertPage) {
        activity.runOnUiThread {
            // Need to insert on UI thread else images will go blank
            adapter.onPageSplit(currentPage, newPage)
        }
    }

    private fun cleanupPageSplit() {
        adapter.cleanupPageSplit()
    }
}
