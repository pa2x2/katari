package mihon.video.engine.mpv.proxy

import java.io.ByteArrayOutputStream

/*
 * The parts of a URI the proxy reads from the text the downloader stored and the references downloaded playlists hold,
 * read as Android's Uri reads them.
 */

/** Everything before the first colon; null for a URI without one, such as a relative reference. */
internal fun schemeOf(uri: String): String? = uri.indexOf(':').takeIf { it >= 0 }?.let { uri.substring(0, it) }

/** The text with its `%XX` escapes decoded as UTF-8; a `%` that starts no escape becomes U+FFFD. */
internal fun String.percentDecoded(): String {
    if ('%' !in this) return this
    val decoded = StringBuilder(length)
    val bytes = ByteArrayOutputStream()
    fun flush() {
        if (bytes.size() == 0) return
        decoded.append(String(bytes.toByteArray(), Charsets.UTF_8))
        bytes.reset()
    }
    var index = 0
    while (index < length) {
        val char = this[index]
        val escaped = if (char == '%') {
            val high = getOrNull(index + 1)?.digitToIntOrNull(16)
            val low = getOrNull(index + 2)?.digitToIntOrNull(16)
            if (high != null && low != null) high * 16 + low else null
        } else {
            null
        }
        when {
            escaped != null -> {
                bytes.write(escaped)
                index += 3
            }
            char == '%' -> {
                flush()
                decoded.append('�')
                index++
            }
            else -> {
                flush()
                decoded.append(char)
                index++
            }
        }
    }
    flush()
    return decoded.toString()
}
