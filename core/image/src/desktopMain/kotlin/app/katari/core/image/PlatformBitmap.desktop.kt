package app.katari.core.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap

actual class PlatformBitmap internal constructor(val skia: Bitmap)

actual val PlatformBitmap.pixelWidth: Int get() = skia.width

actual val PlatformBitmap.pixelHeight: Int get() = skia.height

actual val PlatformBitmap.byteCount: Long get() = skia.rowBytes.toLong() * skia.height

actual fun PlatformBitmap.argbPixel(x: Int, y: Int): Int = skia.getColor(x, y)

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
