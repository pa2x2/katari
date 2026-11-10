package app.katari.desktop.display.scroll

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class ScrollGlideTest {
    // Fingers moving the content down at 1500 pixels a second, reported every 8 ms
    private fun flick(velocity: FingerVelocity): Long {
        velocity.start(0)
        for (time in 8L..160L step 8) velocity.add(time, 0.0, 12.0)
        return 160
    }

    @Test
    fun `a flick glides on by its speed and comes to rest, and fingers that rested before lifting leave no glide`() {
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
    }
}
