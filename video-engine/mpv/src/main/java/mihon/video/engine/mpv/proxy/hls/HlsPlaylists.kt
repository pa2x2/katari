package mihon.video.engine.mpv.proxy.hls

/*
 * The parts of HLS playlists (RFC 8216) that switching variants per segment depends on. Lines and tags it doesn't
 * know are kept as they are by the playlists built from these, or make a variant unfit for switching.
 */

/** A variant of a master playlist: an `EXT-X-STREAM-INF` tag, at line [line], and its URI, as written. */
internal data class HlsVariant(
    val line: Int,
    val uriLine: Int,
    val uri: String,
    val attributes: Map<String, String>,
) {
    /** Peak bits per second. */
    val bandwidth: Long get() = attributes["BANDWIDTH"]?.toLongOrNull() ?: 0L
    val averageBandwidth: Long? get() = attributes["AVERAGE-BANDWIDTH"]?.toLongOrNull()

    /** The `CODECS` entries, or null when the tag doesn't say. */
    val codecs: List<String>? get() = attributes["CODECS"]?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)
    val width: Int? get() = resolution?.first
    val height: Int? get() = resolution?.second
    val frameRate: Double? get() = attributes["FRAME-RATE"]?.toDoubleOrNull()

    private val resolution: Pair<Int, Int>?
        get() = attributes["RESOLUTION"]?.split('x')?.takeIf { it.size == 2 }?.let { (w, h) ->
            val width = w.toIntOrNull() ?: return null
            val height = h.toIntOrNull() ?: return null
            width to height
        }
}

internal data class HlsMasterPlaylist(val lines: List<String>, val variants: List<HlsVariant>)

/** [text] as a master playlist, or null when it lists no variants, as a media playlist doesn't. */
internal fun parseHlsMasterPlaylist(text: String): HlsMasterPlaylist? {
    val lines = text.split('\n').map { it.removeSuffix("\r") }
    val variants = buildList {
        var pending: Pair<Int, Map<String, String>>? = null
        lines.forEachIndexed { index, raw ->
            val line = raw.trim()
            when {
                line.startsWith(STREAM_INF) -> pending = index to parseAttributes(line.removePrefix(STREAM_INF))
                line.isEmpty() || line.startsWith("#") -> Unit
                else -> pending?.let { (tagLine, attributes) ->
                    add(HlsVariant(tagLine, index, line, attributes))
                    pending = null
                }
            }
        }
    }
    return HlsMasterPlaylist(lines, variants).takeIf { variants.isNotEmpty() }
}

/**
 * A segment of a media playlist. [keyTag] is the `EXT-X-KEY` tag in effect for it, as written, null when it isn't
 * encrypted; [byteRange] is the part of [uri] it is, when it isn't all of it.
 */
internal data class HlsSegment(
    val uri: String,
    val durationSeconds: Double,
    val title: String,
    val byteRange: LongRange?,
    val keyTag: String?,
    val discontinuity: Boolean,
    val programDateTime: String?,
)

/**
 * A media playlist. [unfit] names what keeps its segments from being served as another variant's: a tag this doesn't
 * carry over, such as an fMP4 initialization section, which FFmpeg reads only once, or a live playlist's reloads.
 */
internal data class HlsMediaPlaylist(
    val version: Int?,
    val targetDurationSeconds: Int,
    val mediaSequence: Long,
    val discontinuitySequence: Long,
    val independentSegments: Boolean,
    val ended: Boolean,
    val segments: List<HlsSegment>,
    val unfit: String?,
)

