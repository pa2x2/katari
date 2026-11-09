package app.katari.sync.client.trigger

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// The test scheduler's time source, which the pacing measures its gap with
@OptIn(ExperimentalCoroutinesApi::class)
class RunPacingTest {
    @Test
    fun `a signal after a pause acts at once, and signals that keep coming act at most once a gap`() = runTest {
        val pacing = RunPacing(gap = 2.seconds, time = testScheduler.timeSource)
        val actedAt = mutableListOf<Long>()
        val following = launch { pacing.follow { actedAt += testScheduler.currentTime } }

        // A library update writing an entry every 0.75 s until 4.5 s, then one edit alone at 10 s
        repeat(7) {
            pacing.hear()
            delay(750.milliseconds)
        }
        delay(4.75.seconds)
        pacing.hear()
        delay(1.seconds)
        following.cancel()

        actedAt shouldBe listOf(0L, 2_000L, 4_000L, 6_000L, 10_000L)
    }
}
