package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PreferredSourceQualityTest {

    @Test
    fun `a missing preferred height falls back below it before above it`() {
        // Labels as Rezka and similar sources send them, keyed by opaque ids.
        val options = listOf(
            VideoPlaybackOption(key = "q1", label = "360p"),
            VideoPlaybackOption(key = "q2", label = "480p"),
            VideoPlaybackOption(key = "q3", label = "1080p Ultra"),
            VideoPlaybackOption(key = "q4", label = "4K"),
        )

        pickPreferredSourceQuality(options, PreferredHeight.Near(720)) shouldBe "q2"
        pickPreferredSourceQuality(options.drop(2), PreferredHeight.Near(720)) shouldBe "q3"
        pickPreferredSourceQuality(options, PreferredHeight.Highest) shouldBe "q4"
    }
}
