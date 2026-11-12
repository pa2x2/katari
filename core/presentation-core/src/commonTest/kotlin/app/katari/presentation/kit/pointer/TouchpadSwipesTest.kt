package app.katari.presentation.kit.pointer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TouchpadSwipesTest {
    private val seen = mutableListOf<String>()
    private val swipes = TouchpadSwipes(
        slop = 10f,
        onStart = { axis ->
            seen += "start $axis"
            axis == Orientation.Vertical
        },
        onMove = { axis, travel -> seen += "move $axis $travel" },
        onEnd = { seen += "end" },
    )

    private fun step(
        swipe: Int,
        stage: TouchpadScrollStage,
        x: Float = 0f,
        y: Float = 0f,
        contentFollowsFingers: Boolean = false,
    ) = swipes.onStep(TouchpadScroll(swipe, stage, Offset(x, y), contentFollowsFingers))

    @Test
    fun `a swipe keeps to the axis it first goes along and ends once as the fingers lift, its glide set aside`() {
        step(1, TouchpadScrollStage.Fingers, x = 4f, y = -6f) shouldBe true
        step(1, TouchpadScrollStage.Fingers, x = 3f, y = -8f) shouldBe true
        step(1, TouchpadScrollStage.Fingers, x = 20f, y = -2f) shouldBe true
        step(1, TouchpadScrollStage.Lifted) shouldBe true
        step(1, TouchpadScrollStage.Gliding, y = -30f) shouldBe true

        seen shouldBe listOf("start Vertical", "move Vertical -14.0", "move Vertical -16.0", "end")
    }

    @Test
    fun `a swipe along an axis the control refuses, or seen only from its glide, is left to what lies under it`() {
        step(1, TouchpadScrollStage.Fingers, x = 12f) shouldBe false
        step(1, TouchpadScrollStage.Lifted) shouldBe false
        step(2, TouchpadScrollStage.Gliding, y = -30f) shouldBe false

        seen shouldBe listOf("start Horizontal")
    }

    @Test
    fun `the next swipe ends one whose fingers never lifted here`() {
        step(1, TouchpadScrollStage.Fingers, y = 12f)
        step(2, TouchpadScrollStage.Fingers, y = -12f)

        seen shouldBe listOf("start Vertical", "move Vertical 12.0", "end", "start Vertical", "move Vertical -12.0")
    }

    @Test
    fun `with natural scrolling the control follows the fingers, not the content they move the other way`() {
        step(1, TouchpadScrollStage.Fingers, y = 14f, contentFollowsFingers = true)
        step(1, TouchpadScrollStage.Fingers, y = 6f, contentFollowsFingers = true)

        seen shouldBe listOf("start Vertical", "move Vertical -14.0", "move Vertical -20.0")
    }
}
