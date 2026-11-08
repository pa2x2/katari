package app.katari.entry.interactions.book.document.reader

import app.katari.book.api.document.BookDocument
import app.katari.book.api.document.BookDocumentBlock
import app.katari.book.api.document.BookDocumentBlockContent
import app.katari.book.api.document.BookDocumentBlockId
import app.katari.book.api.document.BookDocumentBlockKind
import app.katari.book.api.document.BookDocumentBlockRole
import app.katari.book.api.document.BookDocumentContent
import app.katari.book.api.document.BookDocumentPosition
import app.katari.book.api.document.BookDocumentRichText
import app.katari.book.api.document.BookDocumentTextRange
import app.katari.entry.interactions.book.document.render.PreparedBookDocument
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
