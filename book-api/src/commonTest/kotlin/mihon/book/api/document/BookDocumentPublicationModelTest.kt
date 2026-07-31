package mihon.book.api.document

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookDocumentPublicationModelTest {
    @Test
    fun `publication model provides documents by stable resource identity`() {
        val first = document("first", "First")
        val second = document("second", "Second")

        val model = BookDocumentPublicationModel(listOf(first, second))

        assertEquals(first, model.document("first"))
        assertEquals(second, model.document("second"))
        assertEquals(null, model.document("missing"))
    }

    @Test
    fun `publication model preserves canonical documents across serialization`() {
        val model = BookDocumentPublicationModel(listOf(document("chapter", "Chapter")))

        assertEquals(model, Json.decodeFromString<BookDocumentPublicationModel>(Json.encodeToString(model)))
    }

    @Test
    fun `publication model rejects empty and duplicate document identities`() {
        assertFailsWith<IllegalArgumentException> { BookDocumentPublicationModel(emptyList()) }

        val document = document("chapter", "Chapter")
        assertFailsWith<IllegalArgumentException> {
            BookDocumentPublicationModel(listOf(document, document.copy(revision = "r2")))
        }
    }

    private fun document(resourceId: String, text: String): BookDocument {
        val block = bookDocumentTextBlock(resourceId, text, logicalStart = 0)
        return bookDocument(text, listOf(block)).copy(resourceId = resourceId)
    }
}
