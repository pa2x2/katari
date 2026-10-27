package mihon.entry.interactions.reader.settings

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

/*
 * The book reader's typography choices. Each starts at BOOK, which keeps what the book asks for; any other value
 * replaces it for body text, while headings keep the book's own.
 */

enum class BookDocumentLineSpacing(val stringRes: StringResource) {
    BOOK(MR.strings.book_document_reader_typography_book),
    TIGHT(MR.strings.book_document_reader_line_spacing_tight),
    NORMAL(MR.strings.book_document_reader_line_spacing_normal),
    LOOSE(MR.strings.book_document_reader_line_spacing_loose),
}

enum class BookDocumentMargins(val stringRes: StringResource) {
    BOOK(MR.strings.book_document_reader_typography_book),
    NARROW(MR.strings.book_document_reader_margins_narrow),
    NORMAL(MR.strings.book_document_reader_margins_normal),
    WIDE(MR.strings.book_document_reader_margins_wide),
}

/** LEFT and JUSTIFY apply to paragraphs the book aligns to the start or justifies; centred and end-aligned text stays. */
enum class BookDocumentTextAlignment(val stringRes: StringResource) {
    BOOK(MR.strings.book_document_reader_typography_book),
    LEFT(MR.strings.book_document_reader_text_alignment_left),
    JUSTIFY(MR.strings.book_document_reader_text_alignment_justify),
}
