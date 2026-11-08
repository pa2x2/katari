package app.katari.entry.interactions.anime.introtimes.theintrodb

import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TheIntroDbTimesTest {

    @Test
    fun `credits without an end run to the episode's end, and a span of no length marks nothing`() {
        // TheIntroDB's answer for Breaking Bad (TVDB 81189) S1E1, 2026-11-08. The recap of no length is how its API
        // documents "this episode has none".
        val body = """
            {"tvdb_id":81189,"type":"tv","season":1,"episode":1,
            "intro":[{"start_ms":228671,"end_ms":246465}],
            "recap":[{"start_ms":0,"end_ms":0}],
            "credits":[{"start_ms":3432825,"end_ms":null}]}
        """.trimIndent()

        theIntroDbMarkers(body, lengthMs = 3_480_000) shouldBe listOf(
            PlaybackMarker(startMs = 228_671, endMs = 246_465, kind = PlaybackMarkerKind.INTRO),
            PlaybackMarker(startMs = 3_432_825, endMs = 3_480_000, kind = PlaybackMarkerKind.OUTRO),
        )
    }
}
