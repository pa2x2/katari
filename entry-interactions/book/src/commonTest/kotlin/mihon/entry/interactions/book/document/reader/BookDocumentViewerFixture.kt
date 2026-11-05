package mihon.entry.interactions.book.document.reader

import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockId
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentContent
import mihon.book.api.document.BookDocumentPosition
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentTextRange
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import tachiyomi.domain.entry.model.EntryChapter
internal abstract class BookDocumentViewerFixture {
    protected fun section(owner: String, texts: List<String>): BookDocumentSection<String> =
        textSection(owner, texts)

    protected fun chapter(id: Long): EntryChapter = EntryChapter.create().copy(id = id, name = "Chapter $id")

    protected fun chapterSection(chapter: EntryChapter, texts: List<String>): BookDocumentSection<EntryChapter> =
        textSection(chapter, texts)

    private fun <T> textSection(owner: T, texts: List<String>): BookDocumentSection<T> {
        var offset = 0
        val blocks = texts.mapIndexed { index, text ->
            if (index > 0) offset += 2
            val start = offset
            offset += text.length
            BookDocumentBlock(
                id = BookDocumentBlockId("block-$index"),
                role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
                content = BookDocumentBlockContent.Text(
                    BookDocumentRichText(
                        text = text,
                        range = BookDocumentTextRange(0, text.length),
                    ),
                ),
                plainText = text,
                sourceFragments = emptyList(),
                logicalStart = start,
                logicalEndExclusive = offset,
            )
        }
        val document = BookDocument(
            resourceId = "shared-resource",
            revision = "r1",
            content = BookDocumentContent(
                text = texts.joinToString("\n\n"),
                blocks = blocks,
                anchors = emptyMap(),
            ),
        )
        val prepared = PreparedBookDocument(document)
        return BookDocumentSection(
            key = owner.toString(),
            owner = owner,
            document = prepared,
            initialPosition = BookDocumentPosition(blocks.first().id, 0),
            resourceLoader = null,
        )
    }
}
