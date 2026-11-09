package app.katari.entry.interactions.manga.reader.text.image

import app.katari.core.image.PlatformBitmap
import app.katari.core.image.argbPixels
import app.katari.core.image.pixelHeight
import app.katari.core.image.pixelWidth
import app.katari.core.image.release
import app.katari.text.recognition.api.image.ArgbPixels
import app.katari.text.recognition.api.image.TextRecognitionBitmap

/** Copies the pixels out of Skia's bitmap, which is freed at once rather than waiting for collection. */
internal actual fun PlatformBitmap.toTextRecognitionBitmap(): TextRecognitionBitmap = try {
    TextRecognitionBitmap(ArgbPixels(pixelWidth, pixelHeight, argbPixels()))
} finally {
    release()
}
