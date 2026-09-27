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

    /** Always returns a new bitmap, because callers recycle what they receive. */
    override suspend fun decodeRegion(region: ImageRect, sampleSize: Int): Bitmap {
        val width = (region.width / sampleSize).coerceAtLeast(1)
        val height = (region.height / sampleSize).coerceAtLeast(1)
        val cropped = Bitmap.createBitmap(bitmap, region.left, region.top, region.width, region.height)
        val scaled = Bitmap.createScaledBitmap(cropped, width, height, true)
        return when {
            scaled === bitmap -> bitmap.copy(Bitmap.Config.ARGB_8888, false)
            scaled !== cropped -> scaled.also { if (cropped !== bitmap) cropped.recycle() }
            else -> scaled
        }
    }
}
