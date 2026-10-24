package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.VideoStreamType

/** What a stream's first bytes say it is; [mimeType] names the container of a progressive file. */
internal data class SniffedContent(val type: VideoStreamType, val mimeType: String? = null)

/**
 * What a stream is, from the first bytes it served and its Content-Type, or null when neither says. The bytes come
 * first: hosts serve HLS playlists as `audio/mpegurl` or `application/octet-stream` and DASH manifests as `text/plain`,
 * so the Content-Type decides only when the bytes are no known format. An HTML page is never media, whatever its
 * Content-Type claims: it is an error or anti-bot page.
 */
internal fun sniffStreamContent(prefix: ByteArray, contentType: String?): SniffedContent? {
    val text = prefix.leadingText()
    when {
        text.startsWith("#EXTM3U") -> return SniffedContent(VideoStreamType.HLS)
        text.startsWith("<") -> {
            val root = text.xmlRootName()
            return when {
                root == null -> null
                root.substringAfter(':') == "MPD" -> SniffedContent(VideoStreamType.DASH)
                else -> null
            }
        }
    }
    containerOf(prefix)?.let { return SniffedContent(VideoStreamType.PROGRESSIVE, it) }

    val type = contentType.orEmpty().substringBefore(';').trim().lowercase()
    return when {
        "mpegurl" in type -> SniffedContent(VideoStreamType.HLS)
        "dash+xml" in type -> SniffedContent(VideoStreamType.DASH)
        type.startsWith("video/") || type.startsWith("audio/") -> SniffedContent(VideoStreamType.PROGRESSIVE, type)
        else -> null
    }
}

/** The prefix as text from its first non-blank character, past a UTF-8 byte order mark; Latin-1 keeps every byte. */
private fun ByteArray.leadingText(): String {
    val start = if (size >= 3 && this[0] == 0xEF.toByte() && this[1] == 0xBB.toByte() &&
        this[2] == 0xBF.toByte()
    ) {
        3
    } else {
        0
    }
    return String(this, start, size - start, Charsets.ISO_8859_1).trimStart()
}

/** The name of the document's root element, past the XML declaration, comments and a doctype; null for HTML. */
private fun String.xmlRootName(): String? {
    var rest = this
    while (true) {
        rest = rest.trimStart()
        rest = when {
            rest.startsWith("<?") -> rest.substringAfter("?>", missingDelimiterValue = "")
            rest.startsWith("<!--") -> rest.substringAfter("-->", missingDelimiterValue = "")
            rest.startsWith("<!DOCTYPE", ignoreCase = true) ->
                if (rest.startsWith("<!DOCTYPE html", ignoreCase = true)) return null else rest.substringAfter('>', "")
            rest.startsWith("<") -> {
                val name = ROOT_NAME.find(rest)?.groupValues?.get(1) ?: return null
                return name.takeUnless { it.equals("html", ignoreCase = true) }
            }
            else -> return null
        }
    }
}

private val ROOT_NAME = Regex("""^<([A-Za-z_][\w.:-]*)""")

/** The MIME type of the media container [prefix] starts with, or null when it is none Katari knows. */
private fun containerOf(prefix: ByteArray): String? {
    fun at(index: Int) = prefix.getOrNull(index)?.toInt()?.and(0xFF) ?: -1
    fun ascii(offset: Int, value: String) =
        prefix.size >= offset + value.length && value.indices.all { at(offset + it) == value[it].code }

    return when {
        MP4_BOXES.any { ascii(4, it) } -> "video/mp4"
        at(0) == 0x1A && at(1) == 0x45 && at(2) == 0xDF && at(3) == 0xA3 ->
            if (String(prefix, Charsets.ISO_8859_1).contains("webm")) "video/webm" else "video/x-matroska"
        at(0) == TS_SYNC && (prefix.size <= TS_PACKET || at(TS_PACKET) == TS_SYNC) -> "video/mp2t"
        ascii(0, "FLV") -> "video/x-flv"
        ascii(0, "RIFF") && ascii(8, "AVI ") -> "video/x-msvideo"
        ascii(0, "OggS") -> "video/ogg"
        ASF_HEADER.indices.all { at(it) == ASF_HEADER[it] } -> "video/x-ms-asf"
        ascii(0, "ID3") -> "audio/mpeg"
        at(0) == 0xFF && (at(1) and 0xF6) == 0xF0 -> "audio/aac"
        at(0) == 0xFF && (at(1) and 0xE0) == 0xE0 -> "audio/mpeg"
        else -> null
    }
}

private val MP4_BOXES = listOf("ftyp", "styp", "moov", "moof", "free", "skip", "mdat", "sidx", "wide")
private val ASF_HEADER = intArrayOf(0x30, 0x26, 0xB2, 0x75, 0x8E, 0x66, 0xCF, 0x11)
private const val TS_SYNC = 0x47
private const val TS_PACKET = 188
