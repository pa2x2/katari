package mihon.video.engine.mpv.quality

import io.kotest.matchers.shouldBe
import mihon.video.engine.mpv.tracks.MpvTrack
import org.junit.jupiter.api.Test

class MpvAdaptiveQualityTest {

    // Three variants of one codec, the 2 Mbit/s one playing, and a VP9 variant Auto must not switch codecs to.
    private val tracks = MpvTrack.parseList(
        """
        [
          {"id":1,"type":"video","codec":"h264","selected":false,"hls-bitrate":500000},
          {"id":2,"type":"video","codec":"h264","selected":true,"hls-bitrate":2000000},
          {"id":3,"type":"video","codec":"h264","selected":false,"hls-bitrate":6000000},
          {"id":4,"type":"video","codec":"vp9","selected":false,"hls-bitrate":3000000}
        ]
        """.trimIndent(),
    )

    @Test
    fun `steps up only with enough buffered and down only when the buffer runs short, so it doesn't flap`() {
        val fast = 10_000_000L
        val slow = 1_000_000L

        adaptiveVideoTrack(tracks, fast, bufferedAheadMs = 5_000) shouldBe 2
        adaptiveVideoTrack(tracks, fast, bufferedAheadMs = 12_000) shouldBe 3
        adaptiveVideoTrack(tracks, slow, bufferedAheadMs = 30_000) shouldBe 2
        adaptiveVideoTrack(tracks, slow, bufferedAheadMs = 5_000) shouldBe 1
    }
}
