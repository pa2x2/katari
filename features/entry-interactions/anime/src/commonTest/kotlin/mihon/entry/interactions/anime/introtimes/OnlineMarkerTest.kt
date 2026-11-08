package mihon.entry.interactions.anime.introtimes

import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OnlineMarkerTest {

    @Test
    fun `TheIntroDB adds only what AniSkip doesn't mark, and nothing over one of AniSkip's spans`() {
        val aniSkipIntro = marker(60, 150, PlaybackMarkerKind.MIXED_INTRO)
        val theIntroDbRecap = marker(0, 55, PlaybackMarkerKind.RECAP)
        val theIntroDbPreview = marker(1_400, 1_430, PlaybackMarkerKind.PREVIEW)

        mergeIntroTimes(
            mapOf(
                IntroTimesService.THEINTRODB to listOf(
                    // AniSkip already marks the intro, though as one over story.
                    marker(62, 148, PlaybackMarkerKind.INTRO),
                    theIntroDbRecap,
                    // A kind AniSkip lacks, but over AniSkip's intro.
                    marker(140, 170, PlaybackMarkerKind.RECAP),
                    theIntroDbPreview,
                ),
                IntroTimesService.ANISKIP to listOf(aniSkipIntro),
            ),
        ) shouldBe listOf(
            OnlineMarker(aniSkipIntro, IntroTimesService.ANISKIP),
            OnlineMarker(theIntroDbRecap, IntroTimesService.THEINTRODB),
            OnlineMarker(theIntroDbPreview, IntroTimesService.THEINTRODB),
        )
    }

    private fun marker(startSeconds: Long, endSeconds: Long, kind: PlaybackMarkerKind) =
        PlaybackMarker(startMs = startSeconds * 1_000, endMs = endSeconds * 1_000, kind = kind)
}
