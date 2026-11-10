package app.katari.desktop.display.scroll

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class ScrollGlideTest {
    // Fingers moving the content down at 1500 pixels a second, reported every 8 ms
    private fun flick(velocity: FingerVelocity): Long {
        velocity.start()
        for (time in 8L..160L step 8) velocity.add(time, 0.0, 12.0)
        return 160
    }

    @Test
    fun `a flick glides on by its speed and comes to rest, and fingers that rested or only brushed leave no glide`() {
        val velocity = FingerVelocity()
        val last = flick(velocity)

        val (x, y) = velocity.atLift(last + 8)
        y shouldBe (1_500.0 plusOrMinus 1.0)
        val glide = ScrollGlide.from(x, y)
        glide shouldNotBe null
        // 0.3 s of the speed it loses, and no further however long after
        glide!!.travelled(5_000.0).second shouldBe (441.0 plusOrMinus 1.0)
        glide.isOver(5_000.0) shouldBe true
        glide.isOver(100.0) shouldBe false

        val (restedX, restedY) = velocity.atLift(last + 150)
        ScrollGlide.from(restedX, restedY) shouldBe null

        // Two steps of leaving fingers, 16 pixels in 8 ms: fast, but for no time
        velocity.start()
        velocity.add(1_004, 0.0, 8.0)
        velocity.add(1_008, 0.0, 8.0)
        val (brushedX, brushedY) = velocity.atLift(1_012)
        ScrollGlide.from(brushedX, brushedY) shouldBe null
    }
}
