package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.ui.reader.createReaderThemeContext
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.model.addPages
import eu.kanade.tachiyomi.ui.reader.viewer.calculateChapterGap
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import mihon.entry.interactions.viewer.EntryChildTransition
import tachiyomi.core.common.util.system.logcat

/**
 * Pager adapter used by this [viewer] to where [ViewerChapters] updates are posted.
 */
internal class PagerViewerAdapter(private val viewer: PagerViewer) : ViewPagerAdapter() {

    /**
     * List of currently set items.
     */
    var items: MutableList<ReaderViewerItem> = mutableListOf()
        private set

    /**
     * Holds preprocessed items so they don't get removed when changing chapter
     */
    private var preprocessed: List<InsertPage> = emptyList()

    var nextTransition: EntryChildTransition.Next<ReaderChapter>? = null
        private set

    var currentChapter: ReaderChapter? = null

    /**
     * Context that has been wrapped to use the correct theme values based on the
     * current app theme and reader background color
     */
    private var readerThemedContext = viewer.activity.createReaderThemeContext(
        readerTheme = viewer.config.theme,
        uiPreferences = viewer.activity.uiPreferences,
    )

    /**
     * Updates this adapter with the given [chapters]. It handles setting a few pages of the
     * next/previous chapter to allow seamless transitions and inverting the pages if the viewer
     * has R2L direction.
     */
    fun setChapters(chapters: ViewerChapters, forceTransition: Boolean) {
        val newItems = mutableListOf<ReaderViewerItem>()

        // Forces chapter transition if there is missing chapters
        val prevHasMissingChapters = calculateChapterGap(chapters.current, chapters.previous) > 0
        val nextHasMissingChapters = calculateChapterGap(chapters.next, chapters.current) > 0

        // Add previous chapter pages and transition
        newItems.addChapterPages(chapters.previous?.pages)

        // Skip transition page if the chapter is loaded & current page is not a transition page
        if (prevHasMissingChapters || forceTransition || chapters.previous?.state !is ReaderChapter.State.Loaded) {
            newItems.add(ReaderViewerItem.Transition(chapters.previousTransition()))
        }

        var insertPageLastPage: InsertPage? = null

        // Add current chapter.
        val currPages = chapters.current.pages
        if (currPages != null) {
            val pages = currPages.toMutableList()

            val lastPage = pages.last()

            // Insert preprocessed pages into current page list. Pages of the adjacent chapters are split too, and
            // placed by their index, so only this chapter's belong here. Paired pages are never split.
            preprocessed.filter { it.chapter.chapter.id == chapters.current.chapter.id && !viewer.showsSpreads }
                .sortedByDescending { it.index }
                .forEach { page ->
                    if (lastPage.index == page.index) {
                        insertPageLastPage = page
                    }
                    pages.add(page.index + 1, page)
                }

            newItems.addChapterPages(pages)
        }

        currentChapter = chapters.current

        // Add next chapter transition and pages.
        nextTransition = chapters.nextTransition()
            .also {
                if (
                    nextHasMissingChapters ||
                    forceTransition ||
                    chapters.next?.state !is ReaderChapter.State.Loaded
                ) {
                    newItems.add(ReaderViewerItem.Transition(it))
                }
            }

        newItems.addChapterPages(chapters.next?.pages)

        val currentItem = items.getOrNull(viewer.pager.currentItem)

        // Resets double-page splits, else insert pages get misplaced
        items.removeAll { item -> item is ReaderViewerItem.Page && item.page is InsertPage }

        if (viewer is R2LPagerViewer) {
            newItems.reverse()
        }

        val adjacentChapterIds = setOfNotNull(chapters.previous?.chapter?.id, chapters.next?.chapter?.id)
        preprocessed = preprocessed.filter { it.chapter.chapter.id in adjacentChapterIds }
        items = newItems
        notifyDataSetChanged()

        // The pager only follows the pages it has created, so before its first layout it would stay at the same
        // position while pages are added in front, e.g. the next chapter's in right to left, and land in another
        // chapter
        val currentPosition = positionOf(currentItem)
        if (currentPosition != -1 && currentPosition != viewer.pager.currentItem) {
            viewer.pager.setCurrentItem(currentPosition, false)
        }

        // Will skip insert page otherwise
        if (insertPageLastPage != null) {
            viewer.moveToPage(insertPageLastPage)
        }
    }

