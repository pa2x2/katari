package mihon.entry.interactions.book

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookSessionCloseStackTest {

    @Test
    fun `closes in reverse order once and continues after close failure`() {
        val events = mutableListOf<String>()
        val stack = BookSessionCloseStack().apply {
            own(AutoCloseable { events += "content" })
            own(
                AutoCloseable {
                    events += "publication"
                    error("failure")
                },
            )
            own(AutoCloseable { events += "reader" })
        }

        assertFailsWith<IllegalStateException> { stack.close() }
        stack.close()

        assertEquals(listOf("reader", "publication", "content"), events)
    }

    @Test
    fun `cancellation unwinds reader publication and content`() = runTest {
        val events = mutableListOf<String>()
        val stack = BookSessionCloseStack().apply {
            own(AutoCloseable { events += "content" })
            own(AutoCloseable { events += "publication" })
            own(AutoCloseable { events += "reader" })
        }
        val job = launch {
            try {
                awaitCancellation()
            } finally {
                stack.close()
            }
        }

        runCurrent()
        job.cancel()
        job.join()

        assertEquals(listOf("reader", "publication", "content"), events)
    }
}
