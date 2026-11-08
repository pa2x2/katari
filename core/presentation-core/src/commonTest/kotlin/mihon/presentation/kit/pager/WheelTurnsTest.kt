package mihon.presentation.kit.pager

import androidx.compose.ui.geometry.Offset
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class WheelTurnsTest {
    private val turns = WheelTurns()
    private var pages = 0
    private var now = 0L

    private fun scroll(gapMillis: Long) {
        now += gapMillis
        turns.onScroll(Offset(0f, 1f), now) { notches ->
            val whole = notches.y.toInt()
            pages += whole
            Offset(0f, whole.toFloat())
        }
    }

    // How X11 reports a swipe: whole notches a few tens of milliseconds apart, slowing as the fingers stop
    private fun swipe(liftMillis: Long) {
        scroll(liftMillis)
        listOf(20L, 15L, 15L, 20L, 30L, 45L, 70L, 110L).forEach(::scroll)
    }

    @Test
    fun `each swipe turns one page however soon the next follows or slowly it ends, as does a notch after a pause`() {
        swipe(liftMillis = 1_000)
        pages shouldBe 1

        swipe(liftMillis = 120)
        swipe(liftMillis = 150)
        pages shouldBe 3

        // The last swipe slowing down further is still that swipe
        scroll(160)
        pages shouldBe 3

        scroll(400)
        pages shouldBe 4
    }
}
