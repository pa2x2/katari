package eu.kanade.tachiyomi.ui.video.player.gesture

import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection.Backward
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VideoPlayerSeekFeedbackTest {

    @Test
    fun `a burst of seeks back past the start counts only the distance to the start`() {
        var positionMs = 15_000L
        var feedback: VideoPlayerSeekFeedbackState? = null
        repeat(5) { press ->
            val targetMs = resolveVideoPlayerSeekPosition(positionMs, DURATION_MS, Backward, stepSeconds = 10)
            feedback = nextVideoPlayerSeekFeedbackState(
                previousState = feedback,
                direction = Backward,
                movedMs = positionMs - targetMs,
                hidePlayerChrome = true,
                sequence = press.toLong(),
                updatedAtMillis = press * 100L,
            )
            positionMs = targetMs
        }

        feedback?.totalMs shouldBe 15_000L
    }

    private companion object {
        const val DURATION_MS = 2_708_000L
    }
}
