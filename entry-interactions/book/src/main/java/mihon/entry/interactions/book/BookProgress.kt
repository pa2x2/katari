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
        return media.progressIdentity(chapter.id)
    }
}

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
