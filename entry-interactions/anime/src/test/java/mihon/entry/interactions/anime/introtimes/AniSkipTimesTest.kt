package mihon.entry.interactions.anime.introtimes

import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AniSkipTimesTest {

    @Test
    fun `an answer with credits over story keeps every time, rather than failing the episode as Aniyomi does`() {
        // AniSkip's answer for Jujutsu Kaisen (MAL 40748) episode 1, 2026-10-26.
        val body = """
            {"found":true,"results":[
            {"interval":{"startTime":54.711,"endTime":145.111},"skipType":"op","skipId":"a","episodeLength":1435.122},
            {"interval":{"startTime":1338.255,"endTime":1428.255},"skipType":"ed","skipId":"b","episodeLength":1435.028},
            {"interval":{"startTime":223.965,"endTime":313.965},"skipType":"mixed-ed","skipId":"c","episodeLength":1492.157}
            ],"message":"Successfully found skip times","statusCode":200}
        """.trimIndent()

        aniSkipMarkers(body) shouldBe listOf(
            PlaybackMarker(startMs = 54_711, endMs = 145_111, kind = PlaybackMarkerKind.INTRO),
            PlaybackMarker(startMs = 1_338_255, endMs = 1_428_255, kind = PlaybackMarkerKind.OUTRO),
            PlaybackMarker(startMs = 223_965, endMs = 313_965, kind = PlaybackMarkerKind.MIXED_OUTRO),
        )
    }
}
