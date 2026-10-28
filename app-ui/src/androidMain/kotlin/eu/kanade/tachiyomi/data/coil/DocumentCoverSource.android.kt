package eu.kanade.tachiyomi.data.coil

import androidx.core.net.toUri
import coil3.request.Options
import com.hippo.unifile.UniFile
import okio.BufferedSource
import okio.buffer
import okio.source

internal actual fun openDocumentCover(options: Options, uri: String): BufferedSource =
    UniFile.fromUri(options.context, uri.toUri())!!
        .openInputStream()
        .source()
        .buffer()
