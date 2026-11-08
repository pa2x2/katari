package app.katari.core.image.decode

import app.katari.core.image.progressive.toProgressiveFormat
import tachiyomi.decoder.ImageDecoder

actual fun detectImageType(header: ByteArray): DetectedImageType? =
    ImageDecoder.findType(header)?.let { DetectedImageType(it.format.toProgressiveFormat(), it.isAnimated) }
