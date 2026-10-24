package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VideoStreamChoiceTest {

    @Test
    fun `the saved stream beats the source's pick, which beats the preferred height and the transport score`() {
        val hls = stream(key = "auto", type = VideoStreamType.HLS)
        val fullHd = stream(key = "", type = VideoStreamType.PROGRESSIVE, height = 1080)
        val sd = stream(key = "sd", type = VideoStreamType.PROGRESSIVE, label = "480p")
        val streams = listOf(hls, fullHd, sd)

        chooseStream(streams, savedKey = "sd", sourceKey = "auto", PreferredHeight.Highest) shouldBe sd
        chooseStream(streams, savedKey = "gone", sourceKey = "sd", PreferredHeight.Highest) shouldBe sd
        chooseStream(streams, savedKey = null, sourceKey = null, PreferredHeight.Near(720)) shouldBe sd
        chooseStream(streams, savedKey = null, sourceKey = null, PreferredHeight.Highest) shouldBe fullHd
        chooseStream(streams, savedKey = null, sourceKey = null, preferredHeight = null) shouldBe hls
    }

    private fun stream(key: String, type: VideoStreamType, label: String = "", height: Int? = null) = VideoStream(
        request = VideoRequest("https://cdn.example.com/$key$height.m3u8"),
        label = label,
        type = type,
        key = key,
        height = height,
    )
}
