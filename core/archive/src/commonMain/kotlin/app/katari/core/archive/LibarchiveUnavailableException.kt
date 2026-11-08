package app.katari.core.archive

import java.io.IOException

/** Opening an archive failed because no libarchive this app can load is available. */
class LibarchiveUnavailableException(cause: Throwable) : IOException("libarchive is not available", cause)
