package mihon.presentation.kit.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse

/*
 * Colours a document chose itself, such as a book's coloured text or the background of a status window, on the
 * reading palette. The reader replaces only the page. Documents are mostly written for black text on a white page, so
 * a colour drawn straight on the page is fitted to the palette: lightness maps linearly from that range onto the
 * palette's background-to-foreground range in Oklab, keeping hue and chroma, and on a white page with black text the
 * colours stay as written. A surface the document paints itself, such as a box's background, is a page of its own
 * that the reader doesn't replace: it is drawn as written on every theme, with the border and shadows that edge it,
 * and what lies on it keeps the colours its author chose for it, only kept readable. Fitting such a surface would turn
 * a dark status window light on a dark theme, though it was designed for one.
 *
 * A `surface` parameter is the colour behind the drawn colour: Color.Unspecified for the reader's page, or a
 * surface the document paints, as documentSurfaceColor gives it.
 */

/** A surface the document paints, given as ARGB, such as a box or text background, or the border and shadow of one. */
fun documentSurfaceColor(argb: Long): Color = Color(argb)

/** A border or rule colour of the document, given as ARGB, kept visible on [surface]. */
fun ReadingPalette.documentLineColor(argb: Long, surface: Color = Color.Unspecified): Color =
    readable(onSurface(argb, surface), surface, MINIMUM_LINE_CONTRAST)

/** A text colour of the document, given as ARGB, kept readable on [surface]. */
fun ReadingPalette.documentTextColor(argb: Long, surface: Color = Color.Unspecified): Color =
    readable(onSurface(argb, surface), surface, MINIMUM_TEXT_CONTRAST)

/**
 * The colour of text the document leaves uncoloured, [inherited] from around it, kept readable on [surface]: the
 * reader's text colour can vanish on a background the document paints.
 */
fun ReadingPalette.documentInheritedTextColor(inherited: Color, surface: Color): Color =
    if (surface.isSpecified) readable(inherited, surface, MINIMUM_TEXT_CONTRAST) else inherited

/**
 * A shadow colour of the document, given as ARGB, on [surface]. On the page, a dark shadow keeps its colour, since it
 * darkens whatever is under it on any page; inverting it would turn it into a glow. A light or coloured glow is fitted.
 */
fun ReadingPalette.documentShadowColor(argb: Long, surface: Color = Color.Unspecified): Color {
    val color = Color(argb)
    if (surface.isSpecified || color.convert(ColorSpaces.Oklab).red < DARK_SHADOW_LIGHTNESS) return color
    return fit(color)
}

private fun ReadingPalette.onSurface(argb: Long, surface: Color): Color =
    if (surface.isSpecified) Color(argb) else fit(Color(argb))

private fun ReadingPalette.readable(drawn: Color, surface: Color, minimum: Float): Color {
    val backdrop = surface.takeOrElse { background }
    val target = if (foreground.contrastWith(backdrop) >= background.contrastWith(backdrop)) foreground else background
    var color = drawn
    var step = 0
    while (color.compositeOver(backdrop).contrastWith(backdrop) < minimum && step < CONTRAST_STEPS) {
        step++
        color = lerp(drawn, target, step.toFloat() / CONTRAST_STEPS)
    }
    return color
}

private fun ReadingPalette.fit(color: Color): Color {
    val lab = color.convert(ColorSpaces.Oklab)
    val backgroundLightness = background.convert(ColorSpaces.Oklab).red
    val foregroundLightness = foreground.convert(ColorSpaces.Oklab).red
    val lightness = backgroundLightness + (foregroundLightness - backgroundLightness) * (1f - lab.red)
    return Color(
        red = lightness.coerceIn(0f, 1f),
        green = lab.green,
        blue = lab.blue,
        alpha = color.alpha,
        colorSpace = ColorSpaces.Oklab,
    ).convert(ColorSpaces.Srgb)
}

/** WCAG's minimum contrast for large text; coloured book text is mostly emphasis, not body copy. */
private const val MINIMUM_TEXT_CONTRAST = 3f

/** WCAG's minimum contrast for graphical objects such as borders. */
private const val MINIMUM_LINE_CONTRAST = 3f
private const val DARK_SHADOW_LIGHTNESS = 0.5f
private const val CONTRAST_STEPS = 10
