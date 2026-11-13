package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.exoplayer.FormatHolder
import androidx.media3.exoplayer.RendererConfiguration
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SampleStream
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.extractor.text.CueEncoder
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Host tests see the module's manifest, so Robolectric would follow its target SDK; its sandboxes from SDK 36 on fail to
// set up on this JDK (FileDescriptor internals).
@OptIn(markerClass = [UnstableApi::class])
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DelayedSubtitleRendererTest {

    private val lines = listOf(Line("one", 500_000, 4_000_000), Line("two", 4_500_000, 9_000_000))

    @Test
    fun `lines the renderer had dropped show when the timing moves later, paused or after a seek`() {
        val shown = mutableListOf<String>()
        val delay = SubtitleDelay()
        val stream = LoadedLines(lines)
        val renderer = DelayedSubtitleRenderer(
            TextRenderer(
                object : TextOutput {
                    override fun onCues(cueGroup: CueGroup) {
                        shown.clear()
                        cueGroup.cues.mapTo(shown) { it.text.toString() }
                    }
                },
                null,
            ),
            delay,
        )
        renderer.init(0, PlayerId.UNSET, Clock.DEFAULT)
        renderer.enable(
            RendererConfiguration.DEFAULT,
            arrayOf(Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_MEDIA3_CUES).build()),
            stream,
            0,
            false,
            true,
            0,
            0,
            MediaSource.MediaPeriodId(Any()),
        )
        for (positionUs in 0L..5_000_000L step 100_000L) renderer.render(positionUs, 0)
        shown shouldBe listOf("two")

        // Paused at 5 s: 2 s later is the first line's time
        delay.delayUs = 2_000_000
        renderer.render(5_000_000, 0)
        shown shouldBe listOf("one")
        delay.delayUs = 0
        renderer.render(5_000_000, 0)
        shown shouldBe listOf("two")

        // A seek to 5.5 s reads the stream again from there, which no longer has the first line
        delay.delayUs = 2_000_000
        stream.seekTo(5_500_000)
        renderer.resetPosition(5_500_000, true)
        renderer.render(5_500_000, 0)
        shown shouldBe listOf("one")
    }

    private class Line(val text: String, val startUs: Long, val endUs: Long)

    /** A subtitle stream whose lines have all loaded, read like a sample queue: forward, and again from a seek. */
    private class LoadedLines(private val lines: List<Line>) : SampleStream {
        private val encoder = CueEncoder()
        private var next = 0

        fun seekTo(positionUs: Long) {
            next = lines.indexOfFirst { it.startUs >= positionUs }.takeIf { it >= 0 } ?: lines.size
        }

        override fun isReady() = true

        override fun maybeThrowError() = Unit

        override fun readData(formatHolder: FormatHolder, buffer: DecoderInputBuffer, readFlags: Int): Int {
            val line = lines.getOrNull(next)
            if (line == null) {
                buffer.setFlags(C.BUFFER_FLAG_END_OF_STREAM)
                return C.RESULT_BUFFER_READ
            }
            buffer.setFlags(C.BUFFER_FLAG_KEY_FRAME)
            buffer.timeUs = line.startUs
            if (readFlags and SampleStream.FLAG_OMIT_SAMPLE_DATA == 0) {
                val data = encoder.encode(listOf(Cue.Builder().setText(line.text).build()), line.endUs - line.startUs)
                buffer.ensureSpaceForWrite(data.size)
                buffer.data!!.put(data)
            }
            if (readFlags and SampleStream.FLAG_PEEK == 0) next++
            return C.RESULT_BUFFER_READ
        }

        override fun skipData(positionUs: Long) = 0
    }
}
