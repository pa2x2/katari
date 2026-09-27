package mihon.text.recognition.provider.onnx.tensor

import android.graphics.Bitmap
import java.nio.FloatBuffer

/**
 * Resizes [bitmap] to [width]×[height] and writes it as a planar (channel, row, column) float tensor. [channel]
 * converts one ARGB pixel to the three channel values.
 *
 * The tensor is [tensorWidth] columns wide; columns right of the image hold [padding], as recognizers that read
 * lines of different lengths expect.
 */
internal inline fun planarTensor(
    bitmap: Bitmap,
    width: Int,
    height: Int,
    tensorWidth: Int = width,
    padding: Float = 0f,
    channel: (pixel: Int, index: Int) -> Float,
): FloatBuffer {
    require(tensorWidth >= width)
    val pixels = resamplePixels(bitmap.pixels(), bitmap.width, bitmap.height, width, height)
    val plane = tensorWidth * height
    val buffer = FloatBuffer.allocate(3 * plane)
    if (tensorWidth > width) {
        for (index in 0 until buffer.capacity()) buffer.put(index, padding)
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = pixels[y * width + x]
            val index = y * tensorWidth + x
            buffer.put(index, channel(pixel, 0))
            buffer.put(plane + index, channel(pixel, 1))
            buffer.put(2 * plane + index, channel(pixel, 2))
        }
    }
    return buffer
}

/** The ARGB pixels of [this], read from a software copy when the bitmap lives in graphics memory. */
internal fun Bitmap.pixels(): IntArray {
    val source = if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    if (source !== this) source.recycle()
    return pixels
}

internal fun Int.red(): Int = (this shr 16) and 0xFF

internal fun Int.green(): Int = (this shr 8) and 0xFF

internal fun Int.blue(): Int = this and 0xFF
