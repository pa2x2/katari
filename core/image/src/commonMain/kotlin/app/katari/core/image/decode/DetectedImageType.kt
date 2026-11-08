package app.katari.core.image.decode

import app.katari.core.image.progressive.ProgressiveImageFormat

/** What the decoder makes of an encoded image's header: its format, and whether it holds more than one frame. */
data class DetectedImageType(val format: ProgressiveImageFormat, val isAnimated: Boolean)

/** Reads the type from the first bytes of an encoded image, or null for content the decoder doesn't know. */
expect fun detectImageType(header: ByteArray): DetectedImageType?
