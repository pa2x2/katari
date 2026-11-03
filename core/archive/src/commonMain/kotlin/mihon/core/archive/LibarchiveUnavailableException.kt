package mihon.core.archive

import java.io.IOException

/** Opening an archive failed because the system has no libarchive this app can load. */
class LibarchiveUnavailableException(cause: Throwable) : IOException("libarchive is not available", cause)
