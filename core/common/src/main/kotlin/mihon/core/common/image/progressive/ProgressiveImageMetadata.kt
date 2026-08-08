package mihon.core.common.image.progressive

enum class ProgressiveImageFormat {
    JPEG,
    PNG,
    WEBP,
    GIF,
    HEIF,
    AVIF,
    JXL,
}

data class ProgressiveImageCapabilities(
    val stillImageUpdates: Boolean,
    val animationFrames: Boolean,
)

data class ProgressiveImageMetadata(
    val format: ProgressiveImageFormat,
    val width: Int,
    val height: Int,
    val outputWidth: Int,
    val outputHeight: Int,
    val isAnimated: Boolean,
    val hasAlpha: Boolean,
    val frameCount: Int?,
    val loopCount: Int?,
)

data class ProgressiveImageRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)
