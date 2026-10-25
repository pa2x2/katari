package mihon.video.engine.mpv.tracks

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MpvTrackChoicesTest {

    // Two HLS variants with their audio muxed in, as mpv lists them with flattened editions: each variant's audio is
    // its own track, in its own program. The 1080p variant plays.
    private val tracks = MpvTrack.parseList(
        """
        [
          {"id":1,"type":"audio","lang":"en","selected":false,"hls-bitrate":246440,"program-ids":[1]},
          {"id":1,"type":"video","selected":false,"demux-h":184,"hls-bitrate":246440,"program-ids":[1]},
          {"id":2,"type":"audio","lang":"en","selected":true,"hls-bitrate":6221600,"program-ids":[4]},
          {"id":2,"type":"video","selected":true,"demux-h":1080,"hls-bitrate":6221600,"program-ids":[4]}
        ]
        """.trimIndent(),
    )

    @Test
    fun `picking another variant's video moves the audio into that variant, so only one variant is downloaded`() {
        val choices = mpvTrackChoices(MpvTrackRequest(picked = mapOf(MpvTrackKind.Video to 1)), tracks)

        choices[MpvTrackKind.Video] shouldBe "1"
        choices[MpvTrackKind.Audio] shouldBe "1"
    }
}
