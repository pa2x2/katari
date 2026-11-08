package eu.kanade.tachiyomi.ui.reader.viewer.strip

import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.model.addPages
import eu.kanade.tachiyomi.ui.reader.viewer.calculateChapterGap

/**
 * What the long strip shows, top to bottom: the current chapter's pages with the loaded pages of the chapters on either
 * side, and the transitions between them. [forceTransition] keeps the transitions to loaded neighbours; they show
 * anyway when chapters are missing between or the neighbour hasn't loaded.
 */
internal fun stripItems(chapters: ViewerChapters, forceTransition: Boolean): List<ReaderViewerItem> = buildList {
    val prevHasMissingChapters = calculateChapterGap(chapters.current, chapters.previous) > 0
    val nextHasMissingChapters = calculateChapterGap(chapters.next, chapters.current) > 0

    addPages(chapters.previous?.pages)
    if (prevHasMissingChapters || forceTransition || chapters.previous?.state !is ReaderChapter.State.Loaded) {
        add(ReaderViewerItem.Transition(chapters.previousTransition()))
    }
    addPages(chapters.current.pages)
    if (nextHasMissingChapters || forceTransition || chapters.next?.state !is ReaderChapter.State.Loaded) {
        add(ReaderViewerItem.Transition(chapters.nextTransition()))
    }
    addPages(chapters.next?.pages)
}
