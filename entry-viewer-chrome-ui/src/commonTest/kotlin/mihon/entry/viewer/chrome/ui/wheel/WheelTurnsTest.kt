package mihon.entry.viewer.chrome.ui.wheel

import androidx.compose.ui.geometry.Offset
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WheelTurnsTest {
    @Test
    fun `a touchpad swipe reported as whole notches turns one page, and a notch after a pause turns the next`() =
        runTest {
            val turns = WheelTurns(backgroundScope)
            var pages = 0
            val turn = { notches: Offset ->
                val whole = notches.y.toInt()
                pages += whole
                Offset(0f, whole.toFloat())
            }

            // How X11 reported one swipe: nine notches 33 ms apart
            repeat(9) {
                turns.onScroll(Offset(0f, 1f), turn)
                advanceTimeBy(33)
            }
            pages shouldBe 1

            advanceTimeBy(1_000)
            turns.onScroll(Offset(0f, 1f), turn)
            pages shouldBe 2
        }
}
