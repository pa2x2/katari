package mihon.core.common.image.progressive

data class ProgressiveImageState(
    val format: ProgressiveImageFormat? = null,
    val capabilities: ProgressiveImageCapabilities? = null,
    val metadata: ProgressiveImageMetadata? = null,
    val visual: ProgressiveImageVisual? = null,
    val status: ProgressiveImageStatus = ProgressiveImageStatus.Receiving,
)

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
