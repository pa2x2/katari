package mihon.text.recognition.ui.playground

import android.graphics.Bitmap
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.image.TextRecognitionImage

/** An already decoded, software-backed image, for hosts that hold the whole bitmap anyway. */
class BitmapTextRecognitionImage(
    private val bitmap: Bitmap,
    override val key: ImageContentKey,
) : TextRecognitionImage {
    override val size = ImageSize(bitmap.width, bitmap.height)

    /**
     * Always returns a new bitmap, because callers recycle what they receive.
     *
     * Subsampling averages every block of source pixels, as page decoders do: the region is halved step by step, and
     * a filtered scale to exactly half averages each 2×2 block. One filtered scale by more than half would skip
     * source pixels and lose thin strokes.
     */
    override suspend fun decodeRegion(region: ImageRect, sampleSize: Int): Bitmap {
        var result = Bitmap.createBitmap(bitmap, region.left, region.top, region.width, region.height)
        if (result === bitmap) result = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        var remaining = sampleSize
        while (remaining > 1 && result.width > 1 && result.height > 1) {
            val halved = Bitmap.createScaledBitmap(result, result.width / 2, result.height / 2, true)
            result.recycle()
            result = halved
            remaining /= 2
        }
        return result
    }
}
