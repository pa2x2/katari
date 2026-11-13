package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import android.content.Context
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ForwardingRenderer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererConfiguration
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SampleStream
import androidx.media3.exoplayer.text.TextOutput

/**
 * How late the watched player's subtitles show. The UI thread sets it and the playback thread reads it on every
 * render, so a change shows by the next frame, or within a second while paused, when ExoPlayer renders less often.
 */
internal class SubtitleDelay {
    @Volatile
    var delayUs: Long = 0L
}

/**
 * The default renderers, with every text renderer shown [delay] late. Media3 has no subtitle offset, so the text
 * renderer is handed the playback position moved back by the delay: it then reads, picks and drops cues as if playback
 * were there. Every subtitle it shows moves, whichever way it was loaded.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class DelayedSubtitleRenderersFactory(
    context: Context,
    private val delay: SubtitleDelay,
) : DefaultRenderersFactory(context) {
    override fun buildTextRenderers(
        context: Context,
        output: TextOutput,
        outputLooper: Looper,
        extensionRendererMode: Int,
        out: ArrayList<Renderer>,
    ) {
        val built = ArrayList<Renderer>()
        super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, built)
        built.mapTo(out) { DelayedSubtitleRenderer(it, delay) }
    }
}

/**
 * A text renderer that sees every position [delay] earlier. The lines of a stream parsed to cues come through a
 * [RecordedSubtitleStream], so when the delay changes, or playback seeks, the renderer starts over from the lines due at
 * its new position, even ones it had dropped: the lines move at once, playing or paused. Lines never loaded stay
 * missing: after a seek past what an HLS stream has loaded, with subtitles set later, the lines from before the seek
 * target that would now show come only with the segments loaded from the target on. Other subtitle formats, such as
 * captions carried in the video, keep showing the line they were showing until their next line is due.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class DelayedSubtitleRenderer(
    renderer: Renderer,
    private val delay: SubtitleDelay,
) : ForwardingRenderer(renderer) {
    private var recorded: RecordedSubtitleStream? = null
    private var streamOffsetUs = 0L
    private var appliedDelayUs = 0L

    override fun enable(
        configuration: RendererConfiguration,
        formats: Array<out Format>,
        stream: SampleStream,
        positionUs: Long,
        joining: Boolean,
        mayRenderStartOfStream: Boolean,
        startPositionUs: Long,
        offsetUs: Long,
        mediaPeriodId: MediaSource.MediaPeriodId,
    ) {
        appliedDelayUs = delay.delayUs
        streamOffsetUs = offsetUs
        super.enable(
            configuration,
            formats,
            recording(formats, stream, fromUs = positionUs - appliedDelayUs - offsetUs),
            positionUs - appliedDelayUs,
            joining,
            mayRenderStartOfStream,
            startPositionUs,
            offsetUs,
            mediaPeriodId,
        )
    }

    override fun replaceStream(
        formats: Array<out Format>,
        stream: SampleStream,
        startPositionUs: Long,
        offsetUs: Long,
        mediaPeriodId: MediaSource.MediaPeriodId,
    ) {
        streamOffsetUs = offsetUs
        super.replaceStream(
            formats,
            recording(formats, stream, fromUs = Long.MIN_VALUE),
            startPositionUs,
            offsetUs,
            mediaPeriodId,
        )
    }

    // The player knows the stream it gave, not the recording around it
    override fun getStream(): SampleStream? = recorded?.upstream ?: super.getStream()

    override fun resetPosition(positionUs: Long, sampleStreamIsResetToKeyFrame: Boolean) {
        appliedDelayUs = delay.delayUs
        val recorded = recorded
        if (recorded == null) {
            super.resetPosition(positionUs - appliedDelayUs, sampleStreamIsResetToKeyFrame)
        } else {
            startOver(recorded, positionUs)
        }
    }

    override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
        val recorded = recorded
        if (delay.delayUs != appliedDelayUs) {
            appliedDelayUs = delay.delayUs
            if (recorded != null) {
                // Not a seek, so the player won't mark the stream final again, as it had
                val final = isCurrentStreamFinal
                startOver(recorded, positionUs)
                if (final) setCurrentStreamFinal()
            }
        }
        val shiftedUs = positionUs - appliedDelayUs
        super.render(shiftedUs, elapsedRealtimeUs)
        // The renderer takes in one line a call, a second apart while paused: it takes in every line due now
        var calls = 1
        while (
            recorded != null &&
            calls < MAX_RENDER_CALLS &&
            recorded.hasLineBy(shiftedUs - streamOffsetUs + TEXT_READ_AHEAD_US)
        ) {
            super.render(shiftedUs, elapsedRealtimeUs)
            calls++
        }
    }

    override fun getDurationToProgressUs(positionUs: Long, elapsedRealtimeUs: Long): Long =
        super.getDurationToProgressUs(positionUs - appliedDelayUs, elapsedRealtimeUs)

    override fun disable() {
        recorded = null
        super.disable()
    }

    private fun recording(formats: Array<out Format>, stream: SampleStream, fromUs: Long): SampleStream {
        val parsedToCues = formats.firstOrNull()?.sampleMimeType == MimeTypes.APPLICATION_MEDIA3_CUES
        val recording = if (parsedToCues) RecordedSubtitleStream(stream).also { it.rewind(fromUs) } else null
        recorded = recording
        return recording ?: stream
    }

    // The recording hands out its lines anew, and the stream is already where it is to be read from
    private fun startOver(recorded: RecordedSubtitleStream, positionUs: Long) {
        recorded.rewind(positionUs - appliedDelayUs - streamOffsetUs)
        super.resetPosition(positionUs - appliedDelayUs, true)
    }

    private companion object {
        // How far ahead of its position the text renderer reads lines
        const val TEXT_READ_AHEAD_US = 1_000_000L
        const val MAX_RENDER_CALLS = 64
    }
}
