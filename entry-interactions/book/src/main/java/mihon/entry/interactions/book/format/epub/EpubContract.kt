package mihon.entry.interactions.book.format.epub

internal object EpubContract {
    const val FORMAT = "application/epub+zip"
    const val MAX_XML_BYTES = 4 * 1024 * 1024
    const val MAX_DOCUMENT_BYTES = 16 * 1024 * 1024
    const val MAX_STYLE_SHEET_BYTES = 2 * 1024 * 1024
}
