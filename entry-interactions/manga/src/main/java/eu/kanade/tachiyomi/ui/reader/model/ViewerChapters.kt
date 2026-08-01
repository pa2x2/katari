package eu.kanade.tachiyomi.ui.reader.model

import mihon.entry.interactions.viewer.EntryChildTransition
import mihon.entry.interactions.viewer.EntryChildWindow
import mihon.entry.interactions.viewer.entryChildTransitionItemAtAnchor

internal typealias ViewerChapters = EntryChildWindow<ReaderChapter>

internal sealed interface ReaderViewerItem {
    data class Page(val page: ReaderPage) : ReaderViewerItem

    data class Transition(
        val transition: EntryChildTransition<ReaderChapter>,
    ) : ReaderViewerItem
}

internal fun ReaderViewerItem?.automaticTransitionLoadDestination(): ReaderChapter? =
    (this as? ReaderViewerItem.Transition)
        ?.transition
        ?.to
        ?.takeIf { it.state == ReaderChapter.State.Wait }

internal fun automaticTransitionLoadItemAtAnchor(
    centeredItem: ReaderViewerItem?,
    firstVisibleItem: ReaderViewerItem?,
    lastVisibleItem: ReaderViewerItem?,
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
): ReaderViewerItem.Transition? {
    return entryChildTransitionItemAtAnchor(
        centeredItem = centeredItem,
        firstVisibleItem = firstVisibleItem,
        lastVisibleItem = lastVisibleItem,
        canScrollBackward = canScrollBackward,
        canScrollForward = canScrollForward,
        transitionOf = { (it as? ReaderViewerItem.Transition)?.transition },
        isActionable = { item, _ -> item.automaticTransitionLoadDestination() != null },
    ) as? ReaderViewerItem.Transition
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
