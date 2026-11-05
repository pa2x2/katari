package mihon.core.common.image.progressive

import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

/**
 * Decodes images while they download, so a reader can draw a page before its last byte arrives. A session is opened
 * per image and fed the downloaded bytes; where the engine is off or busy, [openSession] gives none and the image is
 * read once it has downloaded.
 */
expect class ProgressiveImageEngine : Closeable {
    val isEnabled: Boolean

    fun openSession(options: ProgressiveImageDecodeOptions = ProgressiveImageDecodeOptions()): ProgressiveImageSession?

    override fun close()
}

/** One image being decoded as its bytes arrive; [state] is what has been decoded so far. */
expect class ProgressiveImageSession : Closeable {
    val state: StateFlow<ProgressiveImageState>

    fun append(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset)

    fun finish()

    fun fail(error: Throwable)

    override fun close()
}
