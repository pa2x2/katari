package mihon.entry.interactions.book.document.preparation

import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockId
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentContent
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentTextRange

internal fun cachedDocument(text: String = "Readable text", resourceId: String = "chapter"): BookDocument {
    return BookDocument(
        resourceId = resourceId,
        revision = "exact-digest",
        content = BookDocumentContent(
            text = text,
            blocks = listOf(
                BookDocumentBlock(
                    id = BookDocumentBlockId("paragraph"),
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
