package eu.kanade.tachiyomi.ui.stats.recap.cover

import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.Bitmap
import coil3.request.ImageRequest

// Desktop decodes every image into memory the CPU reads.
internal actual fun ImageRequest.Builder.readableOnCpu(): ImageRequest.Builder = this

internal actual fun Bitmap.argbPixels(): IntArray {
    val image = asComposeImageBitmap()
    return IntArray(image.width * image.height).also { image.readPixels(it) }
}
