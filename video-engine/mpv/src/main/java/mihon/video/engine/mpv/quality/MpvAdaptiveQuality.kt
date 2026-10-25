package mihon.video.engine.mpv.quality

import mihon.video.engine.mpv.tracks.MpvTrack
import mihon.video.engine.mpv.tracks.MpvTrackKind

/*
 * Auto quality on mpv. mpv picks a stream's variant when it opens it and never switches, so Katari picks: from the
 * throughput the proxy measures (MpvBandwidthMeter) and how far ahead mpv has buffered, by ExoPlayer's adaptive rule.
 * It steps up only with enough buffered to ride out a slower start of the better variant, and keeps a variant the
 * network can't sustain only while the buffer lasts.
 */

/** The `hls-bitrate` a stream opens with: the highest variant the estimate affords, so mpv starts on it. */
internal fun initialBitrateCap(bitrateEstimate: Long?): Long =
    ((bitrateEstimate ?: DEFAULT_BITRATE_ESTIMATE) * BANDWIDTH_FRACTION).toLong()

/**
 * The video track Auto plays next, or null when the stream has no variants to switch between. Variants are the video
 * tracks with the playing one's codec, since switching codecs means a new decoder.
 */
internal fun adaptiveVideoTrack(tracks: List<MpvTrack>, bitrateEstimate: Long?, bufferedAheadMs: Long): Int? {
    val playing = tracks.firstOrNull { it.kind == MpvTrackKind.Video && it.selected } ?: return null
    val variants = tracks
        .filter {
            it.kind == MpvTrackKind.Video && !it.external && !it.albumart && !it.image &&
                it.codec == playing.codec
        }
        .filter { it.variantBitrate > 0 }
        .sortedBy { it.variantBitrate }
    if (variants.size < 2 || playing !in variants) return null
    val estimate = bitrateEstimate ?: return playing.id
    val affordable = estimate * BANDWIDTH_FRACTION
    val ideal = variants.lastOrNull { it.variantBitrate <= affordable } ?: variants.first()
    return when {
        ideal.variantBitrate > playing.variantBitrate && bufferedAheadMs < MIN_BUFFER_TO_STEP_UP_MS -> playing.id
        ideal.variantBitrate < playing.variantBitrate && bufferedAheadMs >= MAX_BUFFER_TO_STEP_DOWN_MS -> playing.id
        else -> ideal.id
    }
}

private val MpvTrack.variantBitrate: Int get() = hlsBitrate ?: 0

// ExoPlayer's AdaptiveTrackSelection and DefaultBandwidthMeter values.
private const val BANDWIDTH_FRACTION = 0.7
private const val MIN_BUFFER_TO_STEP_UP_MS = 10_000L
private const val MAX_BUFFER_TO_STEP_DOWN_MS = 25_000L
private const val DEFAULT_BITRATE_ESTIMATE = 1_000_000L
