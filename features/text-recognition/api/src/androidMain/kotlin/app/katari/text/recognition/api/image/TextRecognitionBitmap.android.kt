package app.katari.text.recognition.api.image

import android.graphics.Bitmap

actual typealias TextRecognitionBitmap = Bitmap

actual fun TextRecognitionBitmap.release() = recycle()

actual fun TextRecognitionBitmap.readArgbPixels(): IntArray =
    IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
