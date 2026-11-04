package eu.kanade.tachiyomi.ui.video.player.subtitle

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import mihon.video.engine.spi.player.SubtitleImage
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

// An ARGB int laid out little-endian is the bytes B, G, R, A
internal actual fun SubtitleImage.toImageBitmap(): ImageBitmap {
    val bytes = ByteArray(pixels.size * BYTES_PER_PIXEL)
    pixels.forEachIndexed { index, argb ->
        val at = index * BYTES_PER_PIXEL
        bytes[at] = argb.toByte()
        bytes[at + 1] = (argb shr 8).toByte()
        bytes[at + 2] = (argb shr 16).toByte()
        bytes[at + 3] = (argb ushr 24).toByte()
    }
    val bitmap = Bitmap()
    bitmap.installPixels(
        ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL),
        bytes,
        width * BYTES_PER_PIXEL,
    )
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}

private const val BYTES_PER_PIXEL = 4
