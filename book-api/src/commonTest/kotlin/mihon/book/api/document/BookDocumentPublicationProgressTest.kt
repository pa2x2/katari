package mihon.book.api.document

import kotlin.test.Test
import kotlin.test.assertEquals

class BookDocumentPublicationProgressTest {
    @Test
    fun `publication progress is weighted by canonical logical extent`() {
        val progress = BookDocumentPublicationProgress(
            listOf(
                document("first", "a".repeat(100)),
                document("second", "b".repeat(300)),
            ),
        )

        assertEquals(0.25, progress.totalProgression("second", 0.0))
        assertEquals(0.625, progress.totalProgression("second", 0.5))
        assertEquals(1.0, progress.totalProgression("second", 1.0))
    }

    @Test
    fun `halfway through fifth of ten equal documents is forty five percent`() {
        val documents = (1..10).map { index -> document("chapter-$index", "x".repeat(100)) }

        val progress = BookDocumentPublicationProgress(documents)

        assertEquals(0.45, progress.totalProgression("chapter-5", 0.5))
    }

    private fun document(resourceId: String, text: String) = BookDocument(
        resourceId = resourceId,
        revision = "r1",
        content = BookDocumentContent(
            text = text,
            blocks = listOf(
                BookDocumentBlock(
                    id = BookDocumentBlockId("block"),
                    role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
                    content = BookDocumentBlockContent.Text(
                        BookDocumentRichText(text, BookDocumentTextRange(0, text.length)),
                    ),
                    plainText = text,
                    sourceFragments = emptyList(),
                    logicalStart = 0,
                    logicalEndExclusive = text.length,
                ),
            ),
            anchors = emptyMap(),
        ),
    )
}
