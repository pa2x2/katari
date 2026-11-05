package mihon.entry.interactions.book.document.reader.transition

import mihon.entry.interactions.book.document.reader.BookDocumentChapterLoadState
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.viewer.EntryChildTransition
import tachiyomi.domain.entry.model.EntryChapter

/**
 * True when when-needed mode drops the boundary for seamless reading: a populated boundary whose
 * destination document is already prepared, contiguous and not failed. Terminal boundaries,
 * unprepared or failed destinations and chapter gaps keep the full card so the reader still
 * explains what happened and settling on the row can request the destination.
 *
 * Mirrors the manga adapters, which omit the transition slot once the neighbor chapter is loaded
 * and contiguous.
 */
internal fun isSeamlessWhenNeededBoundary(
    displayMode: ChapterTransitionMode,
    transition: EntryChildTransition<EntryChapter>,
    isDestinationPrepared: Boolean,
    destinationLoadState: BookDocumentChapterLoadState?,
): Boolean {
    if (displayMode != ChapterTransitionMode.WHEN_NEEDED) return false
    if (transition.to == null) return false
    if (!isDestinationPrepared) return false
    if (destinationLoadState is BookDocumentChapterLoadState.Failed) return false
    return boundaryChapterGap(transition) <= 0
}
