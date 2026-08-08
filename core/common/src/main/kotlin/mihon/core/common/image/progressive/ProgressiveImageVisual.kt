package mihon.core.common.image.progressive

import android.graphics.Bitmap

sealed interface ProgressiveImageVisual {
    val bitmap: Bitmap
    val generation: Long

    data class Still(
        override val bitmap: Bitmap,
        val updatedRegion: ProgressiveImageRegion,
        override val generation: Long,
    ) : ProgressiveImageVisual

    data class AnimationFrame(
        override val bitmap: Bitmap,
        val frame: ProgressiveAnimationFrame,
        override val generation: Long,
    ) : ProgressiveImageVisual
}

data class ProgressiveAnimationFrame(
    val index: Int,
    val durationMillis: Long,
    val updatedRegion: ProgressiveImageRegion,
    val blendOperation: ProgressiveBlendOperation,
    val disposalOperation: ProgressiveDisposalOperation,
)

enum class ProgressiveBlendOperation {
    SOURCE,
    OVER,
}

enum class ProgressiveDisposalOperation {
    NONE,
    BACKGROUND,
    PREVIOUS,
}
