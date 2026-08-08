package mihon.core.common.image.progressive

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.decoder.Format
import tachiyomi.decoder.incremental.IncrementalBlendOperation
import tachiyomi.decoder.incremental.IncrementalDecodeUpdate
import tachiyomi.decoder.incremental.IncrementalDisposalOperation
import tachiyomi.decoder.incremental.IncrementalImageDecoder
import tachiyomi.decoder.incremental.IncrementalImageInfo
import tachiyomi.decoder.incremental.IncrementalImageRegion
import java.io.Closeable

class ProgressiveImageSession internal constructor(
    private var decoder: IncrementalImageDecoder?,
    private val onTerminated: (ProgressiveImageSession) -> Unit,
) : Closeable {
    private val mutableState = MutableStateFlow(ProgressiveImageState())
    private var nextPublicationTimeMillis = 0L
    private var terminal = false

    val state: StateFlow<ProgressiveImageState> = mutableState.asStateFlow()

    fun append(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset) {
        synchronized(this) {
            val activeDecoder = decoder ?: return
            try {
                activeDecoder.append(bytes, offset, length)
                val now = SystemClock.elapsedRealtime()
                if (now >= nextPublicationTimeMillis) {
                    drainUpdates(activeDecoder)
                    nextPublicationTimeMillis = now + MINIMUM_PUBLICATION_INTERVAL_MILLIS
                }
            } catch (error: Throwable) {
                failLocked(error)
            }
        }
    }

    fun finish() {
        synchronized(this) {
            val activeDecoder = decoder ?: return
            try {
                activeDecoder.finish()
                drainUpdates(activeDecoder)
                if (!terminal) {
                    failLocked(IllegalStateException("Progressive decoder did not reach a terminal state"))
                }
            } catch (error: Throwable) {
                failLocked(error)
            }
        }
    }

    fun fail(error: Throwable) {
        synchronized(this) {
            if (!terminal) failLocked(error)
        }
    }

    internal fun disable() {
        synchronized(this) {
            if (terminal) return
            mutableState.value = mutableState.value.copy(
                visual = null,
                status = ProgressiveImageStatus.Disabled,
            )
            terminateLocked()
        }
    }

    override fun close() {
        synchronized(this) {
            if (terminal) return
            mutableState.value = mutableState.value.copy(
                visual = null,
                status = ProgressiveImageStatus.Cancelled,
            )
            terminateLocked()
        }
    }

    private fun drainUpdates(activeDecoder: IncrementalImageDecoder) {
        while (!terminal) {
            when (val update = activeDecoder.pollUpdate() ?: return) {
                is IncrementalDecodeUpdate.FormatDetected -> {
                    mutableState.value = mutableState.value.copy(
                        format = update.format.toProgressiveFormat(),
                        capabilities = ProgressiveImageCapabilities(
                            stillImageUpdates = update.capabilities.stillImageUpdates,
                            animationFrames = update.capabilities.animationFrames,
                        ),
                    )
                }
                is IncrementalDecodeUpdate.MetadataAvailable -> {
                    mutableState.value = mutableState.value.copy(metadata = update.info.toProgressiveMetadata())
                }
                is IncrementalDecodeUpdate.StillImageAvailable -> {
                    mutableState.value = mutableState.value.copy(
                        visual = ProgressiveImageVisual.Still(
                            bitmap = update.bitmap,
                            updatedRegion = update.updatedRegion.toProgressiveRegion(),
                            generation = update.generation,
                        ),
                    )
                }
                is IncrementalDecodeUpdate.AnimationFrameAvailable -> {
                    mutableState.value = mutableState.value.copy(
                        visual = ProgressiveImageVisual.AnimationFrame(
                            bitmap = update.bitmap,
                            frame = ProgressiveAnimationFrame(
                                index = update.frame.index,
                                durationMillis = update.frame.durationMillis,
                                updatedRegion = update.frame.updatedRegion.toProgressiveRegion(),
                                blendOperation = update.frame.blendOperation.toProgressiveBlendOperation(),
                                disposalOperation = update.frame.disposalOperation.toProgressiveDisposalOperation(),
                            ),
                            generation = update.generation,
                        ),
                    )
                }
                is IncrementalDecodeUpdate.Complete -> {
                    mutableState.value = mutableState.value.copy(
                        metadata = update.info.toProgressiveMetadata(),
                        status = ProgressiveImageStatus.Complete,
                    )
                    terminateLocked()
                }
                is IncrementalDecodeUpdate.Unsupported -> {
                    mutableState.value = mutableState.value.copy(
                        format = update.format?.toProgressiveFormat(),
                        visual = null,
                        status = ProgressiveImageStatus.Unsupported,
                    )
                    terminateLocked()
                }
                is IncrementalDecodeUpdate.Error -> {
                    failLocked(IllegalStateException(update.message))
                }
            }
        }
    }

    private fun failLocked(error: Throwable) {
        logcat(LogPriority.WARN, error) { "Progressive image decoding stopped" }
        mutableState.value = mutableState.value.copy(
            status = ProgressiveImageStatus.Failed(
                error.message ?: error::class.java.simpleName,
            ),
        )
        terminateLocked()
    }

    private fun terminateLocked() {
        if (terminal) return
        terminal = true
        decoder?.close()
        decoder = null
        onTerminated(this)
    }
}

private fun Format.toProgressiveFormat(): ProgressiveImageFormat = when (this) {
    Format.Jpeg -> ProgressiveImageFormat.JPEG
    Format.Png -> ProgressiveImageFormat.PNG
    Format.Webp -> ProgressiveImageFormat.WEBP
    Format.Gif -> ProgressiveImageFormat.GIF
    Format.Heif -> ProgressiveImageFormat.HEIF
    Format.Avif -> ProgressiveImageFormat.AVIF
    Format.Jxl -> ProgressiveImageFormat.JXL
}

private fun IncrementalImageInfo.toProgressiveMetadata() = ProgressiveImageMetadata(
    format = format.toProgressiveFormat(),
    width = width,
    height = height,
    outputWidth = outputWidth,
    outputHeight = outputHeight,
    isAnimated = isAnimated,
    hasAlpha = hasAlpha,
    frameCount = frameCount,
    loopCount = loopCount,
)

private fun IncrementalImageRegion.toProgressiveRegion() = ProgressiveImageRegion(
    left = left,
    top = top,
    right = right,
    bottom = bottom,
)

private fun IncrementalBlendOperation.toProgressiveBlendOperation() = when (this) {
    IncrementalBlendOperation.SOURCE -> ProgressiveBlendOperation.SOURCE
    IncrementalBlendOperation.OVER -> ProgressiveBlendOperation.OVER
}

private fun IncrementalDisposalOperation.toProgressiveDisposalOperation() = when (this) {
    IncrementalDisposalOperation.NONE -> ProgressiveDisposalOperation.NONE
    IncrementalDisposalOperation.BACKGROUND -> ProgressiveDisposalOperation.BACKGROUND
    IncrementalDisposalOperation.PREVIOUS -> ProgressiveDisposalOperation.PREVIOUS
}

private const val MINIMUM_PUBLICATION_INTERVAL_MILLIS = 100L
