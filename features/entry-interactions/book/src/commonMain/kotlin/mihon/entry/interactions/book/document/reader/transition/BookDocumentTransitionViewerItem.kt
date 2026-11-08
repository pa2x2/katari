package mihon.entry.interactions.book.document.reader.transition

import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.viewer.EntryChildTransition

/**
 * The stable lazy-list row identity of a rendered chapter boundary. A populated boundary renders
 * one shared row between its chapters: NEXT(A,B) and PREVIOUS(B,A) produce the same key. A
 * terminal boundary renders its own row per direction so "no previous" and "no next" stay distinct.
 */
internal fun <T, K> EntryChildTransition<T>.toViewerItem(
    keyOf: (T) -> K,
): BookDocumentViewerItem.Transition<T> {
    val fromKey = keyOf(from).toString()
    val toKey = to?.let(keyOf)?.toString()
    val key = if (toKey == null) {
        "document-transition:$direction:$fromKey:terminal"
    } else {
        "document-transition:${listOf(fromKey, toKey).sorted().joinToString(":")}"
    }
    return BookDocumentViewerItem.Transition(this, key)
}
