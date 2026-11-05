package mihon.core.image

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import tachiyomi.decoder.PixelAllocator
import tachiyomi.decoder.PixelTarget

/**
 * A Skia bitmap the decoder writes straight into through its pixel address. Kept apart from [PlatformBitmap] so code
 * that only draws bitmaps doesn't need the decoder on its classpath.
 */
internal class SkiaPixelTarget private constructor(private val skia: Bitmap) : PixelTarget {
    override val width: Int get() = skia.width

    override val height: Int get() = skia.height

    override val address: Long = checkNotNull(skia.peekPixels()).addr

    override val rowBytes: Long get() = skia.rowBytes.toLong()

    fun toPlatformBitmap(): PlatformBitmap = PlatformBitmap(skia)

    companion object {
        /** Allocates bitmaps in the decoder's layout: RGBA bytes, premultiplied. */
        val allocator = PixelAllocator { width, height ->
            val bitmap = Bitmap()
            val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
            if (bitmap.allocPixels(info)) SkiaPixelTarget(bitmap) else null.also { bitmap.close() }
        }
    }
}
