package mihon.core.image.progressive

import mihon.core.image.PlatformBitmap

sealed interface ProgressiveImageVisual {
    val bitmap: PlatformBitmap
    val generation: Long

    data class Still(
        override val bitmap: PlatformBitmap,
        val updatedRegion: ProgressiveImageRegion,
        override val generation: Long,
    ) : ProgressiveImageVisual

    data class AnimationFrame(
        override val bitmap: PlatformBitmap,
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
    val bitmap: PlatformBitmap,
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
