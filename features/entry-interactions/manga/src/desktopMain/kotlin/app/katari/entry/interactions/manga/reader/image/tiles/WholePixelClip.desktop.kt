package app.katari.entry.interactions.manga.reader.image.tiles

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.skiaCanvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Rect

// Compose antialiases every rectangle clip on desktop, so this one goes to Skia directly
internal actual fun DrawScope.clipToWholePixels(right: Float, bottom: Float, block: DrawScope.() -> Unit) {
    val canvas = drawContext.canvas
    canvas.save()
    try {
        canvas.skiaCanvas.clipRect(Rect.makeWH(right, bottom), ClipMode.INTERSECT, false)
        block()
    } finally {
        canvas.restore()
    }
}
