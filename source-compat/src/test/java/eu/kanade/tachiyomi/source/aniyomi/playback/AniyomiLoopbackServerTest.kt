package eu.kanade.tachiyomi.source.aniyomi.playback

import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AniyomiLoopbackServerTest {

    @Test
    fun `the placeholder moves to the server in the video, audio and subtitle links, and other local ports stay`() {
        val stream = VideoStream(
            request = VideoRequest("http://localhost:1/episode.m3u8"),
            subtitles = listOf(VideoSubtitle(VideoRequest("http://localhost:1/en.vtt"), label = "English")),
            audioTracks = listOf(
                VideoAudioTrack(VideoRequest("http://localhost:1?track=ja"), label = "Japanese"),
                VideoAudioTrack(VideoRequest("http://localhost:12345/en.m4a"), label = "English"),
            ),
        )

        val served = stream.withLoopbackServer("http://localhost:40123")

        stream.usesLoopbackServer shouldBe true
        served.request?.url shouldBe "http://localhost:40123/episode.m3u8"
        served.subtitles.map { it.request.url } shouldBe listOf("http://localhost:40123/en.vtt")
        served.audioTracks.map { it.request.url } shouldBe
            listOf("http://localhost:40123?track=ja", "http://localhost:12345/en.m4a")
    }
}
