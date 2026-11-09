package app.katari.text.recognition.api.image

import android.graphics.Bitmap

actual typealias TextRecognitionBitmap = Bitmap

actual fun TextRecognitionBitmap.release() = recycle()

/** Reads a software copy when the bitmap lives in graphics memory, whose pixels can't be read directly. */
actual fun TextRecognitionBitmap.readPixels(): ArgbPixels {
    val source = if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this
    try {
        val values = IntArray(width * height)
        source.getPixels(values, 0, width, 0, 0, width, height)
        return ArgbPixels(width, height, values)
    } finally {
        if (source !== this) source.recycle()
    }
}