internal fun parseHlsMediaPlaylist(text: String): HlsMediaPlaylist {
    var version: Int? = null
    var targetDuration = 0
    var mediaSequence = 0L
    var discontinuitySequence = 0L
    var independentSegments = false
    var ended = false
    var unfit: String? = null
    var keyTag: String? = null
    val segments = mutableListOf<HlsSegment>()
    // Tags that apply to the next segment only.
    var duration: Double? = null
    var title = ""
    var byteRange: LongRange? = null
    var discontinuity = false
    var programDateTime: String? = null
    // Where the next byte range of each URI starts when its tag gives no offset.
    val nextOffsets = mutableMapOf<String, Long>()
    var pendingByteRange: Pair<Long, Long?>? = null

    for (raw in text.split('\n')) {
        val line = raw.trim()
        when {
            line.isEmpty() -> Unit
            line.startsWith("#EXTINF:") -> {
                val value = line.removePrefix("#EXTINF:")
                duration = value.substringBefore(',').trim().toDoubleOrNull()
                title = value.substringAfter(',', "")
            }
            line.startsWith("#EXT-X-VERSION:") -> version = line.substringAfter(':').trim().toIntOrNull()
            line.startsWith("#EXT-X-TARGETDURATION:") ->
                targetDuration = line.substringAfter(':').trim().toIntOrNull() ?: 0
            line.startsWith("#EXT-X-MEDIA-SEQUENCE:") ->
                mediaSequence = line.substringAfter(':').trim().toLongOrNull() ?: 0L
            line.startsWith("#EXT-X-DISCONTINUITY-SEQUENCE:") ->
                discontinuitySequence = line.substringAfter(':').trim().toLongOrNull() ?: 0L
            line == "#EXT-X-INDEPENDENT-SEGMENTS" -> independentSegments = true
            line == "#EXT-X-ENDLIST" -> ended = true
            line == "#EXT-X-DISCONTINUITY" -> discontinuity = true
            line.startsWith("#EXT-X-PROGRAM-DATE-TIME:") -> programDateTime = line.substringAfter(':')
            line.startsWith("#EXT-X-KEY:") ->
                keyTag = line.takeIf { parseAttributes(line.removePrefix("#EXT-X-KEY:"))["METHOD"] != "NONE" }
            line.startsWith("#EXT-X-BYTERANGE:") -> {
                val value = line.substringAfter(':').trim()
                val length = value.substringBefore('@').toLongOrNull()
                if (length == null) {
                    unfit = unfit ?: "a malformed byte range"
                } else {
                    pendingByteRange = length to value.substringAfter('@', "").toLongOrNull()
                }
            }
            line.startsWith("#EXT-X-MAP:") -> unfit = unfit ?: "an initialization section (fMP4)"
            line.startsWith("#EXT-X-PART") || line.startsWith("#EXT-X-PRELOAD-HINT") ||
                line.startsWith("#EXT-X-SERVER-CONTROL") -> unfit = unfit ?: "low-latency parts"
            line.startsWith("#EXT-X-GAP") -> unfit = unfit ?: "a gap"
            line.startsWith("#") -> Unit
            else -> {
                val segmentDuration = duration
                if (segmentDuration == null) {
                    unfit = unfit ?: "a segment without a duration"
                } else {
                    pendingByteRange?.let { (length, offset) ->
                        val start = offset ?: nextOffsets[line] ?: 0L
                        byteRange = start until start + length
                        nextOffsets[line] = start + length
                    }
                    segments +=
                        HlsSegment(line, segmentDuration, title, byteRange, keyTag, discontinuity, programDateTime)
                }
                duration = null
                title = ""
                byteRange = null
                pendingByteRange = null
                discontinuity = false
                programDateTime = null
            }
        }
    }
    if (!ended) unfit = unfit ?: "a live playlist"
    return HlsMediaPlaylist(
        version,
        targetDuration,
        mediaSequence,
        discontinuitySequence,
        independentSegments,
        ended,
        segments,
        unfit,
    )
}

/** An attribute list, `NAME=value,NAME="quoted, value"`, with the quotes taken off. */
internal fun parseAttributes(text: String): Map<String, String> = buildMap {
    ATTRIBUTE.findAll(text).forEach { match ->
        val value = match.groupValues[2]
        put(match.groupValues[1], value.removeSurrounding("\""))
    }
}

private const val STREAM_INF = "#EXT-X-STREAM-INF:"
private val ATTRIBUTE = Regex("""([A-Z0-9-]+)=("[^"]*"|[^",]*)""")
