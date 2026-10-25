package mihon.video.engine.mpv.proxy.hls

import okhttp3.HttpUrl
import kotlin.math.abs

/*
 * Variants that can take each other's place at any segment, so the proxy switches between them as ExoPlayer does:
 * mpv opens one variant for each set ([HlsVariantSet]) and every segment it asks for comes from the variant chosen for
 * it then, with the segments before it still buffered. Switching mpv's own tracks instead costs a seek that drops the
 * buffer.
 *
 * For mpv not to notice, the variants must be the same stream in another quality:
 * - the same codecs, apart from the video's profile and level, which the decoder takes in its stride. Two audio
 *   codecs, such as AAC-LC and HE-AAC, don't mix: mpv misreads the audio's timestamps after the change;
 * - the same audio, subtitle and caption renditions;
 * - TS segments: an fMP4 variant has its own initialization section, which FFmpeg reads only for the first segment;
 * - finished (VOD) playlists whose segments start at the same times, with the same sequence numbers, discontinuities
 *   and keys.
 */

/** A variant of the master playlist with its media playlist, fetched from [url] after redirects. */
internal data class HlsLoadedVariant(val variant: HlsVariant, val url: HttpUrl, val playlist: HlsMediaPlaylist)

/** The sets of variants of [variants] that can switch at any segment, each from the lowest bandwidth up. */
internal fun findHlsVariantSets(variants: List<HlsLoadedVariant>): List<List<HlsLoadedVariant>> =
    variants
        .filter { it.playlist.unfit == null && it.playlist.segments.isNotEmpty() }
        .groupBy { it.variant.switchKey() }
        .filterKeys { it != null }
        .values
        .mapNotNull { group ->
            val sorted = group.sortedBy { it.variant.bandwidth }
            val reference = sorted.first()
            sorted.filter { it === reference || it.alignsWith(reference) }.takeIf { it.size >= 2 }
        }

/** The variants of [master] that might switch with another at any segment, by what their tags say. */
internal fun hlsVariantSetCandidates(master: HlsMasterPlaylist): List<HlsVariant> =
    master.variants
        .groupBy { it.switchKey() }
        .filterKeys { it != null }
        .values
        .filter { it.size >= 2 }
        .flatten()

/** What variants must share to switch at any segment, or null when the variant doesn't say what it holds. */
private data class SwitchKey(
    val videoCodec: String?,
    val audioCodecs: List<String>,
    val audio: String?,
    val subtitles: String?,
    val closedCaptions: String?,
)

private fun HlsVariant.switchKey(): SwitchKey? {
    val codecs = codecs ?: return null
    val (video, audio) = codecs.partition { codec -> VIDEO_CODEC_FAMILIES.keys.any { codec.startsWith(it) } }
    val videoFamilies = video.map { codec -> VIDEO_CODEC_FAMILIES.entries.first { codec.startsWith(it.key) }.value }
    if (videoFamilies.size > 1) return null
    return SwitchKey(
        videoCodec = videoFamilies.singleOrNull(),
        audioCodecs = audio.sorted(),
        audio = attributes["AUDIO"],
        subtitles = attributes["SUBTITLES"],
        closedCaptions = attributes["CLOSED-CAPTIONS"],
    )
}

private fun HlsLoadedVariant.alignsWith(reference: HlsLoadedVariant): Boolean {
    val own = playlist
    val other = reference.playlist
    if (own.segments.size != other.segments.size || own.mediaSequence != other.mediaSequence ||
        own.discontinuitySequence != other.discontinuitySequence
    ) {
        return false
    }
    var ownStart = 0.0
    var otherStart = 0.0
    own.segments.zip(other.segments).forEach { (segment, referenceSegment) ->
        if (segment.discontinuity != referenceSegment.discontinuity) return false
        if (abs(ownStart - otherStart) > MAX_START_DIFFERENCE_SECONDS) return false
        if (resolvedKey(segment.keyTag, url) != resolvedKey(referenceSegment.keyTag, reference.url)) return false
        ownStart += segment.durationSeconds
        otherStart += referenceSegment.durationSeconds
    }
    return true
}

