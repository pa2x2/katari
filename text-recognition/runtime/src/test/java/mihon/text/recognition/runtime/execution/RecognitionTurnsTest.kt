package mihon.text.recognition.runtime.execution

import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.text.recognition.api.request.TextRecognitionPriority.Background
import mihon.text.recognition.api.request.TextRecognitionPriority.Interactive
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecognitionTurnsTest {

    @Test
    fun `the page on screen runs before waiting background work, even when a page handed the turn was dropped`() =
        runTest {
            val turns = RecognitionTurns()
            val order = mutableListOf<String>()
            val running = CompletableDeferred<Unit>()
            lateinit var scrolledAway: Job
            launch {
                turns.withTurn(Background) {
                    running.await()
                    order += "running"
                }
                // The next waiter already holds the turn but has not resumed yet.
                scrolledAway.cancel()
            }
            runCurrent()
            launch { turns.withTurn(Background) { order += "background" } }
            runCurrent()
            scrolledAway = launch { turns.withTurn(Interactive) { order += "scrolled away" } }
            launch { turns.withTurn(Interactive) { order += "on screen" } }
            runCurrent()

            running.complete(Unit)
            runCurrent()

            order shouldContainExactly listOf("running", "on screen", "background")
        }
}
