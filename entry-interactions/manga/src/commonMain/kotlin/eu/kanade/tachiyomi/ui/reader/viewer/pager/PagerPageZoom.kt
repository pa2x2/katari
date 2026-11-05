package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** How a page is fitted into its slot before the reader zooms it, as the page scale setting numbers them. */
internal enum class PageScaleType {
    FitScreen,
    Stretch,
    FitWidth,
    FitHeight,
    OriginalSize,

    /** Fits a tall page's width and a wide page's height. */
    SmartFit,
    ;

    companion object {
        fun fromSetting(value: Int): PageScaleType = entries.getOrElse(value - 1) { FitScreen }
    }
}

/** Which part of a page larger than its slot shows first. */
internal enum class PageStartPosition { Left, Right, Center }

/**
 * How one page sits in its slot: [scale] screen pixels per page pixel and the page's top-left corner at [offset] from
 * the slot's. A page smaller than the slot along a side is centred along it; a larger one never leaves a gap at an
 * edge. Zooming goes from the fitted scale up to [MAX_ZOOM] times it.
 */
internal class PagerPageZoom {
    var slotSize by mutableStateOf(IntSize.Zero)
        private set
    var pageSize by mutableStateOf(IntSize.Zero)
        private set
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var fittedScale by mutableFloatStateOf(1f)
        private set

    private var scaleType = PageScaleType.FitScreen
    private var startPosition = PageStartPosition.Center

    val isZoomed: Boolean
        get() = scale > fittedScale * (1f + ZOOM_TOLERANCE)

    val isLaidOut: Boolean
        get() = slotSize.width > 0 && pageSize.width > 0

    /**
     * Fits a page of [pageSize] into a slot of [slotSize] the way [scaleType] says and shows the part [startPosition]
     * names. Called again when either size changes, as when the window folds or rotates, which starts the page over.
     */
    fun fit(slotSize: IntSize, pageSize: IntSize, scaleType: PageScaleType, startPosition: PageStartPosition) {
        this.slotSize = slotSize
        this.pageSize = pageSize
        this.scaleType = scaleType
        this.startPosition = startPosition
        if (!isLaidOut) return
        fittedScale = fittedScale()
        scale = fittedScale
        offset = clamped(startOffset(fittedScale, forward = true), fittedScale)
    }

    private fun fittedScale(): Float {
        val widthScale = slotSize.width.toFloat() / pageSize.width
        val heightScale = slotSize.height.toFloat() / pageSize.height
        return when (scaleType) {
            PageScaleType.FitScreen -> min(widthScale, heightScale)
            PageScaleType.Stretch -> max(widthScale, heightScale)
            PageScaleType.FitWidth -> widthScale
            PageScaleType.FitHeight -> heightScale
            PageScaleType.OriginalSize -> 1f
            PageScaleType.SmartFit -> if (pageSize.height > pageSize.width) widthScale else heightScale
        }
    }

    /** The offset that shows [startPosition] at [scale]; turning back shows the other end of a wide page first. */
    private fun startOffset(scale: Float, forward: Boolean): Offset = when (startPosition) {
        PageStartPosition.Left -> if (forward) Offset.Zero else Offset(-Float.MAX_VALUE, 0f)
        PageStartPosition.Right -> if (forward) Offset(-Float.MAX_VALUE, 0f) else Offset.Zero
        PageStartPosition.Center -> Offset(
            (slotSize.width - pageSize.width * scale) / 2f,
            (slotSize.height - pageSize.height * scale) / 2f,
        )
    }

    /** Zooms by [factor], keeping the page point under [focus], a point of the slot, where it is. */
    fun zoomBy(factor: Float, focus: Offset) {
        val target = (scale * factor).coerceIn(fittedScale, fittedScale * MAX_ZOOM)
        val applied = target / scale
        offset = clamped(focus - (focus - offset) * applied, target)
        scale = target
    }

    /** Moves the page by [delta] as far as its edges allow; returns how far it moved. */
    fun panBy(delta: Offset): Offset {
        val before = offset
        offset = clamped(offset + delta, scale)
        return offset - before
    }

    /** Whether the page has more to show past its edge on the side a drag of [delta] brings into view. */
    fun canPan(delta: Offset): Boolean {
        val target = clamped(offset + delta, scale)
        return abs(target.x - offset.x) > PAN_EPSILON || abs(target.y - offset.y) > PAN_EPSILON
    }

