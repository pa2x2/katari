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

data class ProgressiveAnimationBuffer(
    val frames: List<ProgressiveAnimationFrameVisual>,
    val retainedBytes: Long,
    val isComplete: Boolean = false,
    val isReplayable: Boolean = true,
    val loopCount: Int? = null,
)

data class ProgressiveAnimationFrameVisual(
    val bitmap: Bitmap,
    val frame: ProgressiveAnimationFrame,
    val generation: Long,
)

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
