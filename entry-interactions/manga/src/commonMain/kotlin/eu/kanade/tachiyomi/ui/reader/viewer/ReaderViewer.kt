package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextSurface

/** The viewer showing the pages in one reading mode, as the reader drives it on every platform. */
internal interface ReaderViewer {
    /** Called when leaving the reader or swapping viewers. */
    fun destroy() {}

    fun setChapters(chapters: ViewerChapters)

    fun moveToPage(page: ReaderPage)

    /** Pages this viewer currently shows to the reader, for text features. */
    fun visibleTextSurfaces(): List<MangaPageTextSurface>

    /** Pages this viewer has loaded around the visible ones, for text features that work ahead of the reader. */
    fun preloadedTextSurfaces(): List<MangaPageTextSurface>

    fun supportsAutoScroll(): Boolean = false

    fun startAutoScroll(speed: Int) {}

    fun updateAutoScrollSpeed(speed: Int) {}

    fun stopAutoScroll() {}

    /** Moves a spread of two pages on by one page, so the pages pair the other way. Viewers without spreads ignore it. */
    fun shiftPages() {}
}
