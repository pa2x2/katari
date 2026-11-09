package app.katari.text.recognition.provider.onnx.tensor

import app.katari.text.recognition.api.image.ArgbPixels
import java.nio.FloatBuffer

/**
 * Resizes [image] to [width]×[height] and writes it as a planar (channel, row, column) float tensor. [channel]
 * converts one ARGB pixel to the three channel values.
 *
 * The tensor is [tensorWidth] columns wide; columns right of the image hold [padding], as recognizers that read
 * lines of different lengths expect.
 */
internal inline fun planarTensor(
    image: ArgbPixels,
    width: Int,
    height: Int,
    tensorWidth: Int = width,
    padding: Float = 0f,
    channel: (pixel: Int, index: Int) -> Float,
): FloatBuffer {
    require(tensorWidth >= width)
    val pixels = resamplePixels(image.values, image.width, image.height, width, height)
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

internal fun Int.red(): Int = (this shr 16) and 0xFF

internal fun Int.green(): Int = (this shr 8) and 0xFF

internal fun Int.blue(): Int = this and 0xFF
