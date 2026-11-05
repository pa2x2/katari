package mihon.core.common.image.progressive

actual data class ProgressiveImageState(
    val format: ProgressiveImageFormat? = null,
    val capabilities: ProgressiveImageCapabilities? = null,
    val metadata: ProgressiveImageMetadata? = null,
    val visual: ProgressiveImageVisual? = null,
    val animation: ProgressiveAnimationBuffer? = null,
    val status: ProgressiveImageStatus = ProgressiveImageStatus.Receiving,
) {
    actual val holdsReplayableAnimation: Boolean
        get() = animation?.let { it.isComplete && it.isReplayable } == true
}

sealed interface ProgressiveImageStatus {
    data object Receiving : ProgressiveImageStatus
    data object Complete : ProgressiveImageStatus
    data object Unsupported : ProgressiveImageStatus
    data object Disabled : ProgressiveImageStatus
    data object Cancelled : ProgressiveImageStatus

    data class Failed(
        val message: String,
    ) : ProgressiveImageStatus
}
