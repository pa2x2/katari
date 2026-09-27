package mihon.text.recognition.provider.onnx.tensor

import android.graphics.Bitmap
import java.nio.FloatBuffer

/**
 * Scales [bitmap] to [width]×[height] and writes it as a planar (channel, row, column) float tensor. [channel]
 * converts one ARGB pixel to the three channel values.
 */
internal inline fun planarTensor(
    bitmap: Bitmap,
    width: Int,
    height: Int,
    channel: (pixel: Int, index: Int) -> Float,
): FloatBuffer {
    val source = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
    val scaled = Bitmap.createScaledBitmap(source, width, height, true)
    val pixels = IntArray(width * height)
    scaled.getPixels(pixels, 0, width, 0, 0, width, height)
    if (scaled !== source) scaled.recycle()
    if (source !== bitmap) source.recycle()

    val plane = width * height
    val buffer = FloatBuffer.allocate(3 * plane)
    for (index in pixels.indices) {
        val pixel = pixels[index]
        buffer.put(index, channel(pixel, 0))
        buffer.put(plane + index, channel(pixel, 1))
        buffer.put(2 * plane + index, channel(pixel, 2))
    }
    return buffer
}

internal fun Int.red(): Int = (this shr 16) and 0xFF

internal fun Int.green(): Int = (this shr 8) and 0xFF

internal fun Int.blue(): Int = this and 0xFF
