package mihon.presentation.kit.content

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * Boxes a document draws around its passages, such as a game's status window or a letter set apart from the
 * narration, for content renderers such as the book reader. Every value comes from the document; colours arrive
 * already fitted to the reading palette (documentColor). A box can span several rows of a lazy list or several
 * pages, so each row draws its own slice: a slice that continues above or below leaves that edge open, with no
 * border, rounding or shadow there, and the slices join into one box.
 */

/** What a document box draws. */
@Immutable
data class DocumentBoxDecoration(
    val background: Color = Color.Unspecified,
    val gradient: DocumentGradient? = null,
    val borders: DocumentBorders = DocumentBorders(),
    val corners: DocumentCorners = DocumentCorners(),
    /** Outermost first, as a document lists them. */
    val shadows: List<DocumentShadow> = emptyList(),
)

@Immutable
data class DocumentBorder(val width: Dp, val color: Color, val style: DocumentBorderStyle)

/** Border styles as CSS names them. */
enum class DocumentBorderStyle {
    Solid,
    Dashed,
    Dotted,
    Double,
    Groove,
    Ridge,
    Inset,
    Outset,
}

@Immutable
data class DocumentBorders(
    val top: DocumentBorder? = null,
    val right: DocumentBorder? = null,
    val bottom: DocumentBorder? = null,
    val left: DocumentBorder? = null,
)

@Immutable
data class DocumentCorners(
    val topLeft: Dp = 0.dp,
    val topRight: Dp = 0.dp,
    val bottomRight: Dp = 0.dp,
    val bottomLeft: Dp = 0.dp,
)

@Immutable
data class DocumentShadow(
    val offsetX: Dp,
    val offsetY: Dp,
    val blur: Dp,
    val spread: Dp,
    val color: Color,
    val inset: Boolean,
)

/**
 * A gradient filling a box, drawn over its background. [angleDegrees] follows CSS: 0 points up, 90 right, 180 down.
 * A stop without a position sits evenly between its neighbours, as in CSS. A radial gradient spreads from the centre
 * to the farthest corner.
 */
@Immutable
data class DocumentGradient(
    val radial: Boolean,
    val angleDegrees: Float,
    val stops: List<Pair<Color, Float?>>,
    val repeating: Boolean,
)

/**
 * Draws [decoration] behind the content as one slice of a box. [continuesAbove] and [continuesBelow] say whether the
 * box goes on in the previous or next slice; the padding, margins and border widths around the content are the
 * caller's.
 */
fun Modifier.documentBox(
    decoration: DocumentBoxDecoration,
    continuesAbove: Boolean,
    continuesBelow: Boolean,
): Modifier = drawWithCache {
    val slice = BoxSlice(this, decoration, continuesAbove, continuesBelow)
    onDrawBehind { slice.draw(this) }
}

