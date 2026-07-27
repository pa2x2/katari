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
    val centeredTransition = centeredItem as? ReaderViewerItem.Transition
    if (centeredTransition != null && (canScrollBackward || canScrollForward)) {
        return centeredTransition.takeIf { it.automaticTransitionLoadDestination() != null }
    }

    val boundaryItems = when {
        !canScrollBackward && !canScrollForward -> listOf(firstVisibleItem, lastVisibleItem)
        !canScrollBackward -> listOf(firstVisibleItem)
        !canScrollForward -> listOf(lastVisibleItem)
        else -> emptyList()
    }
    return boundaryItems.firstNotNullOfOrNull { item ->
        (item as? ReaderViewerItem.Transition)
            ?.takeIf { it.automaticTransitionLoadDestination() != null }
    } ?: centeredTransition?.takeIf { it.automaticTransitionLoadDestination() != null }
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
