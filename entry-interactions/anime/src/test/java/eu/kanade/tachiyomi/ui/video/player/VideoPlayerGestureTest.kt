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

    @Test
    fun `seek position moves five seconds and stays within the video duration`() {
        resolveVideoPlayerSeekPosition(
            positionMs = 20_000L,
            durationMs = 60_000L,
            direction = VideoPlayerSeekDirection.Backward,
        ) shouldBe 15_000L
        resolveVideoPlayerSeekPosition(
            positionMs = 20_000L,
            durationMs = 60_000L,
            direction = VideoPlayerSeekDirection.Forward,
        ) shouldBe 25_000L
        resolveVideoPlayerSeekPosition(
            positionMs = 2_000L,
            durationMs = 60_000L,
            direction = VideoPlayerSeekDirection.Backward,
        ) shouldBe 0L
        resolveVideoPlayerSeekPosition(
            positionMs = 58_000L,
            durationMs = 60_000L,
            direction = VideoPlayerSeekDirection.Forward,
        ) shouldBe 60_000L
    }

    @Test
    fun `seek feedback accumulates a same-direction burst`() {
        val initial = nextVideoPlayerSeekFeedbackState(
            previousState = null,
            direction = VideoPlayerSeekDirection.Forward,
            hidePlayerChrome = true,
            sequence = 1L,
            updatedAtMillis = 1_000L,
        )

        nextVideoPlayerSeekFeedbackState(
            previousState = initial,
            direction = VideoPlayerSeekDirection.Forward,
            hidePlayerChrome = true,
            sequence = 2L,
            updatedAtMillis = 1_900L,
        ).totalSeconds shouldBe 10
    }

    @Test
    fun `seek feedback resets outside a same-direction burst`() {
        val initial = nextVideoPlayerSeekFeedbackState(
            previousState = null,
            direction = VideoPlayerSeekDirection.Forward,
            hidePlayerChrome = true,
            sequence = 1L,
            updatedAtMillis = 1_000L,
        )

        nextVideoPlayerSeekFeedbackState(
            previousState = initial,
            direction = VideoPlayerSeekDirection.Forward,
            hidePlayerChrome = true,
            sequence = 2L,
            updatedAtMillis = 1_901L,
        ).totalSeconds shouldBe 5
        nextVideoPlayerSeekFeedbackState(
            previousState = initial,
            direction = VideoPlayerSeekDirection.Backward,
            hidePlayerChrome = true,
            sequence = 2L,
            updatedAtMillis = 1_500L,
        ).totalSeconds shouldBe 5
    }
}
