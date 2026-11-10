package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * How the whole long strip is magnified: [scale] screen pixels per strip pixel, from [MIN_SCALE] (or 1 while zooming
 * out is off) to [MAX_SCALE], drawn from the strip's top-left corner and moved left by [panX] screen pixels.
 *
 * The strip is laid out [scale] times shorter than the screen and magnified to fill it, so moving up and down is always
 * the strip's own scrolling and its first page's top stays in reach. Across, a strip wider than the screen pans as far
 * as its pages' edges; a narrower one stays centred.
 */
@Stable
internal class StripZoom {
    var scale by mutableFloatStateOf(1f)
        private set
    var panX by mutableFloatStateOf(0f)
        private set

    var viewportWidth = 0f
        private set
    private var sideInset = 0f
    private var animation: Job? = null

    /** Whether zooming out below the strip's width is off, which the setting says. */
    var zoomOutDisabled = false
        set(value) {
            field = value
            if (value && scale < 1f) {
                scale = 1f
                panX = clampedPanX(panX, scale)
            }
        }

    private val minScale: Float get() = if (zoomOutDisabled) 1f else MIN_SCALE

    val isZoomedIn: Boolean get() = scale > 1f + SCALE_TOLERANCE

    /** Where the pages' left edge is on screen, in screen pixels. */
    val pagesLeft: Float get() = sideInset * scale - panX

    /** Where the pages' right edge is on screen, in screen pixels. */
    val pagesRight: Float get() = (viewportWidth - sideInset) * scale - panX

    /** The screen [width] in pixels and the empty space [inset] at each side of the pages, in strip pixels. */
    fun setViewport(width: Float, inset: Float) {
        viewportWidth = width
        sideInset = inset
        panX = clampedPanX(panX, scale)
    }

    /**
     * Zooms by [factor] keeping the strip point under [focus], a point of the screen, where it is. [scrollBy] scrolls
     * the strip by strip pixels, which keeps the point in place up and down.
     */
    fun zoomBy(factor: Float, focus: Offset, scrollBy: (Float) -> Unit) {
        val target = (scale * factor).coerceIn(minScale, MAX_SCALE)
        if (target == scale) return
        val stripX = (focus.x + panX) / scale
        scrollBy(focus.y / scale - focus.y / target)
        scale = target
        panX = clampedPanX(stripX * target - focus.x, target)
    }

    /** Moves the magnified strip across by [deltaX] screen pixels as far as its pages' edges allow. */
    fun panBy(deltaX: Float) {
        panX = clampedPanX(panX - deltaX, scale)
    }

    /**
     * Zooms a strip at its width in to twice that with [focus] staying where it is, or one zoomed in or out back to its
     * width, over [durationMillis].
     */
    suspend fun toggle(focus: Offset, durationMillis: Int, scrollBy: (Float) -> Unit) = animateScale(
        target = if (abs(scale - 1f) < SCALE_TOLERANCE) DOUBLE_TAP_SCALE else 1f,
        focus = focus,
        durationMillis = durationMillis,
        scrollBy = scrollBy,
    )

    /** Keeps panning across with [velocityX] screen pixels per second after a drag lets go, slowing to a stop. */
    suspend fun flingX(velocityX: Float) = runAnimation {
        var previous = 0f
        AnimationState(0f, velocityX).animateDecay(exponentialDecay()) {
            panBy(value - previous)
            previous = value
        }
    }

    /** Stops a zoom or fling in progress, as a new touch does. */
    fun stopAnimation() {
        animation?.cancel()
        animation = null
    }

    private suspend fun animateScale(target: Float, focus: Offset, durationMillis: Int, scrollBy: (Float) -> Unit) =
        runAnimation {
            AnimationState(scale).animateTo(target, tween(durationMillis)) {
                zoomBy(value / scale, focus, scrollBy)
            }
        }

    private suspend fun runAnimation(block: suspend () -> Unit) = coroutineScope {
        stopAnimation()
        val job = launch { block() }
        animation = job
        job.join()
        if (animation === job) animation = null
    }

    private fun clampedPanX(panX: Float, scale: Float): Float {
        val pagesLeft = sideInset * scale
        val pagesRight = (viewportWidth - sideInset) * scale
        return if (pagesRight - pagesLeft >= viewportWidth) {
            panX.coerceIn(pagesLeft, pagesRight - viewportWidth)
        } else {
            (pagesLeft + pagesRight - viewportWidth) / 2f
        }
    }
}

private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 3f
private const val DOUBLE_TAP_SCALE = 2f
private const val SCALE_TOLERANCE = 0.01f
