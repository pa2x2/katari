package eu.kanade.tachiyomi.ui.reader.model

import mihon.entry.interactions.viewer.EntryChildTransition
import mihon.entry.interactions.viewer.EntryChildWindow

internal typealias ViewerChapters = EntryChildWindow<ReaderChapter>

internal sealed interface ReaderViewerItem {
    data class Page(val page: ReaderPage) : ReaderViewerItem

    data class Transition(
        val transition: EntryChildTransition<ReaderChapter>,
    ) : ReaderViewerItem
}

internal fun MutableList<ReaderViewerItem>.addPages(pages: List<ReaderPage>?) {
    pages?.forEach { page ->
        add(ReaderViewerItem.Page(page))
    }
}

internal fun ViewerChapters.ref() {
    current.ref()
    previous?.ref()
    next?.ref()
}

internal fun ViewerChapters.unref() {
    current.unref()
    previous?.unref()
    next?.unref()
}