private class BoxSlice(
    scope: CacheDrawScope,
    private val decoration: DocumentBoxDecoration,
    private val continuesAbove: Boolean,
    private val continuesBelow: Boolean,
) {
    private val width = scope.size.width
    private val height = scope.size.height
    private val top: Float
    private val bottom: Float
    private val widths: FloatArray
    private val radii: FloatArray
    private val outer: Path
    private val inner: Path
    private val outerRect: Rect
    private val innerRect: Rect
    private val shadows: List<ResolvedShadow>

    init {
        with(scope) {
            radii = with(decoration.corners) {
                floatArrayOf(topLeft.toPx(), topRight.toPx(), bottomRight.toPx(), bottomLeft.toPx())
            }
            widths = decoration.borders.let { borders ->
                floatArrayOf(
                    borders.top.px(this),
                    borders.right.px(this),
                    borders.bottom.px(this),
                    borders.left.px(this),
                )
            }
            shadows = decoration.shadows.map { shadow ->
                ResolvedShadow(
                    shadow.offsetX.toPx(),
                    shadow.offsetY.toPx(),
                    shadow.blur.toPx(),
                    shadow.spread.toPx(),
                    shadow.color,
                    shadow.inset,
                )
            }
            // An open edge moves far enough outside the slice that its corners, border and shadow fall outside the
            // clip, so neighbouring slices join without a seam.
            val reach = (radii.maxOrNull() ?: 0f) + (widths.maxOrNull() ?: 0f) +
                shadows.maxOfOrNull { abs(it.offsetX) + abs(it.offsetY) + it.blur * 2 + abs(it.spread) }.orZero() + 1f
            top = if (continuesAbove) -reach else 0f
            bottom = if (continuesBelow) height + reach else height
            outerRect = Rect(0f, top, width, bottom)
            innerRect =
                Rect(outerRect.left + widths[3], top + widths[0], outerRect.right - widths[1], bottom - widths[2])
            outer = roundRectPath(outerRect, radii)
            inner = roundRectPath(
                innerRect,
                floatArrayOf(
                    radii[0] - maxOf(widths[3], widths[0]),
                    radii[1] - maxOf(widths[1], widths[0]),
                    radii[2] - maxOf(widths[1], widths[2]),
                    radii[3] - maxOf(widths[3], widths[2]),
                ),
            )
        }
    }

    fun draw(scope: DrawScope) = with(scope) {
        clipRect(
            left = -UNBOUNDED,
            top = if (continuesAbove) 0f else -UNBOUNDED,
            right = width + UNBOUNDED,
            bottom = if (continuesBelow) height else height + UNBOUNDED,
        ) {
            shadows.filterNot(ResolvedShadow::inset).asReversed().forEach { shadow ->
                clipPath(outer, ClipOp.Difference) { drawOuterShadow(shadow) }
            }
            if (decoration.background.isSpecified()) drawPath(outer, decoration.background)
            decoration.gradient?.let { gradient -> drawPath(outer, gradient.brush(width, height)) }
            shadows.filter(ResolvedShadow::inset).asReversed().forEach { shadow ->
                clipPath(outer) { drawInsetShadow(shadow) }
            }
            clipPath(outer) {
                clipPath(inner, ClipOp.Difference) { drawBorders() }
            }
        }
    }

    private fun DrawScope.drawOuterShadow(shadow: ResolvedShadow) {
        val rect = outerRect.inflate(shadow.spread).translate(shadow.offsetX, shadow.offsetY)
        drawBlurred(roundRectPath(rect, cornerRadii(shadow.spread)), shadow)
    }

    private fun DrawScope.drawInsetShadow(shadow: ResolvedShadow) {
        val hole = outerRect.deflate(shadow.spread).translate(shadow.offsetX, shadow.offsetY)
        val frame = Path().apply {
            addRect(outerRect.inflate(abs(shadow.offsetX) + abs(shadow.offsetY) + shadow.blur * 2 + shadow.spread))
            addPath(roundRectPath(hole, cornerRadii(-shadow.spread)))
            fillType = PathFillType.EvenOdd
        }
        drawBlurred(frame, shadow)
    }

    private fun DrawScope.drawBlurred(path: Path, shadow: ResolvedShadow) {
        if (shadow.blur <= 0f) {
            drawPath(path, shadow.color)
            return
        }
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = shadow.color.toArgb()
                maskFilter = BlurMaskFilter(shadow.blur * BLUR_TO_RADIUS, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
        }
    }

    private fun cornerRadii(grow: Float): FloatArray = FloatArray(4) { index ->
        (radii[index] + grow).coerceAtLeast(0f)
    }

    private fun DrawScope.drawBorders() {
        val sides = with(decoration.borders) { listOf(top, right, bottom, left) }
        sides.forEachIndexed { side, border ->
            if (border == null || widths[side] <= 0f) return@forEachIndexed
            val color = border.color
            val darker = lerp(color, Color.Black, SHADE)
            val lighter = lerp(color, Color.White, SHADE)
            val upperLeft = side == TOP || side == LEFT
            when (border.style) {
                DocumentBorderStyle.Solid -> drawPath(sidePolygon(side, 0f, 1f), color)
                DocumentBorderStyle.Double -> {
                    drawPath(sidePolygon(side, 0f, 1f / 3f), color)
                    drawPath(sidePolygon(side, 2f / 3f, 1f), color)
                }
                DocumentBorderStyle.Groove, DocumentBorderStyle.Ridge -> {
                    val groove = border.style == DocumentBorderStyle.Groove
                    val outerHalf = if (groove == upperLeft) darker else lighter
                    val innerHalf = if (groove == upperLeft) lighter else darker
                    drawPath(sidePolygon(side, 0f, 0.5f), outerHalf)
                    drawPath(sidePolygon(side, 0.5f, 1f), innerHalf)
                }
                DocumentBorderStyle.Inset -> drawPath(sidePolygon(side, 0f, 1f), if (upperLeft) darker else lighter)
                DocumentBorderStyle.Outset -> drawPath(sidePolygon(side, 0f, 1f), if (upperLeft) lighter else darker)
                DocumentBorderStyle.Dashed, DocumentBorderStyle.Dotted -> clipPath(sidePolygon(side, 0f, 1f)) {
                    val (start, end) = sideLine(side)
                    val strokeWidth = widths[side]
                    val dotted = border.style == DocumentBorderStyle.Dotted
                    drawLine(
                        color = color,
                        start = start,
                        end = end,
                        strokeWidth = strokeWidth,
                        cap = if (dotted) StrokeCap.Round else StrokeCap.Butt,
                        pathEffect = if (dotted) {
                            PathEffect.dashPathEffect(floatArrayOf(0f, strokeWidth * 2f))
                        } else {
                            PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 2f, strokeWidth))
                        },
                    )
                }
            }
        }
    }

    // The part of a side's border between two depths, as fractions of its width, mitred into the corners.
    private fun sidePolygon(side: Int, from: Float, to: Float): Path {
        val corners = listOf(
            corner(Offset(outerRect.left, outerRect.top), Offset(innerRect.left, innerRect.top)),
            corner(Offset(outerRect.right, outerRect.top), Offset(innerRect.right, innerRect.top)),
            corner(Offset(outerRect.right, outerRect.bottom), Offset(innerRect.right, innerRect.bottom)),
            corner(Offset(outerRect.left, outerRect.bottom), Offset(innerRect.left, innerRect.bottom)),
        )
        val first = corners[side]
        val second = corners[(side + 1) % 4]
        return Path().apply {
            moveTo(first(from))
            lineTo(second(from))
            lineTo(second(to))
            lineTo(first(to))
            close()
        }
    }

    private fun sideLine(side: Int): Pair<Offset, Offset> {
        val half = widths[side] / 2f
        return when (side) {
            TOP -> Offset(outerRect.left, outerRect.top + half) to Offset(outerRect.right, outerRect.top + half)
            RIGHT -> Offset(outerRect.right - half, outerRect.top) to Offset(outerRect.right - half, outerRect.bottom)
            BOTTOM -> Offset(outerRect.left, outerRect.bottom - half) to
                Offset(outerRect.right, outerRect.bottom - half)
            else -> Offset(outerRect.left + half, outerRect.top) to Offset(outerRect.left + half, outerRect.bottom)
        }
    }
}

