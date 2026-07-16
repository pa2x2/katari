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
    private var preprocessed: MutableMap<Int, InsertPage> = mutableMapOf()

    var nextTransition: EntryChildTransition.Next<ReaderChapter>? = null
        private set

    var currentChapter: ReaderChapter? = null

    /**
     * Context that has been wrapped to use the correct theme values based on the
     * current app theme and reader background color
     */
    private var readerThemedContext = viewer.activity.createReaderThemeContext()

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
        newItems.addPages(chapters.previous?.pages)

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

            // Insert preprocessed pages into current page list
            preprocessed.keys.sortedDescending()
                .forEach { key ->
                    if (lastPage.index == key) {
                        insertPageLastPage = preprocessed[key]
                    }
                    preprocessed[key]?.let { pages.add(key + 1, it) }
                }

            newItems.addPages(pages)
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

        newItems.addPages(chapters.next?.pages)

        // Resets double-page splits, else insert pages get misplaced
        items.removeAll { item -> item is ReaderViewerItem.Page && item.page is InsertPage }

        if (viewer is R2LPagerViewer) {
            newItems.reverse()
        }

        preprocessed = mutableMapOf()
        items = newItems
        notifyDataSetChanged()

        // Will skip insert page otherwise
        if (insertPageLastPage != null) {
            viewer.moveToPage(insertPageLastPage)
        }
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

        // Put aside preprocessed pages for next chapter so they don't get removed when changing chapter
        if (currentPage.chapter.chapter.id != currentChapter?.chapter?.id) {
            preprocessed[newPage.index] = newPage
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
        readerThemedContext = viewer.activity.createReaderThemeContext()
    }
}
