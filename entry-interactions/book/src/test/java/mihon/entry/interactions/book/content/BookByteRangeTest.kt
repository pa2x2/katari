package mihon.entry.interactions.book.content

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class BookByteRangeTest {
    @Test
    fun `empty byte range is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            BookByteRange(startInclusive = 4, endExclusive = 4)
        }
    }
}
