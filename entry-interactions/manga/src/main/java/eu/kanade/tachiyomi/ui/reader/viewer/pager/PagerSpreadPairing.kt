package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem

/**
 * Which pages face each other while the pager shows two at once, the way a printed volume pairs them. A chapter's
 * first page, usually a cover or title page, stands alone. A wide image is a two-page spread in one file: it stands
 * alone, whole, and pairing starts again after it. Images are only known to be wide once loaded, so pages are regrouped
 * as they are found. Shifting at a page flips whether it stands alone, which moves the pairing by one page from there
 * to the end of the chapter, for chapters these rules get wrong (an inserted credits page, say).
 *
 * Kept for as long as the reader is open, not saved: wide pages are found again on the next read, and a shift is for
 * one reading of a chapter.
 */
internal class PagerSpreadPairing {
    private val widePages = mutableSetOf<PageKey>()
    private val shiftedPages = mutableSetOf<PageKey>()

    /** Notes [page] as a wide image; returns whether that is news, so pages must be regrouped. */
    fun markWide(page: ReaderPage): Boolean = widePages.add(page.key)

    fun isWide(page: ReaderPage): Boolean = page.key in widePages

    /** Flips whether [page] stands alone, unless it already does by being wide. */
    fun shiftAt(page: ReaderPage): Boolean {
        if (isWide(page)) return false
        val key = page.key
        if (!shiftedPages.remove(key)) shiftedPages.add(key)
        return true
    }

    /** [pages] of one chapter in reading order, grouped into what shows at once. */
    fun spreadsOf(pages: List<ReaderPage>): List<ReaderViewerItem.Page> {
        // Two pages show instead of a split wide page's halves
        val whole = pages.filter { it !is InsertPage }
        val spreads = mutableListOf<ReaderViewerItem.Page>()
        var i = 0
        while (i < whole.size) {
            val page = whole[i]
            val next = whole.getOrNull(i + 1)
            if (next == null || standsAlone(page) || isWide(next)) {
                spreads += ReaderViewerItem.Page(page)
                i += 1
            } else {
                spreads += ReaderViewerItem.Page(page, facing = next)
                i += 2
            }
        }
        return spreads
    }

    private fun standsAlone(page: ReaderPage): Boolean =
        isWide(page) || (page.index == 0) != (page.key in shiftedPages)

    private data class PageKey(val chapterId: Long?, val index: Int)

    private val ReaderPage.key: PageKey
        get() = PageKey(chapter.chapter.id, index)
}
