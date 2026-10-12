package eu.kanade.tachiyomi.ui.reader.loader.scheduling

import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

/**
 * Loads ahead of the reading position in the chapter being read, and only there: once reading moves into another
 * chapter, the previous chapter stops loading the pages left around its old position.
 */
internal class ReaderReadAhead(
    private val networkPolicy: LoadAheadNetworkPolicy,
) {
    private var loader: PageLoader? = null

    fun moveTo(page: ReaderPage, onlyOverWifi: Boolean) {
        val pageLoader = page.chapter.pageLoader ?: return
        if (pageLoader !== loader) {
            loader?.stopReadingAhead()
            loader = pageLoader
        }
        pageLoader.readAhead(page, wholeChapter = networkPolicy.allows(onlyOverWifi))
    }
}
