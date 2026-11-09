package app.katari.core.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder

actual class PlatformBitmap internal constructor(val skia: Bitmap)

actual val PlatformBitmap.pixelWidth: Int get() = skia.width

actual val PlatformBitmap.pixelHeight: Int get() = skia.height

actual val PlatformBitmap.byteCount: Long get() = skia.rowBytes.toLong() * skia.height

actual fun PlatformBitmap.argbPixel(x: Int, y: Int): Int = skia.getColor(x, y)

/** Unpremultiplied BGRA bytes in little-endian order are ARGB ints. */
actual fun PlatformBitmap.argbPixels(): IntArray {
    val info = ImageInfo(skia.width, skia.height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
    val bytes = checkNotNull(skia.readPixels(info, info.minRowBytes, 0, 0)) { "Pixels unreadable" }
    val pixels = IntArray(skia.width * skia.height)
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(pixels)
    return pixels
}

actual fun PlatformBitmap.release() = skia.close()

actual fun PlatformBitmap.asImageBitmap(): ImageBitmap = skia.asComposeImageBitmap()

actual fun PlatformBitmap.copyRows(top: Int, bottom: Int): PlatformBitmap {
    val info = skia.imageInfo.withWidthHeight(skia.width, bottom - top)
    val pixels = checkNotNull(skia.readPixels(info, info.minRowBytes, 0, top)) { "Rows $top until $bottom unreadable" }
    val rows = Bitmap()
    check(rows.installPixels(info, pixels, info.minRowBytes)) { "Rows $top until $bottom could not be copied" }
    rows.setImmutable()
    return PlatformBitmap(rows)
}
