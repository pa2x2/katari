package mihon.entry.interactions.book.document.render

import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockId

/**
 * Android rendering projection of the processor-neutral [BookDocument].
 *
 * Prepared spans remain an Android-owned projection of the shared semantic document contract.
 */
internal data class PreparedBookDocument(
    val document: BookDocument,
) {
    val blocks: List<BookDocumentBlock>
        get() = document.blocks

    fun block(id: BookDocumentBlockId): BookDocumentBlock? = blocks.firstOrNull { it.id == id }
}

/** Keeps preparation semantic-only; Android spans are projected for composed blocks on demand. */
internal fun BookDocument.toPreparedBookDocument(): PreparedBookDocument = PreparedBookDocument(this)
