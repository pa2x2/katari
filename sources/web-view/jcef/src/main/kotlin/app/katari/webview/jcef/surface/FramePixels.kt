package app.katari.webview.jcef.surface

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.awt.Rectangle
import java.nio.ByteBuffer

/**
 * The last picture Chromium painted of one of a page's widgets, the page or a popup over it, kept as its pixels. Chromium
 * paints on a thread of its own and hands over the whole picture each time, with the parts that changed; those are
 * copied here as they come, and Compose takes an image of the whole once per frame it draws.
 *
 * Each image holds a copy of the picture in native memory, which the garbage collector, seeing only a small object,
 * would leave for long while the page keeps painting, a picture of a full-screen page at a time. The copy is freed as
 * soon as the next image replaces it instead.
 */
internal class FramePixels {
    private var width = 0
    private var height = 0
    private var pixels = ByteArray(0)
    private var version = 0L

    // The image of the last version taken, which a frame drawn for another reason reuses, and the pixels it draws
    private var image: ImageBitmap? = null
    private var imageBitmap: Bitmap? = null
    private var imageVersion = -1L

    /** Copies [dirty], the parts of [buffer] that changed, a [width] by [height] picture in BGRA. */
    @Synchronized
    fun write(buffer: ByteBuffer, width: Int, height: Int, dirty: Array<Rectangle>) {
        val rowBytes = width * BYTES_PER_PIXEL
        if (width != this.width || height != this.height) {
            this.width = width
            this.height = height
            pixels = ByteArray(rowBytes * height)
            buffer.get(0, pixels, 0, pixels.size)
        } else {
            dirty.forEach { rect ->
                val left = rect.x.coerceIn(0, width)
                val right = (rect.x + rect.width).coerceIn(left, width)
                val bytes = (right - left) * BYTES_PER_PIXEL
                for (row in rect.y.coerceIn(0, height) until (rect.y + rect.height).coerceIn(0, height)) {
                    val offset = row * rowBytes + left * BYTES_PER_PIXEL
                    buffer.get(offset, pixels, offset, bytes)
                }
            }
        }
        version++
    }

    /** Forgets the picture, as when a popup closes; the last image stays until [releaseImage]. */
    @Synchronized
    fun clear() {
        width = 0
        height = 0
        pixels = ByteArray(0)
        version++
    }

    /**
     * The picture as an image Compose draws at its size in pixels; null before Chromium painted any. On the main thread,
     * which must draw the image last returned no more: a new one frees it.
     */
    @Synchronized
    fun image(): ImageBitmap? {
        if (width == 0 || height == 0) return null
        if (imageVersion != version) {
            val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL)
            // Immutable, so drawing shares its pixels rather than copying them each frame
            val bitmap = Bitmap()
            bitmap.installPixels(info, pixels, width * BYTES_PER_PIXEL)
            bitmap.setImmutable()
            releaseImage()
            imageBitmap = bitmap
            image = bitmap.asComposeImageBitmap()
            imageVersion = version
        }
        return image
    }

    /** Frees the last image; on the main thread, once nothing draws it. A frame already drawn keeps its own reference. */
    @Synchronized
    fun releaseImage() {
        imageBitmap?.close()
        imageBitmap = null
        image = null
        imageVersion = -1L
    }

    private companion object {
        const val BYTES_PER_PIXEL = 4
    }
}
