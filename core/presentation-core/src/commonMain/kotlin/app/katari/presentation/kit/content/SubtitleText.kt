package app.katari.presentation.kit.content

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.ceil
import kotlin.math.roundToInt

/*
 * Subtitle text over video, in the look the viewer picked: their colours, size and edge are data, so a player passes
 * them here instead of styling text itself. Where each line goes is the player's: it measures the text here, places
 * it, and draws it with drawSubtitleText. The look follows the subtitles Android's caption settings draw: a background
 * behind each line's text, and a dark edge two density-independent pixels wide.
 */

/** What sets subtitle text apart from the picture behind it, besides its background. */
enum class SubtitleEdge { None, Outline, Shadow }

/** The viewer's subtitle look: [textSizePx] in pixels, the colours as ARGB, the background's opacity included. */
@Immutable
data class SubtitleLook(
    val textSizePx: Float,
    val textArgb: Int,
    val backgroundArgb: Int,
    val edge: SubtitleEdge,
)

/** One cue's text laid out in a [SubtitleLook], for [drawSubtitleText]. */
class SubtitleTextLayout internal constructor(internal val result: TextLayoutResult, internal val look: SubtitleLook) {
    val width: Int get() = result.size.width
    val height: Int get() = result.size.height

    /** The widest line's width, in pixels. */
    val linesWidth: Int = (0 until result.lineCount).maxOfOrNull { line ->
        ceil(result.getLineRight(line) - result.getLineLeft(line)).toInt()
    } ?: 0

    val firstLineHeight: Float
        get() = if (result.lineCount > 0) result.getLineBottom(0) - result.getLineTop(0) else 0f
}

/**
 * Lays [text] out in [look], wrapped to [width] pixels and aligned in it by [alignment]. Unless [fixedWidth], the
 * layout is only as wide as its widest line.
 */
fun TextMeasurer.measureSubtitleText(
    text: String,
    look: SubtitleLook,
    width: Int,
    alignment: TextAlign,
    fixedWidth: Boolean,
    density: Density,
): SubtitleTextLayout {
    val maxWidth = width.coerceAtLeast(0)
    val result = measure(
        text = text,
        style = TextStyle(
            color = Color(look.textArgb),
            fontSize = with(density) { look.textSizePx.toSp() },
            textAlign = alignment,
        ),
        overflow = TextOverflow.Clip,
        constraints = Constraints(minWidth = if (fixedWidth) maxWidth else 0, maxWidth = maxWidth),
        density = density,
    )
    return SubtitleTextLayout(result, look)
}

/** Draws [layout] with its top left corner at [topLeft]: the background behind each line, then the edge, then the text. */
fun DrawScope.drawSubtitleText(layout: SubtitleTextLayout, topLeft: Offset) {
    val result = layout.result
    val look = layout.look
    val background = Color(look.backgroundArgb)
    if (background.alpha > 0f) {
        for (line in 0 until result.lineCount) {
            val left = result.getLineLeft(line)
            val right = result.getLineRight(line)
            if (right <= left) continue
            val top = result.getLineTop(line)
            drawRect(
                color = background,
                topLeft = topLeft + Offset(left, top),
                size = Size(right - left, result.getLineBottom(line) - top),
            )
        }
    }
    val edgePx = (EDGE_WIDTH_DP * density).roundToInt().toFloat()
    val edgeColor = Color.Black
    // Android keeps a text layout's paint between draws, and a null style or shadow leaves the last one in place, so
    // every pass sets both: otherwise the fill after an outline strokes over it, and a shadow outlives its edge
    when (look.edge) {
        SubtitleEdge.None -> drawText(result, topLeft = topLeft, drawStyle = Fill, shadow = Shadow.None)
        SubtitleEdge.Outline -> {
            drawText(
                result,
                color = edgeColor,
                topLeft = topLeft,
                drawStyle = Stroke(width = edgePx, join = StrokeJoin.Round),
                shadow = Shadow.None,
            )
            drawText(result, topLeft = topLeft, drawStyle = Fill, shadow = Shadow.None)
        }
        SubtitleEdge.Shadow -> drawText(
            result,
            topLeft = topLeft,
            drawStyle = Fill,
            shadow = Shadow(color = edgeColor, offset = Offset(edgePx, edgePx), blurRadius = edgePx),
        )
    }
}

private const val EDGE_WIDTH_DP = 2f
