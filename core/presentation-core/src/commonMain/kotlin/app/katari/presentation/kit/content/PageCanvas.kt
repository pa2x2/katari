package app.katari.presentation.kit.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.PageBackgroundColors

/** The colour pages are read on, as a reader's background setting picks it. */
enum class PageBackground {
    White,
    Black,
    Gray,

    /** Grey in a dark theme, white in a light one. */
    Automatic,
}

/**
 * The surface pages are read on: [background] behind [content], which shows in shades of grey when [grayscale] and in
 * negative when [inverted], as a reader's colour settings ask. They change the pages, not the background behind
 * them, so a black background stays black under inverted pages.
 */
@Composable
fun PageCanvas(
    background: PageBackground,
    grayscale: Boolean,
    inverted: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val darkTheme = KatariTheme.colors.background.luminance() < DARK_LUMINANCE
    val color = when (background) {
        PageBackground.White -> PageBackgroundColors.white
        PageBackground.Black -> PageBackgroundColors.black
        PageBackground.Gray -> PageBackgroundColors.gray
        PageBackground.Automatic -> if (darkTheme) PageBackgroundColors.gray else PageBackgroundColors.white
    }
    val filter = remember(grayscale, inverted) { pageColorFilter(grayscale, inverted) }
    Box(
        modifier = modifier
            .background(color)
            .then(if (filter != null) Modifier.filtered(filter) else Modifier),
        propagateMinConstraints = true,
    ) {
        content()
    }
}

private fun Modifier.filtered(filter: ColorFilter): Modifier {
    val paint = Paint().apply { colorFilter = filter }
    return drawWithContent {
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
            drawContent()
            canvas.restore()
        }
    }
}

private fun pageColorFilter(grayscale: Boolean, inverted: Boolean): ColorFilter? {
    if (!grayscale && !inverted) return null
    val matrix = ColorMatrix()
    if (grayscale) matrix.setToSaturation(0f)
    if (inverted) matrix.timesAssign(ColorMatrix(INVERT))
    return ColorFilter.colorMatrix(matrix)
}

// Each channel becomes its complement, alpha as it was
private val INVERT = floatArrayOf(
    -1f, 0f, 0f, 0f, 255f,
    0f, -1f, 0f, 0f, 255f,
    0f, 0f, -1f, 0f, 255f,
    0f, 0f, 0f, 1f, 0f,
)

private const val DARK_LUMINANCE = 0.5f
