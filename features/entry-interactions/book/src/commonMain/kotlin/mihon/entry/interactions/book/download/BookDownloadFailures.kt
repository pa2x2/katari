package mihon.entry.interactions.book.download

import eu.kanade.tachiyomi.network.HttpException
import mihon.entry.interactions.book.content.BookResourceUnavailableException
import java.io.IOException

internal class BookPackageIntegrityException(message: String?, cause: Throwable) : IOException(message, cause)

internal class BookResourceValidationException(message: String?, cause: Throwable) : IOException(message, cause)

internal class BookResourceDownloadException(message: String?, cause: Throwable) : IOException(message, cause)

internal class BookResourceBudgetException(message: String) : IOException(message)

/**
 * Whether retrying can't fetch the resource either: the host refused or lost it, the source doesn't offer it,
 * or its bytes aren't usable. Network failures, timeouts and server errors are transient.
 */
internal fun IOException.isPermanentResourceFailure(): Boolean = when (this) {
    is BookResourceValidationException -> true
    is BookResourceDownloadException -> generateSequence(cause) { it.cause }.any { error ->
        when (error) {
            is HttpException -> error.code in 400..499 && error.code !in TRANSIENT_CLIENT_ERROR_CODES
            is NoSuchElementException, is BookResourceUnavailableException -> true
            else -> false
        }
    }
    else -> false
}

private val TRANSIENT_CLIENT_ERROR_CODES = setOf(408, 425, 429)
