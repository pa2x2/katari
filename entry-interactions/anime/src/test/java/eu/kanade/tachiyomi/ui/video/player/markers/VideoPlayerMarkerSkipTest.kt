package eu.kanade.tachiyomi.ui.video.player.markers

import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VideoPlayerMarkerSkipTest {

    private val intro = PlaybackMarker(startMs = 60_000, endMs = 150_000, kind = PlaybackMarkerKind.INTRO)
    private val credits = PlaybackMarker(startMs = 1_290_000, endMs = 1_380_000, kind = PlaybackMarkerKind.OUTRO)
    private val preview = PlaybackMarker(startMs = 1_380_000, endMs = 1_420_000, kind = PlaybackMarkerKind.PREVIEW)

    @Test
    fun `an automatic skip happens once per marker, not again after seeking back into it`() {
        val autoSkip = MarkerAutoSkip()
        val markers = listOf(intro)

        autoSkip.claim(skipOfferAt(61_000, DURATION_MS, markers, hasNextEpisode = true)!!) shouldBe true
        autoSkip.claim(skipOfferAt(90_000, DURATION_MS, markers, hasNextEpisode = true)!!) shouldBe false

        listOf(PlaybackMarkerKind.MIXED_INTRO, PlaybackMarkerKind.MIXED_OUTRO).forEach { kind ->
            val carriesStory = listOf(intro.copy(kind = kind))
            autoSkip.claim(skipOfferAt(61_000, DURATION_MS, carriesStory, hasNextEpisode = true)!!) shouldBe false
        }
    }

    @Test
    fun `credits running to the end offer the next episode, and never skip past the end`() {
        // Credits marked to the second, ending 600 ms after the video does.
        val toTheEnd = credits.copy(endMs = DURATION_MS + 600)
        skipOfferAt(1_300_000, DURATION_MS, listOf(toTheEnd), hasNextEpisode = true)!!.goesToNextEpisode shouldBe true
        skipOfferAt(1_300_000, DURATION_MS, listOf(toTheEnd), hasNextEpisode = false)!!.targetMs shouldBe DURATION_MS

        val beforePreview = skipOfferAt(1_300_000, DURATION_MS, listOf(credits, preview), hasNextEpisode = true)!!
        beforePreview.goesToNextEpisode shouldBe true

        val midEpisode = credits.copy(startMs = 600_000, endMs = 690_000)
        skipOfferAt(610_000, DURATION_MS, listOf(midEpisode), hasNextEpisode = true)!!.goesToNextEpisode shouldBe false
    }

    private companion object {
        const val DURATION_MS = 1_420_000L
    }
}
