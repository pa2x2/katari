package app.katari.text.recognition.ui.playground

import app.katari.text.recognition.api.image.ArgbPixels
import app.katari.text.recognition.api.image.ImageContentKey
import app.katari.text.recognition.api.image.ImageRect
import app.katari.text.recognition.api.image.ImageSize
import app.katari.text.recognition.api.image.TextRecognitionBitmap
import app.katari.text.recognition.api.image.TextRecognitionImage

/** An already decoded image, for hosts that hold the whole picture anyway. */
class PixelsTextRecognitionImage(
    private val pixels: ArgbPixels,
    override val key: ImageContentKey,
) : TextRecognitionImage {
    override val size = ImageSize(pixels.width, pixels.height)

    /** Each output pixel averages its [sampleSize]×[sampleSize] block of the region, as page decoders do. */
    override suspend fun decodeRegion(region: ImageRect, sampleSize: Int): TextRecognitionBitmap {
        val width = maxOf(1, region.width / sampleSize)
        val height = maxOf(1, region.height / sampleSize)
        val values = IntArray(width * height) { index ->
            val left = region.left + index % width * sampleSize
            val top = region.top + index / width * sampleSize
            averageBlock(left, top, minOf(left + sampleSize, region.right), minOf(top + sampleSize, region.bottom))
        }
        return TextRecognitionBitmap(ArgbPixels(width, height, values))
    }

    private fun averageBlock(left: Int, top: Int, right: Int, bottom: Int): Int {
        val sums = LongArray(CHANNEL_SHIFTS.size)
        for (y in top until bottom) {
            for (x in left until right) {
                val pixel = pixels[x, y]
                CHANNEL_SHIFTS.forEachIndexed { channel, shift ->
                    sums[channel] += ((pixel ushr shift) and 0xFF).toLong()
                }
            }
        }
        val count = (right - left).toLong() * (bottom - top)
        return CHANNEL_SHIFTS.foldIndexed(0) { channel, result, shift ->
            result or ((sums[channel] / count).toInt() shl shift)
        }
    }

    private companion object {
        val CHANNEL_SHIFTS = intArrayOf(24, 16, 8, 0)
    }
}
