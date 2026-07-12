package eu.kanade.tachiyomi.ui.video.player

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VideoPlayerGestureTest {

    @Test
    fun `vertical gesture sensitivity is consistent across orientation`() {
        verticalGestureFraction(
            startY = 300f,
            currentY = 120f,
            playerWidth = 640,
            playerHeight = 360,
        ) shouldBe 0.5f
        verticalGestureFraction(
            startY = 500f,
            currentY = 320f,
            playerWidth = 360,
            playerHeight = 640,
        ) shouldBe 0.5f
    }

    @Test
    fun `vertical gesture fraction is clamped`() {
        verticalGestureFraction(
            startY = 640f,
            currentY = 0f,
            playerWidth = 360,
            playerHeight = 640,
        ) shouldBe 1f
        verticalGestureFraction(
            startY = 0f,
            currentY = 640f,
            playerWidth = 360,
            playerHeight = 640,
        ) shouldBe -1f
    }

    @Test
    fun `vertical gesture fraction ignores invalid player dimensions`() {
        verticalGestureFraction(
            startY = 100f,
            currentY = 0f,
            playerWidth = 0,
            playerHeight = 640,
        ) shouldBe 0f
    }
}
