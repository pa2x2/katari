package eu.kanade.tachiyomi.data.track.url

/**
 * Builds a tracker API or login address on [base] as Android's `Uri.Builder` does, so services keep receiving the
 * addresses they always have: path segments and query parameters are percent-encoded as UTF-8 except for letters,
 * digits and `_-!.~'()*`. OkHttp's `HttpUrl` encodes `!'()~` too and leaves `:`, `@` and others in paths.
 */
internal class TrackerUrlBuilder(base: String) {

    private val path = StringBuilder(base)
    private val query = StringBuilder()

    init {
        require('?' !in base && '#' !in base) { "The base of a tracker URL has no query or fragment: $base" }
    }

    fun appendPath(segment: String) = apply {
        if (!path.endsWith('/')) path.append('/')
        path.append(encode(segment))
    }

    fun appendQueryParameter(name: String, value: String) = apply {
        if (query.isNotEmpty()) query.append('&')
        query.append(encode(name)).append('=').append(encode(value))
    }

    fun build(): String = if (query.isEmpty()) path.toString() else "$path?$query"

    private fun encode(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val char = (byte.toInt() and 0xFF).toChar()
            if (char.isUnreserved()) {
                append(char)
            } else {
                append('%').append(HEX_DIGITS[byte.toInt() shr 4 and 0xF]).append(HEX_DIGITS[byte.toInt() and 0xF])
            }
        }
    }

    private fun Char.isUnreserved(): Boolean =
        this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this in UNRESERVED_MARKS

    private companion object {
        const val HEX_DIGITS = "0123456789ABCDEF"
        const val UNRESERVED_MARKS = "_-!.~'()*"
    }
}
