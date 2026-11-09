package app.katari.entry.interactions.manga.reader.text.overlay

import app.katari.text.recognition.api.image.ImageRect
import app.katari.text.recognition.api.image.TextRecognitionImage
import app.katari.text.recognition.api.image.readPixels
import app.katari.text.recognition.api.image.release

/**
 * The color text is printed on inside [area]: the most common color of a small copy of the area. Text strokes cover
 * far less of a speech bubble than its background, so the majority color is the paper or bubble fill.
 */
internal suspend fun sampleTextBackground(image: TextRecognitionImage, area: ImageRect): Int {
    var sampleSize = 1
    while (maxOf(area.width, area.height) / (sampleSize * 2) >= SAMPLE_EDGE) sampleSize *= 2
    val bitmap = image.decodeRegion(area, sampleSize)
    try {
        val pixels = bitmap.readPixels().values
        val counts = HashMap<Int, Int>()
        val buckets = IntArray(pixels.size) { index ->
            val pixel = pixels[index]
            Triple(
                pixel.red shr QUANTIZE,
                pixel.green shr QUANTIZE,
                pixel.blue shr QUANTIZE,
            ).hashBucket().also { counts[it] = (counts[it] ?: 0) + 1 }
        }
        val dominant = counts.maxByOrNull { it.value }?.key ?: return OPAQUE_WHITE
        // The exact color is the mean of the dominant bucket, so the cover blends with the paper it hides.
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0
        pixels.forEachIndexed { index, pixel ->
            if (buckets[index] != dominant) return@forEachIndexed
            red += pixel.red
            green += pixel.green
            blue += pixel.blue
            count++
        }
        return opaqueRgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
    } finally {
        bitmap.release()
    }
}

private val Int.red get() = (this shr RED_SHIFT) and CHANNEL
private val Int.green get() = (this shr GREEN_SHIFT) and CHANNEL
private val Int.blue get() = (this shr BLUE_SHIFT) and CHANNEL

private fun opaqueRgb(red: Int, green: Int, blue: Int): Int =
    OPAQUE or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or (blue shl BLUE_SHIFT)

private fun Triple<Int, Int, Int>.hashBucket(): Int =
    (first shl RED_SHIFT) or (second shl GREEN_SHIFT) or (third shl BLUE_SHIFT)

private const val SAMPLE_EDGE = 48
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BLUE_SHIFT = 0
private const val CHANNEL = 0xFF
private const val OPAQUE = 0xFF shl 24
private const val OPAQUE_WHITE = -1

/** Bits dropped per channel, so antialiasing and paper texture fall into one bucket. */
private const val QUANTIZE = 4
