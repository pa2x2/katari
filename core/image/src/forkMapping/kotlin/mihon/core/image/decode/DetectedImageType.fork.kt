package mihon.core.image.decode

import mihon.core.image.progressive.toProgressiveFormat
import tachiyomi.decoder.ImageDecoder

actual fun detectImageType(header: ByteArray): DetectedImageType? =
    ImageDecoder.findType(header)?.let { DetectedImageType(it.format.toProgressiveFormat(), it.isAnimated) }