private class ResolvedShadow(
    val offsetX: Float,
    val offsetY: Float,
    val blur: Float,
    val spread: Float,
    val color: Color,
    val inset: Boolean,
)

private fun corner(outer: Offset, inner: Offset): (Float) -> Offset = { fraction -> lerp(outer, inner, fraction) }

private fun Path.moveTo(point: Offset) = moveTo(point.x, point.y)

private fun Path.lineTo(point: Offset) = lineTo(point.x, point.y)

private fun DocumentBorder?.px(density: Density): Float =
    this?.let { with(density) { it.width.toPx() } } ?: 0f

private fun Float?.orZero(): Float = this ?: 0f

private fun Color.isSpecified(): Boolean = this != Color.Unspecified

private fun roundRectPath(rect: Rect, radii: FloatArray): Path = Path().apply {
    addRoundRect(
        RoundRect(
            rect,
            topLeft = CornerRadius(radii[0].coerceAtLeast(0f)),
            topRight = CornerRadius(radii[1].coerceAtLeast(0f)),
            bottomRight = CornerRadius(radii[2].coerceAtLeast(0f)),
            bottomLeft = CornerRadius(radii[3].coerceAtLeast(0f)),
        ),
    )
}

private fun DocumentGradient.brush(width: Float, height: Float): Brush {
    val resolved = resolvedStops()
    val last = resolved.last().second.takeIf { repeating && it > 0f && it < 1f } ?: 1f
    val colorStops = resolved.map { (color, position) -> position / last to color }.toTypedArray()
    val tileMode = if (repeating) TileMode.Repeated else TileMode.Clamp
    if (radial) {
        return Brush.radialGradient(
            colorStops = colorStops,
            center = Offset(width / 2f, height / 2f),
            radius = (hypot(width, height) / 2f * last).coerceAtLeast(1f),
            tileMode = tileMode,
        )
    }
    // CSS sizes the gradient line so the corners in its direction get the first and last colours exactly.
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = sin(radians).toFloat()
    val dy = -cos(radians).toFloat()
    val halfLength = (abs(width * dx) + abs(height * dy)) / 2f
    val center = Offset(width / 2f, height / 2f)
    val start = center - Offset(dx, dy) * halfLength
    val end = start + Offset(dx, dy) * (halfLength * 2f * last)
    return Brush.linearGradient(colorStops = colorStops, start = start, end = end, tileMode = tileMode)
}

// CSS fills positions left out: the first stop at 0, the last at 1, the rest evenly between known neighbours, and a
// position before an earlier one moves up to it.
private fun DocumentGradient.resolvedStops(): List<Pair<Color, Float>> {
    val positions = stops.map { it.second }.toMutableList()
    if (positions.first() == null) positions[0] = 0f
    if (positions.last() == null) positions[positions.lastIndex] = 1f
    var index = 0
    while (index < positions.size) {
        if (positions[index] == null) {
            val before = index - 1
            val after = (index until positions.size).first { positions[it] != null }
            val from = positions[before]!!
            val to = positions[after]!!
            for (gap in index until after) {
                positions[gap] = from + (to - from) * (gap - before) / (after - before)
            }
            index = after
        }
        index++
    }
    var floor = 0f
    return stops.mapIndexed { stop, (color, _) ->
        floor = maxOf(floor, positions[stop]!!)
        color to floor
    }
}

private const val TOP = 0
private const val RIGHT = 1
private const val BOTTOM = 2
private const val LEFT = 3
private const val UNBOUNDED = 100_000f
private const val SHADE = 0.4f

// A CSS blur radius is twice the Gaussian deviation; BlurMaskFilter takes about 1.7 deviations.
private const val BLUR_TO_RADIUS = 0.87f
