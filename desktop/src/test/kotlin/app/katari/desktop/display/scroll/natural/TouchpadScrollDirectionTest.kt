package app.katari.desktop.display.scroll.natural

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TouchpadScrollDirectionTest {
    private fun direction(systemNatural: Boolean?, choice: NaturalScrollingChoice) =
        TouchpadScrollDirection.of(systemNatural, choice).let { it.turnsContent to it.contentFollowsFingers }

    @Test
    fun `where the system can be read, content scrolls as chosen, turned where the system scrolls the other way`() {
        direction(true, NaturalScrollingChoice.AsSystem) shouldBe (false to true)
        direction(false, NaturalScrollingChoice.AsSystem) shouldBe (false to false)
        direction(true, NaturalScrollingChoice.On) shouldBe (false to true)
        direction(false, NaturalScrollingChoice.On) shouldBe (true to true)
        direction(true, NaturalScrollingChoice.Off) shouldBe (true to false)
        direction(false, NaturalScrollingChoice.Off) shouldBe (false to false)
    }

    @Test
    fun `where it can't, content scrolls as the system has it, and the choice says which way that is`() {
        direction(null, NaturalScrollingChoice.AsSystem) shouldBe (false to false)
        direction(null, NaturalScrollingChoice.On) shouldBe (false to true)
        direction(null, NaturalScrollingChoice.Off) shouldBe (false to false)
    }
}
