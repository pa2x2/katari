package mihon.entry.interactions.book.state

import eu.kanade.tachiyomi.source.entry.EntryMedia
import tachiyomi.domain.entry.model.EntryProgressState

internal const val BOOK_PROGRESS_LOCATOR_KIND = "book"

internal val EntryProgressState.hasPartialBookProgress: Boolean
    get() = !completed && locator.kind == BOOK_PROGRESS_LOCATOR_KIND && !locator.isEmpty

internal data class BookProgressIdentity(
    val contentKey: String,
    val resourceKey: String,
    val resourceRevision: String?,
)

internal fun EntryMedia.Book.progressIdentity(chapterId: Long): BookProgressIdentity {
    val resourceKey = initialResourceId
        ?: catalog.resources.singleOrNull()?.id
        ?: error("BOOK child $chapterId does not identify one publication resource")
    val resourceRevision = catalog.resources
        .firstOrNull { it.id == resourceKey }
        ?.revision
        ?: publicationRevision

    return BookProgressIdentity(
        contentKey = publicationKeyOverride.orEmpty(),
        resourceKey = resourceKey,
        resourceRevision = resourceRevision,
    )
}
