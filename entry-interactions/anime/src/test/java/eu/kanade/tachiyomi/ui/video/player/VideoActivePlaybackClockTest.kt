package eu.kanade.tachiyomi.ui.video.player

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VideoActivePlaybackClockTest {

    @Test
    fun `checkpoints count only active intervals and never count one twice`() {
        var elapsed = 1_000L
        val clock = VideoActivePlaybackClock { elapsed }

        clock.setActive(true)
        elapsed = 4_000L
        clock.setActive(false)
        elapsed = 9_000L
        clock.checkpoint() shouldBe 3_000L
        clock.checkpoint() shouldBe 0L

        clock.setActive(true)
        elapsed = 11_500L
        clock.checkpoint() shouldBe 2_500L
        elapsed = 13_000L
        clock.checkpoint() shouldBe 1_500L
    }
}
