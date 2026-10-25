package mihon.video.engine.mpv.proxy.hls

import mihon.video.engine.mpv.quality.adaptiveVariant

/** What plays from a set of variants: Auto's choice, one variant, or the tallest within a height. */
internal sealed interface HlsVariantPick {
    data object Auto : HlsVariantPick

    data class Fixed(val variant: Int) : HlsVariantPick

    data class MaxHeight(val height: Int) : HlsVariantPick
}

/**
 * A set of variants mpv plays as one ([findHlsVariantSets]), and which of them each segment came from. Each segment's
 * variant is chosen when mpv asks for the segment, so a change takes effect at the next segment mpv buffers, behind
 * the ones it has, as ExoPlayer changes variants. [programId] is the set's number among the variants mpv opened.
 *
 * Called from the proxy's threads and the player's.
 */
internal class HlsVariantSet(
    val programId: Int,
    /** From the lowest bandwidth up. */
    val variants: List<HlsLoadedVariant>,
) {
    private val bitrates = variants.map { it.variant.bandwidth }
    private val segmentStartsMs = variants.first().playlist.segments
        .runningFold(0.0) { start, segment -> start + segment.durationSeconds * MS_PER_SECOND }
        .map { it.toLong() }
    private val chosen = IntArray(variants.first().playlist.segments.size) { NONE }
    private var lastChosen = NONE

    val segmentCount: Int get() = chosen.size

    /**
     * The variant segment [index] comes from. A request that [continues] the segment, such as a resume after a dropped
     * connection, gets the bytes of the variant it started on.
     */
    @Synchronized
    fun variantForSegment(
        index: Int,
        continues: Boolean,
        pick: HlsVariantPick,
        bitrateEstimate: Long?,
        bufferedAheadMs: Long,
    ): Int {
        if (continues && chosen[index] != NONE) return chosen[index]
        val variant = when (pick) {
            HlsVariantPick.Auto -> adaptiveVariant(
                bitrates,
                lastChosen.takeIf { it != NONE },
                bitrateEstimate,
                bufferedAheadMs,
            )
            is HlsVariantPick.Fixed -> pick.variant.coerceIn(variants.indices)
            is HlsVariantPick.MaxHeight ->
                variants.indices.lastOrNull { (variants[it].variant.height ?: 0) <= pick.height } ?: 0
        }
        chosen[index] = variant
        lastChosen = variant
        return variant
    }

    /** The variant playing at [positionMs]: the one its segment came from, or null before mpv asked for it. */
    @Synchronized
    fun variantAt(positionMs: Long): Int? {
        val index = segmentStartsMs.binarySearch(positionMs).let { if (it >= 0) it else -it - 2 }
        return chosen.getOrNull(index.coerceAtMost(chosen.lastIndex))?.takeIf { it != NONE }
    }

    private companion object {
        const val NONE = -1
        const val MS_PER_SECOND = 1000.0
    }
}
