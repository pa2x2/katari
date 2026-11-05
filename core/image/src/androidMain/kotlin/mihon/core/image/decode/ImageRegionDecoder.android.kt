package mihon.core.image.decode

import android.graphics.Rect
import androidx.compose.ui.unit.IntRect
import mihon.core.image.PlatformBitmap
import tachiyomi.decoder.ImageDecoder
import java.io.InputStream

actual class ImageRegionDecoder private constructor(private val decoder: ImageDecoder) : AutoCloseable {
    actual val width: Int get() = decoder.width

    actual val height: Int get() = decoder.height

    actual val bounds: IntRect = decoder.bounds.let { IntRect(it.left, it.top, it.right, it.bottom) }

    actual fun decode(region: IntRect, sampleSize: Int): PlatformBitmap? =
        decoder.decode(Rect(region.left, region.top, region.right, region.bottom), sampleSize)

    actual override fun close() = decoder.recycle()

    actual companion object {
        actual fun open(stream: InputStream, cropBorders: Boolean, displayProfile: ByteArray?): ImageRegionDecoder? =
            ImageDecoder.newInstance(stream, cropBorders, displayProfile)?.let(::ImageRegionDecoder)
    }
}
