package mihon.core.image

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
