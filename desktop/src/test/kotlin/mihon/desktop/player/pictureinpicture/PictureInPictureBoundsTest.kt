package mihon.desktop.player.pictureinpicture

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.Rectangle

class PictureInPictureBoundsTest {

    @Test
    fun `a window saved on a larger or unplugged screen comes back whole on a screen there is`() {
        val laptop = Rectangle(0, 0, 1536, 864)
        val shape = 16f / 9f
        // Saved in the bottom-right corner of a 4K monitor to the right of the laptop, which is unplugged now
        val onUnpluggedMonitor = Rectangle(4800, 1700, 960, 540)
        // Saved larger than the laptop's screen and hanging off its bottom-right corner
        val tooLarge = Rectangle(1000, 600, 1800, 1013)

        restoredPictureInPictureBounds(onUnpluggedMonitor, shape, listOf(laptop)) shouldBe
            Rectangle(576, 324, 960, 540)
        restoredPictureInPictureBounds(tooLarge, shape, listOf(laptop)) shouldBe Rectangle(0, 0, 1536, 864)
    }

    @Test
    fun `dragging a corner keeps the video's shape and the opposite corner in place`() {
        val start = Rectangle(1000, 600, 480, 270)

        // The top-left corner dragged up and left, further sideways than up
        resizedPictureInPictureBounds(
            start,
            ResizeEdge.TopLeft,
            dx = -160,
            dy = -20,
            shape = 16f / 9f,
            maxWidth = 1920,
        ) shouldBe
            Rectangle(840, 510, 640, 360)
        // The bottom edge dragged down widens it from its left edge
        resizedPictureInPictureBounds(
            start,
            ResizeEdge.Bottom,
            dx = 0,
            dy = 90,
            shape = 16f / 9f,
            maxWidth = 1920,
        ) shouldBe
            Rectangle(1000, 600, 640, 360)
    }
}
