package app.katari.entry.interactions.manga.reader.image.tiles

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect

// Android clips rectangles without antialiasing
internal actual fun DrawScope.clipToWholePixels(right: Float, bottom: Float, block: DrawScope.() -> Unit) =
    clipRect(right = right, bottom = bottom, block = block)
