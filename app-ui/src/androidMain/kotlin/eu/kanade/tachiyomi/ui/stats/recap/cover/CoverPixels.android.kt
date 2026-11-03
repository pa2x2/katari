package eu.kanade.tachiyomi.ui.stats.recap.cover

import coil3.Bitmap
import coil3.request.ImageRequest
import coil3.request.allowHardware

// Pixels are read back on the CPU, which a hardware bitmap doesn't allow.
internal actual fun ImageRequest.Builder.readableOnCpu(): ImageRequest.Builder = allowHardware(false)

internal actual fun Bitmap.argbPixels(): IntArray {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return pixels
}
