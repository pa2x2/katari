package app.katari.core.image

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap as asComposeImageBitmap

actual typealias PlatformBitmap = Bitmap

actual val PlatformBitmap.pixelWidth: Int get() = width

actual val PlatformBitmap.pixelHeight: Int get() = height

actual val PlatformBitmap.byteCount: Long get() = allocationByteCount.toLong()

actual fun PlatformBitmap.argbPixel(x: Int, y: Int): Int = getPixel(x, y)

actual fun PlatformBitmap.argbPixels(): IntArray =
    IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }

actual fun PlatformBitmap.release() = recycle()

actual fun PlatformBitmap.asImageBitmap(): ImageBitmap = asComposeImageBitmap()

actual fun PlatformBitmap.copyRows(top: Int, bottom: Int): PlatformBitmap {
    // createBitmap may return this bitmap itself and returns a mutable one despite its docs; only a copy is immutable
    val rows = Bitmap.createBitmap(this, 0, top, width, bottom - top)
    val copy = checkNotNull(rows.copy(config ?: Bitmap.Config.ARGB_8888, false))
    if (rows !== this) rows.recycle()
    // Starts uploading it to the GPU on the render thread, before it is first drawn
    copy.prepareToDraw()
    return copy
}