    /** Whether the page reaches past the slot's left (negative) or right (positive) edge in [direction]. */
    fun canPanHorizontally(direction: Int): Boolean = canPan(Offset(-direction * PAN_EPSILON * 2, 0f))

    /** Moves by a slot's width in [direction], -1 to the left or 1 to the right, as navigation does in place of a turn. */
    suspend fun panScreen(direction: Int) {
        animateTo(scale, clamped(offset - Offset(direction * slotSize.width.toFloat(), 0f), scale), PAN_SCREEN_MILLIS)
    }

    /**
     * Zooms a double-tapped page in to twice its fitted scale with the tapped point moved to the middle, or one already
     * zoomed back out, over [durationMillis].
     */
    suspend fun toggleZoom(focus: Offset, durationMillis: Int) {
        if (!isLaidOut) return
        if (scale > fittedScale * DOUBLE_TAP_ZOOM * ZOOMED_IN_SHARE && isZoomed) {
            animateTo(fittedScale, clamped(startOffset(fittedScale, forward = true), fittedScale), durationMillis)
            return
        }
        val target = fittedScale * DOUBLE_TAP_ZOOM
        val pagePoint = (focus - offset) / scale
        val centred = Offset(slotSize.width / 2f, slotSize.height / 2f) - pagePoint * target
        animateTo(target, clamped(centred, target), durationMillis)
    }

    /**
     * Zooms a wide page fitted to the screen in until it fills the slot's height, showing the end it starts from:
     * [forward] reading starts at [PageStartPosition], turning back at the other end.
     */
    suspend fun zoomLandscapePage(forward: Boolean) {
        if (!isLaidOut || scaleType != PageScaleType.FitScreen || pageSize.width <= pageSize.height || isZoomed) {
            return
        }
        val target = slotSize.height.toFloat() / pageSize.height
        animateTo(target, clamped(startOffset(target, forward), target), LANDSCAPE_ZOOM_MILLIS, EaseInOutQuad)
    }

    /** Carries on a released drag at [velocity] until it slows to a stop or meets the page's edges. */
    suspend fun fling(velocity: Velocity) {
        var previous = Offset.Zero
        AnimationState(Offset.VectorConverter, Offset.Zero, Offset(velocity.x, velocity.y)).animateDecay(DECAY) {
            val delta = value - previous
            previous = value
            if (panBy(delta) == Offset.Zero) cancelAnimation()
        }
    }

    private suspend fun animateTo(
        target: Float,
        targetOffset: Offset,
        durationMillis: Int,
        easing: Easing = EaseOutQuad,
    ) {
        Animatable(ZoomFrame(scale, offset), ZoomFrameConverter)
            .animateTo(ZoomFrame(target, targetOffset), tween(durationMillis, easing = easing)) {
                scale = value.scale
                offset = clamped(value.offset, value.scale)
            }
    }

    // A larger page never shows a gap at an edge; a smaller one is centred along the side it doesn't fill
    private fun clamped(offset: Offset, scale: Float): Offset {
        val width = pageSize.width * scale
        val height = pageSize.height * scale
        val x = if (width <= slotSize.width) {
            (slotSize.width - width) / 2f
        } else {
            offset.x.coerceIn(slotSize.width - width, 0f)
        }
        val y = if (height <= slotSize.height) {
            (slotSize.height - height) / 2f
        } else {
            offset.y.coerceIn(slotSize.height - height, 0f)
        }
        return Offset(x, y)
    }
}

private data class ZoomFrame(val scale: Float, val offset: Offset)

private val ZoomFrameConverter = TwoWayConverter<ZoomFrame, AnimationVector3D>(
    convertToVector = { AnimationVector3D(it.scale, it.offset.x, it.offset.y) },
    convertFromVector = { ZoomFrame(it.v1, Offset(it.v2, it.v3)) },
)

private val EaseOutQuad = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)
private val EaseInOutQuad = CubicBezierEasing(0.455f, 0.03f, 0.515f, 0.955f)
private val DECAY = exponentialDecay<Offset>()

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2f

/** A page zoomed past this share of the double-tap scale counts as zoomed in, so a double tap zooms it out. */
private const val ZOOMED_IN_SHARE = 0.9f
private const val ZOOM_TOLERANCE = 0.001f
private const val PAN_EPSILON = 0.5f
private const val PAN_SCREEN_MILLIS = 250
private const val LANDSCAPE_ZOOM_MILLIS = 500
