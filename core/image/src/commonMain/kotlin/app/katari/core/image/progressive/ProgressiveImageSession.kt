package app.katari.core.image.progressive

import app.katari.core.image.byteCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.Closeable
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** One image being decoded as its bytes arrive; [state] is what has been decoded so far. */
class ProgressiveImageSession internal constructor(
    private var decoder: ProgressiveDecoder?,
    private val onTerminated: (ProgressiveImageSession) -> Unit,
) : Closeable {
    private val mutableState = MutableStateFlow(ProgressiveImageState())
    private var nextPublication: TimeMark? = null
    private var terminal = false
    private var released = false

    val state: StateFlow<ProgressiveImageState> = mutableState.asStateFlow()
    internal val subscriptionCount: StateFlow<Int> = mutableState.subscriptionCount

    fun append(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset) {
        synchronized(this) {
            val activeDecoder = decoder ?: return
            try {
                activeDecoder.append(bytes, offset, length)
                val state = mutableState.value
                val animationNeedsImmediateDrain = state.capabilities?.animationFrames == true &&
                    state.metadata?.isAnimated != false
                if (animationNeedsImmediateDrain || nextPublication?.hasPassedNow() != false) {
                    drainUpdates(activeDecoder)
                    nextPublication = TimeSource.Monotonic.markNow() + MINIMUM_PUBLICATION_INTERVAL
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
            mutableState.value = mutableState.value.copy(
                visual = null,
                animation = null,
                status = ProgressiveImageStatus.Disabled,
            )
            terminateLocked()
            releaseLocked()
        }
    }

    override fun close() {
        synchronized(this) {
            if (released) return
            mutableState.value = if (terminal) {
                mutableState.value.copy(visual = null, animation = null)
            } else {
                mutableState.value.copy(
                    visual = null,
                    animation = null,
                    status = ProgressiveImageStatus.Cancelled,
                )
            }
            terminateLocked()
            releaseLocked()
        }
    }

    internal fun releaseIfUnobservedAndTerminal() {
        synchronized(this) {
            if (terminal && subscriptionCount.value == 0) {
                mutableState.value = mutableState.value.copy(visual = null, animation = null)
                releaseLocked()
            }
        }
    }

    private fun drainUpdates(activeDecoder: ProgressiveDecoder) {
        while (!terminal) {
            when (val update = activeDecoder.pollUpdate() ?: return) {
                is ProgressiveDecodeUpdate.FormatDetected -> {
                    mutableState.value = mutableState.value.copy(
                        format = update.format,
                        capabilities = update.capabilities,
                    )
                }
                is ProgressiveDecodeUpdate.MetadataAvailable -> {
                    mutableState.value = mutableState.value.copy(metadata = update.metadata)
                }
                is ProgressiveDecodeUpdate.StillImageAvailable -> {
                    mutableState.value = mutableState.value.copy(
                        visual = ProgressiveImageVisual.Still(
                            bitmap = update.pixels,
                            updatedRegion = update.updatedRegion,
                            generation = update.generation,
                        ),
                    )
                }
                is ProgressiveDecodeUpdate.AnimationFrameAvailable -> {
                    val frame = update.frame
                    val frameVisual = ProgressiveAnimationFrameVisual(
                        bitmap = update.pixels,
                        frame = frame,
                        generation = update.generation,
                    )
                    val currentAnimation = mutableState.value.animation ?: ProgressiveAnimationBuffer(
                        frames = emptyList(),
                        retainedBytes = 0,
                    )
                    val frameBytes = update.pixels.byteCount
                    val retainFrame = currentAnimation.isReplayable &&
                        currentAnimation.frames.size < MAXIMUM_RETAINED_ANIMATION_FRAMES &&
                        currentAnimation.retainedBytes + frameBytes <= MAXIMUM_RETAINED_ANIMATION_BYTES
                    mutableState.value = mutableState.value.copy(
                        visual = ProgressiveImageVisual.AnimationFrame(
                            bitmap = update.pixels,
                            frame = frame,
                            generation = update.generation,
                        ),
                        animation = if (retainFrame) {
                            currentAnimation.copy(
                                frames = currentAnimation.frames + frameVisual,
                                retainedBytes = currentAnimation.retainedBytes + frameBytes,
                            )
                        } else {
                            currentAnimation.copy(isReplayable = false)
                        },
                    )
                }
                is ProgressiveDecodeUpdate.Complete -> {
                    val metadata = update.metadata
                    val animation = mutableState.value.animation?.let { buffer ->
                        val retainedEveryFrame = metadata.frameCount?.let { it == buffer.frames.size } ?: true
                        buffer.copy(
                            isComplete = true,
                            isReplayable = buffer.isReplayable && retainedEveryFrame,
                            loopCount = metadata.loopCount,
                        )
                    }
                    mutableState.value = mutableState.value.copy(
                        metadata = metadata,
                        animation = animation,
                        status = ProgressiveImageStatus.Complete,
                    )
                    terminateLocked()
                }
                is ProgressiveDecodeUpdate.Unsupported -> {
                    mutableState.value = mutableState.value.copy(
                        format = update.format,
                        visual = null,
                        animation = null,
                        status = ProgressiveImageStatus.Unsupported,
                    )
                    terminateLocked()
                }
                is ProgressiveDecodeUpdate.Error -> {
                    failLocked(IllegalStateException(update.message))
                }
            }
        }
    }

    private fun failLocked(error: Throwable) {
        logcat(LogPriority.WARN, error) { "Progressive image decoding stopped" }
        mutableState.value = mutableState.value.copy(
            status = ProgressiveImageStatus.Failed(
                error.message ?: error::class.simpleName.orEmpty(),
            ),
        )
        terminateLocked()
    }

    private fun terminateLocked() {
        if (terminal) return
        terminal = true
        decoder?.close()
        decoder = null
        if (subscriptionCount.value == 0) {
            mutableState.value = mutableState.value.copy(visual = null, animation = null)
            releaseLocked()
        }
    }

    private fun releaseLocked() {
        if (released) return
        released = true
        onTerminated(this)
    }
}

private val MINIMUM_PUBLICATION_INTERVAL = 100.milliseconds
private const val MAXIMUM_RETAINED_ANIMATION_FRAMES = 120
private const val MAXIMUM_RETAINED_ANIMATION_BYTES = 64L * 1_024 * 1_024
