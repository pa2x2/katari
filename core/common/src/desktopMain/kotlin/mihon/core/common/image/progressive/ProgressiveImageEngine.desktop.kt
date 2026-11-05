package mihon.core.common.image.progressive

import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

/** Desktop decodes no image progressively yet, so every image is read once it has downloaded. */
actual class ProgressiveImageEngine : Closeable {
    actual val isEnabled: Boolean = false

    actual fun openSession(options: ProgressiveImageDecodeOptions): ProgressiveImageSession? = null

    actual override fun close() = Unit
}

/** Never made on desktop, as [ProgressiveImageEngine] opens no session there. */
actual class ProgressiveImageSession private constructor() : Closeable {
    actual val state: StateFlow<ProgressiveImageState> get() = throw UnsupportedOperationException()

    actual fun append(bytes: ByteArray, offset: Int, length: Int) {
        throw UnsupportedOperationException()
    }

    actual fun finish() {
        throw UnsupportedOperationException()
    }

    actual fun fail(error: Throwable) {
        throw UnsupportedOperationException()
    }

    actual override fun close() {
        throw UnsupportedOperationException()
    }
}
