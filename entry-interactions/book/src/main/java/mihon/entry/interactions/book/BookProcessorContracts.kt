package mihon.entry.interactions.book

import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailure
import mihon.book.api.BookLocator
import mihon.book.api.BookPublication
import java.io.File

internal interface BookContentSession : AutoCloseable {
    val descriptor: BookContentDescriptor
    val publicationId: String
    val revision: String

    suspend fun materializePrimaryResource(): Result<MaterializedBookResource>
}

internal interface MaterializedBookResource : AutoCloseable {
    val file: File
}

internal interface BookProcessor {
    fun supports(descriptor: BookContentDescriptor): Boolean

    suspend fun open(content: BookContentSession): BookOpenResult
}

internal sealed interface BookOpenResult {
    data class Success(val session: BookPublicationSession) : BookOpenResult
    data class Failure(val failure: BookFailure) : BookOpenResult
}

internal interface BookPublicationSession : AutoCloseable {
    val publication: BookPublication

    fun validate(locator: BookLocator): Boolean
}
