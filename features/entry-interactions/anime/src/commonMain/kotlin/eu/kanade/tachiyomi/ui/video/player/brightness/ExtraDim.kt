package eu.kanade.tachiyomi.ui.video.player.brightness

import kotlin.math.pow

/**
 * Extra dim: the brightness levels below 0, where a dark layer over the picture makes it darker than the screen's
 * lowest brightness, as volume boost makes it louder than the device's full volume.
 */
internal object ExtraDim {
    const val LEVELS = 50

    /** The darkest the layer goes, the share of the picture's light it takes away at the deepest level. */
    private const val DARKEST = 0.9f

    /**
     * The layer's share at [level], from [atZero] at 0 down to [DARKEST]. Each level takes the same share of the light
     * that is left, so every step looks as large as the last, where an even share of the whole would make the first
     * steps barely visible and the last ones jumps.
     */
    fun pictureDim(level: Int, atZero: Float): Float {
        val depth = (-level).coerceIn(0, LEVELS) / LEVELS.toFloat()
        val lightAtZero = 1f - atZero
        return 1f - lightAtZero * ((1f - DARKEST) / lightAtZero).pow(depth)
    }
}
