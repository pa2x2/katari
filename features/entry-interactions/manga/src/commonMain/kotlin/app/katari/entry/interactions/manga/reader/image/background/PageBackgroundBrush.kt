package app.katari.entry.interactions.manga.reader.image.background

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import app.katari.presentation.kit.theme.pageBackdropBrush
import app.katari.presentation.kit.theme.pageBackdropColor

/** How the background is painted; a gradient runs over the height of the area it fills. */
internal fun PageBackground.brush(): Brush = when (this) {
    is PageBackground.Solid -> SolidColor(pageBackdropColor(argb))
    is PageBackground.Gradient -> pageBackdropBrush(argb)
}

/** How the span is painted, [height] pixels tall from the top of the area drawn in, fading over that height. */
internal fun BackdropSpan.brush(height: Float): Brush = if (topArgb == bottomArgb) {
    SolidColor(pageBackdropColor(topArgb))
} else {
    pageBackdropBrush(listOf(topArgb, bottomArgb), height)
}
