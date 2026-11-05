package mihon.core.image.animation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mihon.core.image.PlatformBitmap
import mihon.core.image.progressive.ProgressiveDecodeUpdate
import mihon.core.image.progressive.ProgressiveDecoder
import mihon.core.image.progressive.ProgressiveImageDecodeOptions
import mihon.core.image.progressive.openProgressiveDecoder
import mihon.core.image.release

/**
 * Plays an animated image from its encoded [bytes]. Frames are decoded as they fall due, from a chunk of bytes at a
 * time, instead of all being held at once, and each loop starts over from the bytes. [options] bound each frame's
 * size.
 */
class AnimatedImagePlayback(
    private val bytes: ByteArray,
    private val options: ProgressiveImageDecodeOptions = ProgressiveImageDecodeOptions(),
) {
    /**
     * Shows each frame through [show] for its duration, as often as the image loops, until the caller is cancelled. A
     * frame stays valid until two more have been shown, so a screen still drawing it doesn't draw freed pixels; the
     * frames still shown when playback ends are left to the garbage collector. Returns false when the bytes held no
     * frame to show.
     */
    suspend fun play(show: (PlatformBitmap) -> Unit): Boolean = withContext(Dispatchers.Default) { playLoops(show) }

    private suspend fun playLoops(show: (PlatformBitmap) -> Unit): Boolean {
        val shown = ArrayDeque<PlatformBitmap>()
        var plays = 0
        while (true) {
            val outcome = openProgressiveDecoder(options).use { decoder ->
                playOnce(decoder) { frame, durationMillis ->
                    show(frame)
                    shown.addLast(frame)
                    while (shown.size > VALID_FRAMES) shown.removeFirst().release()
                    delay(durationMillis)
                }
            }
            plays++
            when (outcome) {
                is PlayOutcome.Played -> {
                    val loopCount = outcome.loopCount
                    if (loopCount != 0 && plays >= (loopCount ?: 1)) return true
                }
                PlayOutcome.Failed -> return plays > 1 || shown.isNotEmpty()
            }
        }
    }

    private suspend fun playOnce(
        decoder: ProgressiveDecoder,
        frame: suspend (PlatformBitmap, durationMillis: Long) -> Unit,
    ): PlayOutcome {
        var appended = 0
        while (true) {
            when (val update = decoder.pollUpdate()) {
                null -> {
                    if (appended == bytes.size) return PlayOutcome.Failed
                    val length = minOf(CHUNK_BYTES, bytes.size - appended)
                    decoder.append(bytes, appended, length)
                    appended += length
                    if (appended == bytes.size) decoder.finish()
                }
                is ProgressiveDecodeUpdate.AnimationFrameAvailable -> frame(update.pixels, frameDuration(update))
                // A still image shows its last decoded state, which the next update replaces
                is ProgressiveDecodeUpdate.StillImageAvailable -> frame(update.pixels, 0)
                is ProgressiveDecodeUpdate.Complete -> return PlayOutcome.Played(update.metadata.loopCount)
                is ProgressiveDecodeUpdate.Unsupported,
                is ProgressiveDecodeUpdate.Error,
                -> return PlayOutcome.Failed
                is ProgressiveDecodeUpdate.FormatDetected,
                is ProgressiveDecodeUpdate.MetadataAvailable,
                -> Unit
            }
        }
    }

    private sealed interface PlayOutcome {
        /** [loopCount] is how often the image asks to be played: 0 for ever, null once. */
        data class Played(val loopCount: Int?) : PlayOutcome

        data object Failed : PlayOutcome
    }
}

// Browsers show frames that ask for 10 ms or less for 100 ms, as old GIF encoders wrote 0 meaning "as fast as allowed"
private fun frameDuration(update: ProgressiveDecodeUpdate.AnimationFrameAvailable): Long =
    update.frame.durationMillis.takeIf { it > MINIMUM_HONOURED_FRAME_MILLIS } ?: FAST_FRAME_MILLIS

private const val MINIMUM_HONOURED_FRAME_MILLIS = 10L
private const val FAST_FRAME_MILLIS = 100L

private const val VALID_FRAMES = 3

/** Small enough that one chunk decodes to only a few frames, which the decoder queues until they are polled. */
private const val CHUNK_BYTES = 64 * 1024
