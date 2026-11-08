package app.katari.desktop.window

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.Rectangle

class WindowReachTest {

    @Test
    fun `a window saved on a screen that is gone is not put back there`() {
        val laptop = Rectangle(0, 0, 1536, 864)
        val unpluggedMonitor = Rectangle(1536, 0, 1920, 1080)
        val onMonitor = Rectangle(1800, 100, 1200, 800)
        // Mostly off the laptop screen's left edge, with the end of its title bar still on it
        val hangingOffLeft = Rectangle(-1100, 200, 1200, 800)

        isReachable(onMonitor, listOf(laptop, unpluggedMonitor)) shouldBe true
        isReachable(onMonitor, listOf(laptop)) shouldBe false
        isReachable(hangingOffLeft, listOf(laptop)) shouldBe true
    }
}
