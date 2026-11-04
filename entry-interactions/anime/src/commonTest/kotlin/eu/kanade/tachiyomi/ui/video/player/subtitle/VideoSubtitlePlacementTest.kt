package eu.kanade.tachiyomi.ui.video.player.subtitle

import io.kotest.matchers.shouldBe
import mihon.video.engine.spi.player.SubtitleAnchored
import mihon.video.engine.spi.player.SubtitleCue
import mihon.video.engine.spi.player.SubtitleLine
import org.junit.jupiter.api.Test

class VideoSubtitlePlacementTest {

    @Test
    fun `cues on screen together stack up from the resting line instead of covering each other`() {
        // Media3's subtitle view drew every unplaced cue at the same spot, so two SubRip lines overlapped; a cue its
        // file places mustn't push the stack either.
        val placed = placeSubtitles(
            cues = listOf(
                SubtitleCue("aaaa"),
                SubtitleCue(
                    "cc",
                    line = SubtitleLine.Share(SubtitleAnchored(0.1f, SubtitleAnchored.Anchor.Start)),
                    position = SubtitleAnchored(0.25f, SubtitleAnchored.Anchor.Start),
                ),
                SubtitleCue("bb"),
                SubtitleCue("dd", line = SubtitleLine.Share(SubtitleAnchored(1f, SubtitleAnchored.Anchor.Start))),
            ),
            layout = LAYOUT,
            boundsWidth = 1000f,
            measure = ::measure,
        )

        placed.map { it.left to it.top } shouldBe listOf(
            // Centred in the picture (100 to 900), its bottom on the resting line at 552, moved down by 10
            475f to 512f,
            // A quarter across the picture and a tenth down it
            300f to 70f,
            // Above the first cue
            485f to 462f,
            // Placed at the picture's bottom edge, held inside it
            485f to 560f,
        )
    }

    private fun measure(cue: SubtitleCue, width: Int, fixedWidth: Boolean): FakeText {
        val textWidth = minOf(cue.text.length * 10, width)
        return FakeText(width = if (fixedWidth) width else textWidth, linesWidth = textWidth)
    }

    private class FakeText(override val width: Int, override val linesWidth: Int) : MeasuredSubtitleText {
        override val height: Int = 50
        override val firstLineHeight: Float = 50f
    }

    private companion object {
        // A 4:3 picture fitted in a 1000 by 600 player, text 40 pixels tall (padded by 5 a side), dragged 10 down
        val LAYOUT = VideoSubtitleLayout(
            playerWidth = 1000f,
            playerHeight = 600f,
            textSizePx = 40f,
            bottomPx = 48f,
            areaLeft = 100f,
            areaTop = 0f,
            areaWidth = 800f,
            areaHeight = 600f,
            shiftX = 0f,
            shiftY = 10f,
        )
    }
}
