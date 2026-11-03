package eu.kanade.tachiyomi.data.coil

import coil3.request.Options
import okio.BufferedSource
import java.io.IOException

// Content URIs name Android documents, which only come to desktop in a backup of local source entries; their covers
// fail to load and show the placeholder.
internal actual fun openDocumentCover(options: Options, uri: String): BufferedSource =
    throw IOException("Desktop cannot open the document cover $uri")
