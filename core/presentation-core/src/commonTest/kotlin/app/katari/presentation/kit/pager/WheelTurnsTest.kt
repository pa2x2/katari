package app.katari.presentation.kit.pager

import androidx.compose.ui.geometry.Offset
import app.katari.presentation.kit.pointer.TouchpadScroll
import app.katari.presentation.kit.pointer.TouchpadScrollStage
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class WheelTurnsTest {
    private val turns = WheelTurns()
    private var pages = 0
    private var now = 0L

    private fun scroll(gapMillis: Long, notches: Float = 1f, touchpad: TouchpadScroll? = null) {
        now += gapMillis
        turns.onScroll(Offset(0f, notches), now, touchpad) { counted ->
            val whole = counted.y.toInt()
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

    @Test
    fun `a touchpad's swipe turns one page whatever follows it, and the next swipe the next, even into its glide`() {
        fun fingers(swipe: Int, steps: Int, notches: Float = 0.6f) =
            repeat(steps) {
                scroll(
                    8,
                    notches,
                    TouchpadScroll(swipe, TouchpadScrollStage.Fingers, Offset.Zero, contentFollowsFingers = false),
                )
            }

        fun glide(swipe: Int) = repeat(25) {
            scroll(
                8,
                notches = 0.3f,
                TouchpadScroll(swipe, TouchpadScrollStage.Gliding, Offset.Zero, contentFollowsFingers = false),
            )
        }

        fingers(swipe = 1, steps = 5)
        glide(swipe = 1)
        pages shouldBe 1

        // No gap tells this swipe from the glide it follows
        fingers(swipe = 2, steps = 5)
        pages shouldBe 2

        // The display ends the scroll and starts another under fingers that never lifted
        fingers(swipe = 3, steps = 5)
        pages shouldBe 2

        // The leaving fingers brush the touchpad: a short scroll of its own, fast enough to leave a glide
        now += 300
        fingers(swipe = 4, steps = 2, notches = 0.1f)
        glide(swipe = 4)
        pages shouldBe 2
    }
}
