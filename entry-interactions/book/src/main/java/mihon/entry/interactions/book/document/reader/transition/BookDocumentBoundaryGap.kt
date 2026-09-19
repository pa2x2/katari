package mihon.entry.interactions.book.document.reader.transition

import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.calculateChapterGap

/**
 * Missing whole chapters between a boundary's two chapters, ordered so the higher number always
 * leads. Zero or negative means the endpoints are contiguous: duplicate numbering produces a
 * negative difference and counts as contiguous, matching the manga adapters' gap checks.
 */
internal fun boundaryChapterGap(transition: EntryChildTransition<EntryChapter>): Int {
    val destination = transition.to ?: return 0
    return when (transition.direction) {
        EntryChildDirection.NEXT -> calculateChapterGap(destination, transition.from)
        EntryChildDirection.PREVIOUS -> calculateChapterGap(transition.from, destination)
    }
}
