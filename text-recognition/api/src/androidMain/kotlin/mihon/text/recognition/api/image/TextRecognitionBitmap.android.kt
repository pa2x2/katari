package mihon.text.recognition.api.image

import android.graphics.Bitmap

actual typealias TextRecognitionBitmap = Bitmap

actual fun TextRecognitionBitmap.release() = recycle()
