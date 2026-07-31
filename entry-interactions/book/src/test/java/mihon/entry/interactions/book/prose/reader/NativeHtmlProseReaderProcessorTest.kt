package mihon.entry.interactions.book.prose

import mihon.book.api.document.BookDocumentPublicationModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class NativeHtmlProseReaderProcessorTest {
    @Test
    fun `preserves the established prose processor identity`() {
        val processor = NativeHtmlProseReaderProcessor()

        assertEquals("builtin.html.prose-chapter", processor.id)
        assertTrue(processor.supports(BookDocumentPublicationModel.DESCRIPTOR))
    }
}
