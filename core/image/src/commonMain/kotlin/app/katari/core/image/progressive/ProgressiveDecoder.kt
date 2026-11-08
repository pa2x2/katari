package app.katari.core.image.progressive

import app.katari.core.image.PlatformBitmap
import java.io.Closeable

/**
 * The platform's incremental decoder, which takes encoded bytes as they arrive and publishes what they decode to, as
 * [pollUpdate] returns it. Calls come from one session at a time.
 */
internal interface ProgressiveDecoder : Closeable {
    fun append(bytes: ByteArray, offset: Int, length: Int)

    /** Signals that no more bytes come. */
    fun finish()

    /** The next update, or null until more bytes have been appended. */
    fun pollUpdate(): ProgressiveDecodeUpdate?
}

/** Opens the platform's decoder for [options]. */
internal expect fun openProgressiveDecoder(options: ProgressiveImageDecodeOptions): ProgressiveDecoder

internal sealed interface ProgressiveDecodeUpdate {
    data class FormatDetected(
        val format: ProgressiveImageFormat,
        val capabilities: ProgressiveImageCapabilities,
    ) : ProgressiveDecodeUpdate

    data class MetadataAvailable(val metadata: ProgressiveImageMetadata) : ProgressiveDecodeUpdate

    /** Pixels the receiver owns from now on. */
    data class StillImageAvailable(
        val pixels: PlatformBitmap,
        val updatedRegion: ProgressiveImageRegion,
        val generation: Long,
    ) : ProgressiveDecodeUpdate

    /** A whole animation canvas the receiver owns from now on. */
    data class AnimationFrameAvailable(
        val pixels: PlatformBitmap,
        val frame: ProgressiveAnimationFrame,
        val generation: Long,
    ) : ProgressiveDecodeUpdate

    data class Complete(val metadata: ProgressiveImageMetadata) : ProgressiveDecodeUpdate

    data class Unsupported(val format: ProgressiveImageFormat?) : ProgressiveDecodeUpdate

    data class Error(val message: String) : ProgressiveDecodeUpdate
}
