package app.katari.desktop.application.icon

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.skiaCanvas
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import java.io.IOException

/**
 * The app's icon, which the build draws from the Android app's launcher icon of the same build and the desktop entry
 * names too. Windows, notifications and the tray carry it themselves: an AppImage nobody integrated and a run from the
 * build tool have no desktop entry installed, so the desktop has nowhere else to find it.
 */
object AppIcon {
    private val svg: ByteArray by lazy {
        val resource = AppIcon::class.java.getResourceAsStream(RESOURCE) ?: error("The app has no $RESOURCE")
        resource.use { it.readBytes() }
    }

    /** Draws the icon at whatever size it is given, as a window's icon is drawn at the size its toolkit picks. */
    fun painter(): Painter = SvgPainter(document())

    /** The icon as a PNG of [size] pixels a side. */
    fun png(size: Int): ByteArray {
        val png = render(size).makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)
            ?: throw IOException("Couldn't encode the app's icon")
        return png.bytes
    }

    /**
     * The icon as [size] pixels a side, row by row from the top left, each an ARGB colour whose colour channels are not
     * multiplied by its alpha.
     */
    fun argb(size: Int): IntArray {
        val surface = render(size)
        val bitmap = Bitmap().apply { allocPixels(ImageInfo(size, size, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)) }
        check(surface.readPixels(bitmap, 0, 0)) { "Couldn't read the app's icon" }
        val bgra = checkNotNull(bitmap.readPixels()) { "Couldn't read the app's icon" }
        return IntArray(size * size) { pixel ->
            val at = pixel * 4
            bgra[at + 3].channel() shl 24 or (bgra[at + 2].channel() shl 16) or (bgra[at + 1].channel() shl 8) or
                bgra[at].channel()
        }
    }

    private fun render(size: Int): Surface {
        val surface = Surface.makeRasterN32Premul(size, size)
        document().apply { setContainerSize(size.toFloat(), size.toFloat()) }.render(surface.canvas)
        return surface
    }

    private fun Byte.channel(): Int = toInt() and 0xFF

    // A document per user: rendering sets its size
    private fun document() = SVGDOM(Data.makeFromBytes(svg))

    private class SvgPainter(private val document: SVGDOM) : Painter() {
        override val intrinsicSize: Size = Size.Unspecified

        override fun DrawScope.onDraw() {
            document.setContainerSize(size.width, size.height)
            drawIntoCanvas { document.render(it.skiaCanvas) }
        }
    }

    private const val RESOURCE = "/app-icon.svg"
}
