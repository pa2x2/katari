package mihon.video.engine.spi.tracks

import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
import eu.kanade.tachiyomi.source.entry.VideoRequest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ExternalAudioTracksTest {

    @Test
    fun `the stream's own audio isn't named after an external track, and each merged track keeps its own name`() {
        val english = VideoAudioTrack(VideoRequest("https://cdn.example.com/en.m4a"), label = "English")
        val japanese = VideoAudioTrack(VideoRequest("https://cdn.example.com/ja.m4a"), label = "Japanese")
        val tracks = listOf(english, japanese)

        // ExoPlayer's ids with external subtitles merged into the stream as well.
        externalAudioTrackOf("0:0:1/eng", tracks) shouldBe null
        externalAudioTrackOf("1:0", tracks) shouldBe english
        externalAudioTrackOf("2:", tracks) shouldBe japanese
        externalAudioTrackOf("eng", tracks) shouldBe null
    }
}
