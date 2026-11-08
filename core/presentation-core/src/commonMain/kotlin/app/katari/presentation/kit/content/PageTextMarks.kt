package app.katari.presentation.kit.content

import androidx.annotation.ColorInt
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.presentation.kit.theme.style
import kotlin.math.roundToInt

/**
 * A translation laid over a page's original text: [text] on [background], an ARGB colour sampled from the page around
 * the text, filling [area].
 */
@Immutable
data class PageTextCover(val area: Rect, val text: String, @ColorInt val background: Int)

/**
 * Marks over a page for the text recognized on it: [regions] outlined, [covers] laying translations over the original
 * text, and [highlighted], a region or an area the reader outlined, shaded more strongly. Rectangles are in this
 * composable's own pixels, so marks drawn inside a magnified page zoom with it; [magnification] is how far the page
 * is magnified on screen, which outlines and corners are drawn against so they keep their width. A translation is set
 * in the largest size that fits its area, in black or white, whichever reads on its background.
 */
@Composable
fun PageTextMarks(
    regions: List<Rect>,
    highlighted: Rect?,
    covers: List<PageTextCover>,
    modifier: Modifier = Modifier,
    magnification: () -> Float = { 1f },
) {
    val markup = KatariTheme.markup
    val measurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography
    val style = remember(typography) {
        TextRole.Label.style(typography).copy(
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = LINE_HEIGHT_EM.em,
        )
    }
    // Fitting tries a dozen sizes, so a cover keeps its layout until it changes
    val fitted = remember(style) { HashMap<PageTextCover, TextLayoutResult?>() }
    Canvas(modifier) {
        val magnified = magnification().takeIf { it > 0f } ?: 1f
        val radius = CornerRadius(CORNER_RADIUS.toPx() / magnified)
        val outline = Stroke(OUTLINE_WIDTH.toPx() / magnified)
        val regionFill = markup.fill.copy(alpha = markup.fill.alpha * REGION_FILL_SHARE)
        regions.forEach { region ->
            drawRoundRect(regionFill, region.topLeft, region.size, radius)
            drawRoundRect(markup.mark, region.topLeft, region.size, radius, style = outline)
        }
        fitted.keys.retainAll(covers.toSet())
        covers.forEach { cover ->
            drawRoundRect(Color(cover.background), cover.area.topLeft, cover.area.size, radius)
            val layout = fitted.getOrPut(cover) { fit(cover, measurer, style) } ?: return@forEach
            val padding = TEXT_PADDING.toPx()
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    cover.area.left + padding,
                    cover.area.top + (cover.area.height - layout.size.height) / 2f,
                ),
            )
        }
        highlighted?.let {
            drawRoundRect(markup.fill, it.topLeft, it.size, radius)
            drawRoundRect(markup.mark, it.topLeft, it.size, radius, style = outline)
        }
    }
}

/** The largest size whose layout fits the cover, down to a legibility floor; null when the area holds no text. */
private fun DrawScope.fit(cover: PageTextCover, measurer: TextMeasurer, style: TextStyle): TextLayoutResult? {
    val padding = TEXT_PADDING.toPx()
    val width = (cover.area.width - 2 * padding).roundToInt()
    val height = (cover.area.height - 2 * padding).roundToInt()
    if (width <= 0 || height <= 0) return null
    val colored = style.copy(
        color = if (Color(cover.background).luminance() > DARK_BACKGROUND_LUMINANCE) Color.Black else Color.White,
    )
    // Words are broken across lines only when even the smallest size can't fit them
    val longestWord = cover.text.split(WHITESPACE).maxByOrNull(String::length).orEmpty()
    fun layout(size: Float) =
        measurer.measure(cover.text, colored.copy(fontSize = size.toSp()), constraints = Constraints.fixedWidth(width))
    fun wordFits(size: Float) =
        measurer.measure(longestWord, colored.copy(fontSize = size.toSp()), softWrap = false).size.width <= width
    var low = MINIMUM_TEXT_SIZE.toPx()
    var high = maxOf(low, height.toFloat())
    var best = layout(low)
    repeat(FIT_ITERATIONS) {
        val size = (low + high) / 2
        val candidate = layout(size)
        if (candidate.size.height <= height && wordFits(size)) {
            best = candidate
            low = size
        } else {
            high = size
        }
    }
    return best
}

private val OUTLINE_WIDTH = 1.5.dp
private val CORNER_RADIUS = 6.dp
private val TEXT_PADDING = 2.dp
private val MINIMUM_TEXT_SIZE = 7.dp
private const val LINE_HEIGHT_EM = 0.95f
private const val FIT_ITERATIONS = 10
private const val DARK_BACKGROUND_LUMINANCE = 0.45f

// Regions are marked more lightly than the highlighted one
private const val REGION_FILL_SHARE = 0.4f

private val WHITESPACE = Regex("""\s+""")
