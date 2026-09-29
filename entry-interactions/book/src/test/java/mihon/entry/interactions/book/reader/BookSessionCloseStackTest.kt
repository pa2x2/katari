package mihon.entry.interactions.book.reader

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
}
