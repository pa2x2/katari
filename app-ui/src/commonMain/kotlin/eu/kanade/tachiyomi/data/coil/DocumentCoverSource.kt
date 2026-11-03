package eu.kanade.tachiyomi.data.coil

import coil3.request.Options
import okio.BufferedSource

/** Opens a cover a document provider serves under a `content` URI, such as a local source entry's on Android. */
internal expect fun openDocumentCover(options: Options, uri: String): BufferedSource
