package app.katari.entry.interactions.manga.reader.text.image

import app.katari.core.image.PlatformBitmap
import app.katari.text.recognition.api.image.TextRecognitionBitmap

/** The decoded region as recognition reads it; this bitmap is not used afterwards. */
internal expect fun PlatformBitmap.toTextRecognitionBitmap(): TextRecognitionBitmap
