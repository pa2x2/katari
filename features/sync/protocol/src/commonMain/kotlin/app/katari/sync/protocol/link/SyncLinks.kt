package app.katari.sync.protocol.link

/**
 * The `katari-sync:<kind>?<query>` links that Katari's QR codes hold, such as a link code's or an invite's. A link
 * rather than plain text, so a camera app that isn't Katari's shows it as one. The query is a URL query: parameters
 * percent-encoded as UTF-8, with `+` read as a space as URL libraries write it, so links written by earlier apps
 * through OkHttp read the same.
 *
 * Later versions may add parameters; a reader ignores the ones it doesn't know.
 */
object SyncLinks {
    private const val SCHEME = "katari-sync"

    fun encode(kind: String, parameters: List<Pair<String, String>>): String =
        parameters.joinToString("&", prefix = "$SCHEME:$kind?") { (name, value) ->
            "${percentEncode(name)}=${percentEncode(value)}"
        }

    /** The parameters of [text] when it is a link of [kind], the first of any repeated one; null when it isn't. */
    fun parameters(text: String, kind: String): Map<String, String>? {
        val prefix = "$SCHEME:$kind?"
        if (!text.startsWith(prefix)) return null
        val parameters = mutableMapOf<String, String>()
        for (pair in text.removePrefix(prefix).split('&')) {
            if (pair.isEmpty()) continue
            val name = percentDecode(pair.substringBefore('=')) ?: return null
            val value = percentDecode(pair.substringAfter('=', missingDelimiterValue = "")) ?: return null
            parameters.getOrPut(name) { value }
        }
        return parameters
    }

    private fun percentEncode(text: String): String = buildString {
        for (byte in text.encodeToByteArray()) {
            val value = byte.toInt() and 0xFF
            val char = value.toChar()
            if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in UNRESERVED_MARKS) {
                append(char)
            } else {
                append('%').append(HEX_DIGITS[value shr 4]).append(HEX_DIGITS[value and 0xF])
            }
        }
    }

    /** Null when a `%` isn't followed by two hex digits. */
    private fun percentDecode(text: String): String? {
        val bytes = mutableListOf<Byte>()
        var index = 0
        while (index < text.length) {
            when (text[index]) {
                '%' -> {
                    val high = text.getOrNull(index + 1)?.digitToIntOrNull(16) ?: return null
                    val low = text.getOrNull(index + 2)?.digitToIntOrNull(16) ?: return null
                    bytes += (high * 16 + low).toByte()
                    index += 3
                }
                '+' -> {
                    bytes += ' '.code.toByte()
                    index++
                }
                else -> {
                    val end = text.indexOfAny(charArrayOf('%', '+'), index).takeIf { it >= 0 } ?: text.length
                    bytes += text.substring(index, end).encodeToByteArray().asList()
                    index = end
                }
            }
        }
        return bytes.toByteArray().decodeToString()
    }

    private const val UNRESERVED_MARKS = "-._~"
    private const val HEX_DIGITS = "0123456789ABCDEF"
}
