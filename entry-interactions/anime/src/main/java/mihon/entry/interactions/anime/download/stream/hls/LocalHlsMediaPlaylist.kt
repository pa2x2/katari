package mihon.entry.interactions.anime.download.stream.hls

import mihon.entry.interactions.anime.download.stream.segments.SegmentFile
import okhttp3.HttpUrl

/** A media playlist that plays from the [files] beside it, under their names. */
internal class LocalHlsMediaPlaylist(val text: String, val files: List<SegmentFile>)

/**
 * The media playlist [text], fetched from [url], rewritten to play from files named after their place in it:
 * `<prefix>-<n>` for the n-th segment, `<prefix>-init<n>` and `<prefix>-key<n>` for the n-th initialization section
 * and key, each with the extension its URL has. Names follow the playlist's order rather than its URLs, which can
 * change from one fetch to the next while the playlist stays the same.
 *
 * A segment that is a byte range of a larger resource becomes a file of its own, so the playlist loses its byte
 * ranges. Timing, sequence numbers, discontinuities and encryption parameters stay as the server wrote them. Comments,
 * low-latency parts and hints go, and so does any other tag that points at the network.
 */
internal fun localHlsMediaPlaylist(text: String, url: HttpUrl, prefix: String): LocalHlsMediaPlaylist {
    val files = ArrayList<SegmentFile>()
    val keyNames = HashMap<HttpUrl, String>()
    val mapNames = HashMap<Pair<HttpUrl, LongRange?>, String>()
    var segmentCount = 0
    var pendingRange: String? = null
    // A byte range without an offset starts where the previous segment's ended, in the same resource.
    var previousSegment: SegmentFile? = null

    val lines = buildList {
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val tag = HlsTag.parse(line)
            when {
                // A comment can differ between fetches of the same playlist, and a retry compares local playlists.
                tag == null && line.startsWith("#") -> Unit
                tag == null -> {
                    val segmentUrl = url.resolve(line)
                    if (segmentUrl == null) {
                        add(line)
                    } else {
                        val previousEnd = previousSegment?.takeIf { it.url == segmentUrl }?.range?.let { it.last + 1 }
                        val segment = SegmentFile(
                            url = segmentUrl,
                            range = pendingRange?.let { byteRange(it, previousEnd) },
                            name = "$prefix-${++segmentCount}${extensionOf(segmentUrl)}",
                        )
                        files += segment
                        previousSegment = segment
                        add(segment.name)
                    }
                    pendingRange = null
                }
                tag.name == "#EXT-X-BYTERANGE" -> pendingRange = tag.value
                tag.name == "#EXT-X-MAP" -> {
                    val mapUrl = tag["URI"]?.let(url::resolve)
                    if (mapUrl == null) {
                        add(line)
                    } else {
                        // An initialization section's range without an offset starts at the resource's first byte.
                        val range = tag["BYTERANGE"]?.let { byteRange(it, previousEnd = 0L) }
                        val name = mapNames.getOrPut(mapUrl to range) {
                            "$prefix-init${mapNames.size + 1}${extensionOf(mapUrl)}"
                                .also { files += SegmentFile(it, mapUrl, range) }
                        }
                        add(tag.with("URI" to name, "BYTERANGE" to null))
                    }
                }
                tag.name == "#EXT-X-KEY" -> {
                    // A key the playlist carries itself (`data:`) or a DRM system's (`skd:`) isn't fetched.
                    val keyUrl = tag["URI"]?.let(url::resolve)
                    if (keyUrl == null) {
                        add(line)
                    } else {
                        val name = keyNames.getOrPut(keyUrl) {
                            "$prefix-key${keyNames.size + 1}${extensionOf(keyUrl)}"
                                .also { files += SegmentFile(it, keyUrl, range = null) }
                        }
                        add(tag.with("URI" to name))
                    }
                }
                tag.name in LOW_LATENCY_TAGS || tag["URI"] != null -> Unit
                else -> add(line)
            }
        }
    }
    return LocalHlsMediaPlaylist(lines.joinToString("\n", postfix = "\n"), files)
}

/** `<length>[@<offset>]`, as `EXT-X-BYTERANGE` and `BYTERANGE` write it; [previousEnd] is the default offset. */
private fun byteRange(text: String, previousEnd: Long?): LongRange {
    val length = text.substringBefore('@').trim().toLongOrNull()?.takeIf { it > 0 }
    val offset = if ('@' in text) text.substringAfter('@').trim().toLongOrNull() else previousEnd
    if (length == null || offset == null) throw UnsupportedOperationException("Byte range '$text'")
    return offset until offset + length
}

/** The extension [url]'s file name has, with its dot, or nothing for a name without one. */
private fun extensionOf(url: HttpUrl): String {
    val extension = url.pathSegments.lastOrNull().orEmpty().substringAfterLast('.', "")
    return if (extension.length in 1..5 && extension.all(Char::isLetterOrDigit)) ".${extension.lowercase()}" else ""
}

private val LOW_LATENCY_TAGS = setOf("#EXT-X-PART", "#EXT-X-PRELOAD-HINT", "#EXT-X-RENDITION-REPORT")
