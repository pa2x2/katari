package mihon.video.engine.mpv.quality

import mihon.video.engine.mpv.tracks.MpvTrack
import mihon.video.engine.mpv.tracks.MpvTrackKind

/*
 * Auto quality on mpv, by ExoPlayer's adaptive rule: from the throughput the proxy measures (MpvBandwidthMeter) and
 * how far ahead mpv has buffered. It steps up only with enough buffered to ride out a slower start of the better
 * variant, and keeps a variant the network can't sustain only while the buffer lasts.
 *
 * Variants that can switch at any segment are chosen per segment by the proxy (HlsVariantSet), as ExoPlayer chooses
 * its next chunk. mpv picks a stream's variant when it opens it and never switches, so between the rest Katari
 * switches mpv's video track, which costs a seek.
 */

/** The `hls-bitrate` a stream opens with: the highest variant the estimate affords, so mpv starts on it. */
internal fun initialBitrateCap(bitrateEstimate: Long?): Long =
    ((bitrateEstimate ?: DEFAULT_BITRATE_ESTIMATE) * BANDWIDTH_FRACTION).toLong()

/**
 * The variant of [bitrates] (ascending, in bits per second) to play next, given the [current] one. Without a current
 * one, as at the start or after a seek, it is the highest the estimate affords, from a default estimate when nothing
 * was measured yet.
 */
internal fun adaptiveVariant(bitrates: List<Long>, current: Int?, bitrateEstimate: Long?, bufferedAheadMs: Long): Int {
    if (current == null) return idealVariant(bitrates, bitrateEstimate ?: DEFAULT_BITRATE_ESTIMATE)
    val estimate = bitrateEstimate ?: return current
    val ideal = idealVariant(bitrates, estimate)
    return when {
        bitrates[ideal] > bitrates[current] && bufferedAheadMs < MIN_BUFFER_TO_STEP_UP_MS -> current
        bitrates[ideal] < bitrates[current] && bufferedAheadMs >= MAX_BUFFER_TO_STEP_DOWN_MS -> current
        else -> ideal
    }
}

private fun idealVariant(bitrates: List<Long>, estimate: Long): Int {
    val affordable = estimate * BANDWIDTH_FRACTION
    return bitrates.indexOfLast { it <= affordable }.coerceAtLeast(0)
}

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
    if (bitrateEstimate == null) return playing.id
    val bitrates = variants.map { it.variantBitrate.toLong() }
    return variants[adaptiveVariant(bitrates, variants.indexOf(playing), bitrateEstimate, bufferedAheadMs)].id
}

private val MpvTrack.variantBitrate: Int get() = hlsBitrate ?: 0

// ExoPlayer's AdaptiveTrackSelection and DefaultBandwidthMeter values.
private const val BANDWIDTH_FRACTION = 0.7
private const val MIN_BUFFER_TO_STEP_UP_MS = 10_000L
private const val MAX_BUFFER_TO_STEP_DOWN_MS = 25_000L
private const val DEFAULT_BITRATE_ESTIMATE = 1_000_000L
