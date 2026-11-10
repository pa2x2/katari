package eu.kanade.tachiyomi.ui.video.player.subtitle

import androidx.compose.ui.geometry.Rect
import app.katari.video.engine.spi.player.SubtitleCue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.SubtitlePosition

class SubtitleEditorMoveTest {

    @Test
    fun `a stack snapped to the top guide rests in the top row, and one dragged freely is drawn where it was left`() {
        // A two-line stack 200 wide resting at the bottom centre of a 1000 by 600 player
        val resting = Rect(left = 400f, top = 452f, right = 600f, bottom = 552f)

        val snapped = LAYOUT.moveSubtitles(
            appearance = VideoSubtitleAppearance(),
            from = resting,
            to = Rect(left = 520f, top = 52f, right = 720f, bottom = 152f),
            stacked = true,
            snapPx = 8f,
        )
        // The offset across stays, as the stack isn't near the centre line; the row's offset would put it elsewhere
        // on a player of another height
        snapped.appearance.position shouldBe SubtitlePosition.TOP_CENTER
        snapped.appearance.offsetY shouldBe 0f
        snapped.appearance.offsetX shouldBe 0.15f
        snapped.reached shouldBe setOf(SubtitleGuide.TOP)

        val dropped = Rect(left = 200f, top = 152f, right = 400f, bottom = 252f)
        val free = LAYOUT.moveSubtitles(VideoSubtitleAppearance(), resting, dropped, stacked = true, snapPx = 8f)
        val placed = placeSubtitles(
            cues = TWO_LINES,
            layout = free.appearance.layoutIn(LAYOUT),
            boundsWidth = 1000f,
            measure = { _, width, fixedWidth -> FakeText(width = if (fixedWidth) width else 190, linesWidth = 190) },
        )
        placed.resting shouldBe dropped
        free.reached shouldBe emptySet()
    }

    private fun VideoSubtitleAppearance.layoutIn(layout: VideoSubtitleLayout) = layout.copy(
        position = position,
        shiftX = layout.areaWidth * offsetX,
        shiftY = layout.areaHeight * offsetY,
    )

    private class FakeText(override val width: Int, override val linesWidth: Int) : MeasuredSubtitleText {
        override val height: Int = 50
        override val firstLineHeight: Float = 50f
    }

    private companion object {
        // Text 40 tall padded by 5 a side, so each 190-wide line is drawn 200 wide
        val TWO_LINES = listOf(
            SubtitleCue("first"),
            SubtitleCue("second"),
        )

        // A 4:3 picture fitted in a 1000 by 600 player, resting 48 from its edges
        val LAYOUT = VideoSubtitleLayout(
            playerWidth = 1000f,
            playerHeight = 600f,
            textSizePx = 40f,
            position = SubtitlePosition.BOTTOM_CENTER,
            restTop = 48f,
            restMiddle = 300f,
            restBottom = 552f,
            restLeft = 148f,
            restRight = 852f,
            areaLeft = 100f,
            areaTop = 0f,
            areaWidth = 800f,
            areaHeight = 600f,
            shiftX = 0f,
            shiftY = 0f,
            marginPx = 8f,
        )
    }
}
