package eu.kanade.tachiyomi.ui.video.player.markers

import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapterName.Given
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapterName.Marker
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapterName.Numbered
import io.kotest.matchers.shouldBe
import mihon.video.engine.spi.chapters.FileChapter
import org.junit.jupiter.api.Test

class VideoPlayerChaptersTest {

    private val intro = PlaybackMarker(startMs = 90_000, endMs = 180_000, kind = PlaybackMarkerKind.INTRO)
    private val credits = PlaybackMarker(startMs = 1_270_000, endMs = 1_360_000, kind = PlaybackMarkerKind.OUTRO)

    @Test
    fun `the source's intro and credits fold into the file's chapters a second or two off them, not beside them`() {
        val fileChapters = listOf(
            FileChapter(0, "Prologue"),
            FileChapter(91_000, "Opening"),
            FileChapter(181_500, "Part A"),
            FileChapter(700_000, "Part B"),
            FileChapter(1_272_000, "Ending"),
            FileChapter(1_360_000, null),
        )

        videoPlayerChapters(listOf(intro, credits), fileChapters, DURATION_MS)
            .map { it.startMs to it.name } shouldBe listOf(
            0L to Given("Prologue"),
            90_000L to Given("Opening"),
            180_000L to Given("Part A"),
            700_000L to Given("Part B"),
            1_270_000L to Given("Ending"),
            1_360_000L to Numbered(6),
        )
    }

    @Test
    fun `the stretch after a source's intro is a part of its own, not more of the intro`() {
        val partB = PlaybackMarker(700_000, 1_270_000, PlaybackMarkerKind.CHAPTER, label = "Part B")
        // Credits marked to the second, ending a moment before the video does.
        val chapters =
            videoPlayerChapters(listOf(intro, partB, credits.copy(endMs = 1_418_500)), emptyList(), DURATION_MS)

        chapters.map { it.startMs to it.name } shouldBe listOf(
            0L to Numbered(1),
            90_000L to Marker(PlaybackMarkerKind.INTRO),
            180_000L to Numbered(3),
            700_000L to Given("Part B"),
            1_270_000L to Marker(PlaybackMarkerKind.OUTRO),
        )
        chapters.chapterAt(300_000)?.name shouldBe Numbered(3)
    }

    private companion object {
        const val DURATION_MS = 1_420_000L
    }
}
