package mihon.entry.interactions.book.document.reader

import mihon.book.api.document.BookDocumentPosition

/** One explicit reader navigation that must supersede viewport-anchor preservation. */
internal data class BookDocumentNavigationRequest(
    val id: Long,
    val chapterId: Long,
    val position: BookDocumentPosition,
)

internal fun BookDocumentNavigationRequest?.acceptsLocation(
    chapterId: Long,
    position: BookDocumentPosition,
): Boolean = this == null || (this.chapterId == chapterId && this.position == position)

internal fun BookDocumentNavigationRequest?.afterAcceptedLocation(
    observedRequest: BookDocumentNavigationRequest?,
    chapterId: Long,
): BookDocumentNavigationRequest? {
    val currentRequest = this ?: return null
    return currentRequest.takeUnless {
        observedRequest != null &&
            currentRequest.id == observedRequest.id &&
            currentRequest.chapterId == chapterId
    }
}
