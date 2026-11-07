package app.katari.sync.client.trigger

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

// The test scheduler's time source, which the batching measures its waits with
@OptIn(ExperimentalCoroutinesApi::class)
class PullBatchingTest {
    @Test
    fun `announcements that never pause still start a pull at the longest wait, and a pause ends a batch`() = runTest {
        val batching = PullBatching(quiet = 2.seconds, longest = 10.seconds, time = testScheduler.timeSource)
        val pulledAt = mutableListOf<Long>()
        val pulling = launch { batching.pulls { pulledAt += testScheduler.currentTime } }

        // Another device sending its library: an announcement every 1.5 s until 13.5 s, then one alone at 20 s
        repeat(10) {
            batching.hear()
            delay(1.5.seconds)
        }
        delay(5.seconds)
        batching.hear()
        delay(5.seconds)
        pulling.cancel()

        pulledAt shouldBe listOf(10_000L, 15_500L, 22_000L)
    }
}
