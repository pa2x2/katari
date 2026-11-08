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

    /**
     * [framing] zoomed by [factor] about the point ([focusX], [focusY]) of the player, which keeps the part of the
     * picture under it there, landing on Fit or Fill when close to either.
     */
    fun zoom(
        framing: PictureFraming,
        factor: Float,
        focusX: Float = width / 2f,
        focusY: Float = height / 2f,
    ): PictureFraming = framingOf(pinch(place(framing), factor, focusX, focusY, focusX, focusY))

    /**
     * [base] scaled by [factor], with the part of the picture that was under the point ([fromX], [fromY]) of the
     * player moved under ([toX], [toY]), as two fingers that pinch and move together do; the scale lands on Fit or
     * Fill when close to either, and the picture stays over the player.
     */
    fun pinch(
        base: VideoPicturePlacement,
        factor: Float,
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
    ): VideoPicturePlacement {
        val scale = snapScale(base.scale * factor)
        val centreX = width / 2f
        val centreY = height / 2f
        // The point of the picture under (fromX, fromY), in the picture's fitted size
        val anchorX = (fromX - centreX - base.offsetX) / base.scale
        val anchorY = (fromY - centreY - base.offsetY) / base.scale
        return clamp(
            VideoPicturePlacement(
                scale = scale,
                offsetX = toX - centreX - anchorX * scale,
                offsetY = toY - centreY - anchorY * scale,
            ),
        )
    }

    /** [framing] moved by [deltaX] and [deltaY] pixels, as far as the picture overflows the player. */
    fun pan(framing: PictureFraming, deltaX: Float, deltaY: Float): PictureFraming {
        val placement = place(framing)
        return framingOf(placement.copy(offsetX = placement.offsetX + deltaX, offsetY = placement.offsetY + deltaY))
    }

    /** Whether the picture as [framing] places it is larger than the player, so it can be moved. */
    fun overflows(framing: PictureFraming): Boolean {
        val scale = place(framing).scale
        return travelX(scale) > 0f || travelY(scale) > 0f
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