    private fun MutableList<ReaderViewerItem>.addChapterPages(pages: List<ReaderPage>?) {
        if (pages != null && viewer.showsSpreads) addAll(viewer.pairing.spreadsOf(pages)) else addPages(pages)
    }

    /** Where [page] shows now: on its own, beside another, or as the halves of the wide page it was split from. */
    fun positionShowing(page: ReaderPage): Int {
        val position = items.indexOfFirst { it is ReaderViewerItem.Page && it.shows(page) }
        if (position != -1 || page !is InsertPage) return position
        return positionShowing(page.parent)
    }

    /** Where what [item] showed shows now, after the pages were grouped again. */
    private fun positionOf(item: ReaderViewerItem?): Int = when (item) {
        is ReaderViewerItem.Page -> positionShowing(item.page)
        else -> items.indexOf(item)
    }

    /**
     * Returns the amount of items of the adapter.
     */
    override fun getCount(): Int {
        return items.size
    }

    /**
     * Creates a new view for the item at the given [position].
     */
    override fun createView(container: ViewGroup, position: Int): View {
        return when (val item = items[position]) {
            is ReaderViewerItem.Page if item.facing != null -> PagerSpreadHolder(readerThemedContext, viewer, item)
            is ReaderViewerItem.Page -> PagerPageHolder(readerThemedContext, viewer, item.page)
            is ReaderViewerItem.Transition -> PagerTransitionHolder(
                readerThemedContext,
                viewer,
                item.transition,
            )
        }
    }

    /**
     * Returns the current position of the given [view] on the adapter.
     */
    override fun getItemPosition(view: Any): Int {
        if (view is PositionableView) {
            val item = when (view) {
                is PagerSpreadHolder -> view.item
                is PagerPageHolder -> ReaderViewerItem.Page(view.page)
                is PagerTransitionHolder -> ReaderViewerItem.Transition(view.transition)
                else -> null
            }
            val position = item?.let(items::indexOf) ?: -1
            if (position != -1) {
                return position
            } else {
                logcat { "Position for ${view.item} not found" }
            }
        }
        return POSITION_NONE
    }

    fun onPageSplit(currentPage: ReaderPage, newPage: InsertPage) {
        val currentIndex = items.indexOf(ReaderViewerItem.Page(currentPage))

        // Put aside pages of an adjacent chapter until it becomes the current one, so they are not lost on the change
        if (currentPage.chapter.chapter.id != currentChapter?.chapter?.id) {
            val chapterId = newPage.chapter.chapter.id
            if (preprocessed.none { it.chapter.chapter.id == chapterId && it.index == newPage.index }) {
                preprocessed = preprocessed + newPage
            }
            return
        }

        val placeAtIndex = when (viewer) {
            is L2RPagerViewer,
            is VerticalPagerViewer,
            -> currentIndex + 1
            else -> currentIndex
        }

        // It will enter a endless cycle of insert pages
        if (
            viewer is R2LPagerViewer &&
            placeAtIndex - 1 >= 0 &&
            (items[placeAtIndex - 1] as? ReaderViewerItem.Page)?.page is InsertPage
        ) {
            return
        }

        // Same here it will enter a endless cycle of insert pages
        if ((items[placeAtIndex] as? ReaderViewerItem.Page)?.page is InsertPage) {
            return
        }

        items.add(placeAtIndex, ReaderViewerItem.Page(newPage))

        notifyDataSetChanged()
    }

    fun cleanupPageSplit() {
        items.removeAll { item -> item is ReaderViewerItem.Page && item.page is InsertPage }
        notifyDataSetChanged()
    }

    fun refresh() {
        readerThemedContext = viewer.activity.createReaderThemeContext(
            readerTheme = viewer.config.theme,
            uiPreferences = viewer.activity.uiPreferences,
        )
    }
}
