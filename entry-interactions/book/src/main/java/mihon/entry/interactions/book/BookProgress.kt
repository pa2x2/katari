package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryMedia
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

internal const val BOOK_PROGRESS_LOCATOR_KIND = "book"

internal data class BookProgressIdentity(
    val contentKey: String,
    val resourceKey: String,
    val resourceRevision: String?,
)

internal class BookProgressIdentityResolver(
    private val getEntryWithChapters: GetEntryWithChapters,
    private val sourceManager: SourceManager,
) {
    suspend fun resolve(chapter: EntryChapter): BookProgressIdentity {
        val owner = getEntryWithChapters.awaitEntry(chapter.entryId)
        owner.requireBook()
        val source = sourceManager.get(owner.source)
            ?: error("BOOK source ${owner.source} is not available")
        val media = source.getMedia(chapter.toSEntryChapter()) as? EntryMedia.Book
            ?: error("BOOK source returned non-book media for child ${chapter.id}")
        val resourceKey = media.initialResourceId
            ?: media.catalog.resources.singleOrNull()?.id
            ?: error("BOOK child ${chapter.id} does not identify one publication resource")
        val resourceRevision = media.catalog.resources
            .firstOrNull { it.id == resourceKey }
            ?.revision
            ?: media.publicationRevision

        return BookProgressIdentity(
            contentKey = media.publicationKeyOverride.orEmpty(),
            resourceKey = resourceKey,
            resourceRevision = resourceRevision,
        )
    }
}
