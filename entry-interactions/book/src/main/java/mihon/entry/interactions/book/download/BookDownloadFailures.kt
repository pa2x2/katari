package mihon.entry.interactions.book.download

import java.io.IOException

internal class BookPackageIntegrityException(message: String?, cause: Throwable) : IOException(message, cause)

internal class BookResourceValidationException(message: String?, cause: Throwable) : IOException(message, cause)

internal class BookResourceDownloadException(message: String?, cause: Throwable) : IOException(message, cause)

internal class BookResourceBudgetException(message: String) : IOException(message)
