package app.katari.presentation.kit.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * Colours read from a page image, such as the margin a manga page continues into the space around it. They are drawn
 * as the image has them on every theme, as the page itself is.
 */

/** A colour of the page image, given as ARGB. */
fun pageBackdropColor(argb: Int): Color = Color(argb)

/**
 * Colours of the page image, given as ARGB, spread evenly from top to bottom: over the whole area drawn in, or over
 * [height] pixels from its top, such as a stretch of a side painted at its own place in a taller area.
 */
fun pageBackdropBrush(argb: List<Int>, height: Float = Float.POSITIVE_INFINITY): Brush =
    Brush.verticalGradient(argb.map(::Color), startY = 0f, endY = height)
