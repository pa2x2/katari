package eu.kanade.tachiyomi.ui.video.player.picture

import tachiyomi.domain.entry.model.PictureFraming
import kotlin.math.abs
import kotlin.math.max

/** [scale] times the picture's fitted size, its centre [offsetX] and [offsetY] pixels from the player's. */
internal data class VideoPicturePlacement(val scale: Float, val offsetX: Float, val offsetY: Float)

/**
 * A video of [aspectRatio] in a player [width] by [height] pixels. Turns a framing into a placement, and a placement
 * from a pinch back into the framing to save, which lands on Fit or Fill when the pinch ends close to either.
 */
internal class VideoPictureGeometry(val width: Float, val height: Float, aspectRatio: Float) {
    val fitWidth: Float = if (width / height > aspectRatio) height * aspectRatio else width
    val fitHeight: Float = fitWidth / aspectRatio

    /** The scale at which the picture covers the player. */
    val fillScale: Float = max(width / fitWidth, height / fitHeight)

    fun place(framing: PictureFraming): VideoPicturePlacement = when (framing) {
        PictureFraming.Fit -> VideoPicturePlacement(1f, 0f, 0f)
        PictureFraming.Fill -> VideoPicturePlacement(fillScale, 0f, 0f)
        is PictureFraming.Zoom -> {
            val scale = framing.scale.toFloat().coerceIn(1f, MAX_SCALE)
            VideoPicturePlacement(
                scale = scale,
                offsetX = framing.panX.toFloat().coerceIn(-1f, 1f) * travelX(scale),
                offsetY = framing.panY.toFloat().coerceIn(-1f, 1f) * travelY(scale),
            )
        }
    }

    /** [scale] held to the allowed range, and pulled to Fit or Fill when it is close to either. */
    fun snapScale(scale: Float): Float {
        val held = scale.coerceIn(1f, MAX_SCALE)
        return when {
            abs(held - 1f) < SNAP_SHARE -> 1f
            abs(held - fillScale) < fillScale * SNAP_SHARE -> fillScale
            else -> held
        }
    }

    /** The placement with the picture kept over the player: it moves only as far as its scale overflows it. */
    fun clamp(placement: VideoPicturePlacement): VideoPicturePlacement {
        val travelX = travelX(placement.scale)
        val travelY = travelY(placement.scale)
        return placement.copy(
            offsetX = placement.offsetX.coerceIn(-travelX, travelX),
            offsetY = placement.offsetY.coerceIn(-travelY, travelY),
        )
    }

    fun framingOf(placement: VideoPicturePlacement): PictureFraming {
        val (scale, offsetX, offsetY) = clamp(placement)
        val panX = share(offsetX, travelX(scale))
        val panY = share(offsetY, travelY(scale))
        return when {
            scale == 1f -> PictureFraming.Fit
            scale == fillScale && abs(panX) < SNAP_SHARE && abs(panY) < SNAP_SHARE -> PictureFraming.Fill
            else -> PictureFraming.Zoom(scale.toDouble(), panX.toDouble(), panY.toDouble())
        }
    }

    private fun travelX(scale: Float): Float = max(0f, (fitWidth * scale - width) / 2f)

    private fun travelY(scale: Float): Float = max(0f, (fitHeight * scale - height) / 2f)

    private fun share(offset: Float, travel: Float): Float = if (travel > 0f) offset / travel else 0f

    companion object {
        const val MAX_SCALE = 4f

        // Within 5% of Fit or Fill, a pinch lands on it.
        private const val SNAP_SHARE = 0.05f
    }
}
