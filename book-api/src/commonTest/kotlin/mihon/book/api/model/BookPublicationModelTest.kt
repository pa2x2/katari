package mihon.book.api.model

import kotlin.test.Test
import kotlin.test.assertFailsWith

class BookPublicationModelTest {
    @Test
    fun `descriptor requires a stable nonblank identity and positive version`() {
        assertFailsWith<IllegalArgumentException> { BookPublicationModelDescriptor(" ") }
        assertFailsWith<IllegalArgumentException> { BookPublicationModelDescriptor("book.document", version = 0) }
    }
}
