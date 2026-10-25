package mihon.presentation.kit.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp

/*
 * Colours a document chose itself, such as a book's coloured text or the background of a status window, fitted to the
 * reading palette. Documents are written for black text on a white page, so the fit maps lightness linearly from that
 * range onto the palette's background-to-foreground range in Oklab and keeps hue and chroma: a dark box stays dark on
 * paper and turns light on a dark page, and the text inside it turns with it, so the contrast the author chose
 * survives. On a white page with black text the colours stay as written.
 */

/** A fill colour of the document, given as ARGB, fitted to this palette. */
fun ReadingPalette.documentColor(argb: Long): Color = fit(Color(argb))

/** A border or rule colour of the document, given as ARGB, fitted to this palette and kept visible on the page. */
fun ReadingPalette.documentLineColor(argb: Long): Color = readable(fit(Color(argb)), background, MINIMUM_LINE_CONTRAST)

/**
 * A text colour of the document, given as ARGB, fitted to this palette and kept readable on [backdrop], the fitted
 * colour behind the text.
 */
fun ReadingPalette.documentTextColor(argb: Long, backdrop: Color = background): Color =
    readable(fit(Color(argb)), backdrop, MINIMUM_TEXT_CONTRAST)

/**
 * A shadow colour of the document, given as ARGB. A dark shadow keeps its colour, since it darkens whatever is under
 * it on any page; inverting it would turn it into a glow. A light or coloured glow is fitted like a fill.
 */
fun ReadingPalette.documentShadowColor(argb: Long): Color {
    val color = Color(argb)
    return if (color.convert(ColorSpaces.Oklab).red < DARK_SHADOW_LIGHTNESS) color else fit(color)
}

private fun ReadingPalette.readable(fitted: Color, backdrop: Color, minimum: Float): Color {
    val target = if (foreground.contrastWith(backdrop) >= background.contrastWith(backdrop)) foreground else background
    var color = fitted
    var step = 0
    while (color.compositeOver(backdrop).contrastWith(backdrop) < minimum && step < CONTRAST_STEPS) {
        step++
        color = lerp(fitted, target, step.toFloat() / CONTRAST_STEPS)
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
