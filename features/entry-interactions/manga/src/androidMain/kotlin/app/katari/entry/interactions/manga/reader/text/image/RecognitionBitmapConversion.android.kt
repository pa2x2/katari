package app.katari.entry.interactions.manga.reader.text.image

import app.katari.core.image.PlatformBitmap
import app.katari.text.recognition.api.image.TextRecognitionBitmap

/** Both are Android's bitmap, which recognition reads directly. */
internal actual fun PlatformBitmap.toTextRecognitionBitmap(): TextRecognitionBitmap = this
