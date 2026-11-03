package mihon.entry.interactions.manga.reader.text.overlay

import android.graphics.Color
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.TextRecognitionImage

/**
 * The color text is printed on inside [area]: the most common color of a small copy of the area. Text strokes cover
 * far less of a speech bubble than its background, so the majority color is the paper or bubble fill.
 */
internal suspend fun sampleTextBackground(image: TextRecognitionImage, area: ImageRect): Int {
    var sampleSize = 1
    while (maxOf(area.width, area.height) / (sampleSize * 2) >= SAMPLE_EDGE) sampleSize *= 2
    val bitmap = image.decodeRegion(area, sampleSize)
    try {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val counts = HashMap<Int, Int>()
        val buckets = IntArray(pixels.size) { index ->
            val pixel = pixels[index]
            Triple(
                Color.red(pixel) shr QUANTIZE,
                Color.green(pixel) shr QUANTIZE,
                Color.blue(pixel) shr QUANTIZE,
            ).hashBucket().also { counts[it] = (counts[it] ?: 0) + 1 }
        }
        val dominant = counts.maxByOrNull { it.value }?.key ?: return Color.WHITE
        // The exact color is the mean of the dominant bucket, so the cover blends with the paper it hides.
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0
        pixels.forEachIndexed { index, pixel ->
            if (buckets[index] != dominant) return@forEachIndexed
            red += Color.red(pixel)
            green += Color.green(pixel)
            blue += Color.blue(pixel)
            count++
        }
        return Color.rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
    } finally {
        bitmap.recycle()
    }
}

private fun Triple<Int, Int, Int>.hashBucket(): Int =
    (first shl RED_SHIFT) or (second shl GREEN_SHIFT) or (third shl BLUE_SHIFT)

private const val SAMPLE_EDGE = 48
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BLUE_SHIFT = 0

/** Bits dropped per channel, so antialiasing and paper texture fall into one bucket. */
private const val QUANTIZE = 4
