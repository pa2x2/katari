package app.katari.entry.interactions.manga.reader.image.tiles

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draws [block] clipped to the rectangle from the origin to [right] and [bottom], with each pixel along its edges
 * either wholly in or wholly out. Pages and their pieces meet at fractional pixels once stretched or magnified; an
 * antialiased clip would cover the shared row only partly from each side, and the background would show through as
 * a line.
 */
internal expect fun DrawScope.clipToWholePixels(right: Float, bottom: Float, block: DrawScope.() -> Unit)
