package mihon.desktop.application.icon

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.skiaCanvas
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import java.io.IOException

/**
 * The app's icon, which the build draws from the Android app's launcher icon of the same build and the desktop entry
 * names too. Windows and notifications carry it themselves: an AppImage nobody integrated and a run from the build
 * tool have no desktop entry installed, so the desktop has nowhere else to find it.
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
        val surface = Surface.makeRasterN32Premul(size, size)
        document().apply { setContainerSize(size.toFloat(), size.toFloat()) }.render(surface.canvas)
        val png = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)
            ?: throw IOException("Couldn't encode the app's icon")
        return png.bytes
    }

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
