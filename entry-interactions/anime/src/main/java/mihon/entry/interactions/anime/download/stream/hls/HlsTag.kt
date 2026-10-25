package mihon.entry.interactions.anime.download.stream.hls

/**
 * A playlist tag, `#EXT-X-MEDIA:TYPE=AUDIO,URI="a.m3u8"`, with its attribute list split as RFC 8216 writes it: a
 * quoted value may hold commas (`CODECS="avc1.64001f,mp4a.40.2"`), so the list isn't split on every comma.
 */
internal class HlsTag private constructor(
    val name: String,
    /** The text after the colon, for a tag such as `#EXTINF:6.0,` or `#EXT-X-BYTERANGE:720@0` that has no list. */
    val value: String,
    private val attributes: List<Pair<String, String>>,
) {
    /** The attribute's value, unquoted. */
    operator fun get(key: String): String? =
        attributes.firstOrNull { it.first == key }?.second?.removeSurrounding("\"")

    /** The tag as a line, with each attribute in [changes] set to its value, quoted, or removed when it is null. */
    fun with(vararg changes: Pair<String, String?>): String {
        val changed = changes.toMap()
        val written = attributes.mapNotNull { (key, value) ->
            if (key !in changed) key to value else changed[key]?.let { key to "\"$it\"" }
        }
        return "$name:" + written.joinToString(",") { (key, value) -> "$key=$value" }
    }

    companion object {
        /** The tag [line] holds, or null for a URI line, a blank line or a comment. */
        fun parse(line: String): HlsTag? {
            if (!line.startsWith("#EXT")) return null
            val name = line.substringBefore(':')
            val value = line.substringAfter(':', missingDelimiterValue = "")
            return HlsTag(name, value, parseAttributes(value))
        }
    }
}

private fun parseAttributes(text: String): List<Pair<String, String>> {
    val attributes = ArrayList<Pair<String, String>>()
    var index = 0
    while (index < text.length) {
        val equals = text.indexOf('=', index)
        if (equals < 0) break
        val key = text.substring(index, equals).trim()
        var end = equals + 1
        if (text.getOrNull(end) == '"') {
            end = text.indexOf('"', end + 1).let { if (it < 0) text.length else it + 1 }
        }
        end = text.indexOf(',', end).let { if (it < 0) text.length else it }
        attributes += key to text.substring(equals + 1, end).trim()
        index = end + 1
    }
    return attributes
}
