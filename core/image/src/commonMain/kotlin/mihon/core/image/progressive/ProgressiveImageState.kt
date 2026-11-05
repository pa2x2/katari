package mihon.core.image.progressive

/** What a progressive decode has produced so far, for previews and readers that draw partial images. */
data class ProgressiveImageState(
    val format: ProgressiveImageFormat? = null,
    val capabilities: ProgressiveImageCapabilities? = null,
    val metadata: ProgressiveImageMetadata? = null,
    val visual: ProgressiveImageVisual? = null,
    val animation: ProgressiveAnimationBuffer? = null,
    val status: ProgressiveImageStatus = ProgressiveImageStatus.Receiving,
) {
    /**
     * Whether it holds a whole animation that can play again from what was decoded, so a reader keeps showing it
     * rather than the finished file once that is ready.
     */
    val holdsReplayableAnimation: Boolean
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