/** A key tag's attributes with its URI resolved, which is the same key whichever playlist names it. */
private fun resolvedKey(keyTag: String?, base: HttpUrl): Map<String, String>? =
    keyTag?.let { tag ->
        val attributes = parseAttributes(tag.removePrefix("#EXT-X-KEY:"))
        attributes + listOfNotNull(attributes["URI"]?.let { "URI" to (base.resolve(it)?.toString() ?: it) })
    }

/**
 * The master playlist mpv gets: each set's variants become the set's lowest one, with its tag as written and [setUrl]
 * of the set as its URI, and every other reference goes through [route] ([rewriteHlsPlaylist]). ffmpeg numbers the
 * variants it opens in order, as mpv's tracks report in `program-ids`; the result maps each set to its number.
 */
internal fun hlsMasterWithVariantSets(
    master: HlsMasterPlaylist,
    sets: List<List<HlsLoadedVariant>>,
    setUrl: (Int) -> String,
    route: (String) -> String?,
): Pair<String, List<Int>> {
    val dropped = sets.flatMap { set -> set.drop(1).flatMap { listOf(it.variant.line, it.variant.uriLine) } }.toSet()
    val setOfUriLine = sets.withIndex().associate { (index, set) -> set.first().variant.uriLine to index }
    val kept = master.variants.filter { it.line !in dropped }
    val programIds = sets.map { set -> kept.indexOfFirst { it.line == set.first().variant.line } }
    val text = master.lines.withIndex()
        .filter { (index, _) -> index !in dropped }
        .joinToString("\n") { (index, line) ->
            setOfUriLine[index]?.let(setUrl) ?: rewriteHlsPlaylist(line, route)
        }
    return text to programIds
}

/**
 * The media playlist of [set] mpv gets: the segments of its lowest variant, timed as that variant times them, each
 * at [segmentUrl], where the proxy serves it from the variant chosen for it. Keys go through [route] from where the
 * lowest variant names them; the variants share them.
 */
internal fun hlsVariantSetPlaylist(
    set: List<HlsLoadedVariant>,
    segmentUrl: (Int) -> String,
    route: (HttpUrl, String) -> String?,
): String {
    val reference = set.first()
    val playlist = reference.playlist
    return buildString {
        appendLine("#EXTM3U")
        set.mapNotNull { it.playlist.version }.maxOrNull()?.let { appendLine("#EXT-X-VERSION:$it") }
        appendLine("#EXT-X-TARGETDURATION:${set.maxOf { it.playlist.targetDurationSeconds }}")
        appendLine("#EXT-X-MEDIA-SEQUENCE:${playlist.mediaSequence}")
        if (playlist.discontinuitySequence != 0L) {
            appendLine("#EXT-X-DISCONTINUITY-SEQUENCE:${playlist.discontinuitySequence}")
        }
        appendLine("#EXT-X-PLAYLIST-TYPE:VOD")
        if (set.all { it.playlist.independentSegments }) appendLine("#EXT-X-INDEPENDENT-SEGMENTS")
        var keyTag: String? = null
        playlist.segments.forEachIndexed { index, segment ->
            if (segment.discontinuity) appendLine("#EXT-X-DISCONTINUITY")
            if (segment.keyTag != keyTag) {
                appendLine(
                    segment.keyTag?.let { tag -> rewriteHlsPlaylist(tag) { route(reference.url, it) } }
                        ?: "#EXT-X-KEY:METHOD=NONE",
                )
                keyTag = segment.keyTag
            }
            segment.programDateTime?.let { appendLine("#EXT-X-PROGRAM-DATE-TIME:$it") }
            appendLine("#EXTINF:${segment.durationSeconds},${segment.title}")
            appendLine(segmentUrl(index))
        }
        appendLine("#EXT-X-ENDLIST")
    }
}

// CODECS entries that are video, by the family a decoder takes in its stride; anything else is audio.
private val VIDEO_CODEC_FAMILIES = mapOf(
    "avc1" to "avc",
    "avc3" to "avc",
    "hvc1" to "hevc",
    "hev1" to "hevc",
    "dvh1" to "dolby-vision",
    "dvhe" to "dolby-vision",
    "av01" to "av1",
    "vp09" to "vp9",
    "vp8" to "vp8",
    "mp4v" to "mpeg4",
)

/** How far apart the variants' segments may start, as their durations are rounded to frames. */
private const val MAX_START_DIFFERENCE_SECONDS = 0.5
