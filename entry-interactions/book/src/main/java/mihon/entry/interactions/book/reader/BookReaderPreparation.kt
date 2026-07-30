package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailure
import mihon.entry.interactions.book.download.VerifiedBookDownloadPackage
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal data class PreparedBookReaderRequest(
    val request: BookReaderRequest,
    val visibleEntry: Entry,
    val owner: Entry,
    val chapter: EntryChapter,
    val content: PreparedBookContent,
)

internal sealed interface PreparedBookContent {
    val descriptor: BookContentDescriptor

    fun progressIdentity(chapterId: Long): BookProgressIdentity

    data class Source(
        val source: UnifiedSource,
        val media: EntryMedia.Book,
    ) : PreparedBookContent {
        override val descriptor: BookContentDescriptor = media.descriptor

        override fun progressIdentity(chapterId: Long): BookProgressIdentity = media.progressIdentity(chapterId)
    }

    data class Downloaded(
        val download: VerifiedBookDownloadPackage,
    ) : PreparedBookContent {
        override val descriptor: BookContentDescriptor = download.manifest.descriptor

        override fun progressIdentity(chapterId: Long): BookProgressIdentity =
            download.manifest.progressIdentity(chapterId)
    }
}

internal sealed interface BookReaderPrepareResult {
    data class Success(val request: PreparedBookReaderRequest) : BookReaderPrepareResult
    data class Failure(val failure: BookFailure) : BookReaderPrepareResult
}

internal sealed interface BookReaderOpenResult {
    data class Success(val session: OpenedBookReaderSession) : BookReaderOpenResult
    data class Failure(val failure: BookFailure) : BookReaderOpenResult
}
