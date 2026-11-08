package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.model.addPages
import eu.kanade.tachiyomi.ui.reader.viewer.calculateChapterGap

/** Which way the pages turn. */
internal enum class PagerDirection { LeftToRight, RightToLeft, Vertical }

/**
 * What the pager shows, slot by slot, in layout order: the current chapter's pages with a few of the chapters on either
 * side, the transitions between them, the second halves of split wide pages, and pages paired up while two show at once.
 * Right to left, the slots run backwards so the first page sits at the right.
 */
internal class PagerItems(private val direction: PagerDirection, private val pairing: PagerSpreadPairing) {
    var items: List<ReaderViewerItem> = emptyList()
        private set

    var currentChapter: ReaderChapter? = null
        private set

    // Halves of wide pages found in an adjacent chapter, kept until that chapter becomes the current one
    private var preprocessed: List<InsertPage> = emptyList()

    /**
     * Groups [chapters] into slots. [forceTransition] keeps the transitions to loaded neighbours; they show anyway
     * when chapters are missing between or the neighbour hasn't loaded. With [showsSpreads], pages pair up and wide
     * pages are not split. Returns the inserted half to move to when the last page of the chapter was split, which the
     * pager would otherwise skip.
     */
    fun setChapters(chapters: ViewerChapters, forceTransition: Boolean, showsSpreads: Boolean): InsertPage? {
        val newItems = mutableListOf<ReaderViewerItem>()

        val prevHasMissingChapters = calculateChapterGap(chapters.current, chapters.previous) > 0
        val nextHasMissingChapters = calculateChapterGap(chapters.next, chapters.current) > 0

        newItems.addChapterPages(chapters.previous?.pages, showsSpreads)
        if (prevHasMissingChapters || forceTransition || chapters.previous?.state !is ReaderChapter.State.Loaded) {
            newItems.add(ReaderViewerItem.Transition(chapters.previousTransition()))
        }

        var insertPageLastPage: InsertPage? = null
        val currPages = chapters.current.pages
        if (currPages != null) {
            val pages = currPages.toMutableList()
            val lastPage = pages.last()
            // Pages of the adjacent chapters are split too, and placed by their index, so only this chapter's belong
            // here. Paired pages are never split.
            preprocessed.filter { it.chapter.chapter.id == chapters.current.chapter.id && !showsSpreads }
                .sortedByDescending { it.index }
                .forEach { page ->
                    if (lastPage.index == page.index) insertPageLastPage = page
                    pages.add(page.index + 1, page)
                }
            newItems.addChapterPages(pages, showsSpreads)
        }
        currentChapter = chapters.current

        val nextTransition = chapters.nextTransition()
        if (nextHasMissingChapters || forceTransition || chapters.next?.state !is ReaderChapter.State.Loaded) {
            newItems.add(ReaderViewerItem.Transition(nextTransition))
        }
        newItems.addChapterPages(chapters.next?.pages, showsSpreads)

        if (direction == PagerDirection.RightToLeft) newItems.reverse()

        val adjacentChapterIds = setOfNotNull(chapters.previous?.chapter?.id, chapters.next?.chapter?.id)
        preprocessed = preprocessed.filter { it.chapter.chapter.id in adjacentChapterIds }
        items = newItems
        return insertPageLastPage
    }

    private fun MutableList<ReaderViewerItem>.addChapterPages(pages: List<ReaderPage>?, showsSpreads: Boolean) {
        if (pages != null && showsSpreads) addAll(pairing.spreadsOf(pages)) else addPages(pages)
    }

    /** Where [page] shows now: on its own, beside another, or as the halves of the wide page it was split from. */
    fun positionShowing(page: ReaderPage): Int {
        val position = items.indexOfFirst { it is ReaderViewerItem.Page && it.shows(page) }
        if (position != -1 || page !is InsertPage) return position
        return positionShowing(page.parent)
    }

    /** Where what [item] showed shows now, after the pages were grouped again. */
    fun positionOf(item: ReaderViewerItem?): Int = when (item) {
        is ReaderViewerItem.Page -> positionShowing(item.page)
        else -> items.indexOf(item)
    }

    /**
     * Adds [newPage], the second half of the wide [currentPage], after it in reading order. Returns whether the slots
     * changed; a page of an adjacent chapter is put aside until that chapter is the current one.
     */
    fun onPageSplit(currentPage: ReaderPage, newPage: InsertPage): Boolean {
        val currentIndex = items.indexOf(ReaderViewerItem.Page(currentPage))
        if (currentIndex == -1) return false

        if (currentPage.chapter.chapter.id != currentChapter?.chapter?.id) {
            val chapterId = newPage.chapter.chapter.id
            if (preprocessed.none { it.chapter.chapter.id == chapterId && it.index == newPage.index }) {
                preprocessed = preprocessed + newPage
            }
            return false
        }

        val placeAtIndex = if (direction == PagerDirection.RightToLeft) currentIndex else currentIndex + 1
        // Splitting again what is already split would insert halves without end
        if (
            direction == PagerDirection.RightToLeft &&
            placeAtIndex - 1 >= 0 &&
            (items[placeAtIndex - 1] as? ReaderViewerItem.Page)?.page is InsertPage
        ) {
            return false
        }
        if ((items.getOrNull(placeAtIndex) as? ReaderViewerItem.Page)?.page is InsertPage) return false

        items = items.toMutableList().apply { add(placeAtIndex, ReaderViewerItem.Page(newPage)) }
        return true
    }

    /** Drops every split half, when splitting is turned off. */
    fun cleanupPageSplit() {
        items = items.filterNot { it is ReaderViewerItem.Page && it.page is InsertPage }
    }
}
