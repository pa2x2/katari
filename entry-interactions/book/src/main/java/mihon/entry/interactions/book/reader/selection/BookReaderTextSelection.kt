package mihon.entry.interactions.book.reader.selection

internal data class BookReaderTextSelection(
    val ownerIdentity: String,
    val identity: String,
    val text: String,
    val languageContextText: String,
    val anchor: BookReaderTextSelectionAnchor?,
) {
    init {
        require(ownerIdentity.isNotBlank())
        require(identity.isNotBlank())
        require(text.isNotBlank())
        require(languageContextText.isNotBlank())
    }
}

internal data class BookReaderTextSelectionAnchor(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)
