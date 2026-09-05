package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import tachiyomi.domain.entry.model.EntryChapter

internal fun pagingSection(text: String, id: Long = 1): BookDocumentSection<EntryChapter> {
    val block = BookDocumentBlock(
        id = BookDocumentBlockId("paragraph"),
        role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
        content = BookDocumentBlockContent.Text(BookDocumentRichText(text, BookDocumentTextRange(0, text.length))),
        plainText = text.trim(),
        sourceFragments = emptyList(),
        logicalStart = 0,
        logicalEndExclusive = text.length,
    )
    val document = BookDocument("chapter-$id", "r1", BookDocumentContent(text, listOf(block), emptyMap()))
    return BookDocumentSection(
        "section-$id",
        EntryChapter.create().copy(id = id, name = "Chapter $id"),
        PreparedBookDocument(document),
        BookDocumentPosition(block.id, 0),
        null,
    )
}

@Composable
internal fun PagingTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        CompositionLocalProvider(
            LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
        ) {
            content()
        }
    }
}

internal fun pagingHtmlSection(html: String): BookDocumentSection<EntryChapter> {
    val body = mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer.sanitize(
        html.encodeToByteArray(),
        mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizationPolicy(),
    )
    val document = mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser()
        .parse("chapter-1", null, body)
    return BookDocumentSection(
        "section-1",
        EntryChapter.create().copy(id = 1L, name = "Chapter 1"),
        PreparedBookDocument(document),
        BookDocumentPosition(document.blocks.first().id, 0),
        null,
    )
}
