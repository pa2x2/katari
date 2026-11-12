package app.katari.desktop.display.x11

import app.katari.desktop.display.scroll.ScrollMotion
import app.katari.desktop.display.scroll.ScrollPoint
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ScrollStepsTest {
    private val steps = ScrollSteps()
    private var now = 0L

    private fun xwayland(vertical: Double): ScrollMotion? {
        now += 8
        return steps.read(ScrollDeviceKind.Xwayland, 0.0, vertical, ScrollPoint(now, 0.0, 0.0, 0))
    }

    @Test
    fun `on Xwayland a finger step that could be a wheel's stays the fingers' until they lift`() {
        xwayland(0.37) shouldBe ScrollMotion.Fingers(ScrollPoint(8, 0.0, 0.0, 0), 0.0, 3.7, starts = true)

        // 2.5 pixels of travel: a quarter of a step, as a smooth wheel also turns
        xwayland(0.25) shouldBe ScrollMotion.Fingers(ScrollPoint(16, 0.0, 0.0, 0), 0.0, 2.5, starts = false)

        xwayland(0.0).shouldBeInstanceOf<ScrollMotion.FingersLifted>()
        xwayland(0.25) shouldBe ScrollMotion.Wheel(ScrollPoint(32, 0.0, 0.0, 0), 0.0, 0.25)
    }

    @Test
    fun `fingers that rested before lifting still lift`() {
        xwayland(0.37).shouldBeInstanceOf<ScrollMotion.Fingers>()
        now += 5_000

        xwayland(0.0).shouldBeInstanceOf<ScrollMotion.FingersLifted>()
        xwayland(0.0) shouldBe null
    }
}
