package mihon.entry.interactions.book.document.render

import android.text.Spanned
import mihon.entry.interactions.book.document.model.BookDocument
import mihon.entry.interactions.book.document.model.BookDocumentBlock
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBlockId

/**
 * Android rendering projection of the processor-neutral [BookDocument].
 *
 * Keeping prepared spans outside the semantic model preserves the model's intended future move to `book-api`.
 */
internal data class PreparedBookDocument(
    val document: BookDocument,
    val blocks: List<PreparedBookDocumentBlock>,
    val combinedText: Spanned,
) {
    init {
        require(blocks.map { it.block.id } == document.blocks.map(BookDocumentBlock::id)) {
            "prepared blocks must match the semantic document"
        }
        require(combinedText.length == document.logicalExtent) {
            "prepared text length must match the semantic document extent"
        }
    }

    fun block(id: BookDocumentBlockId): PreparedBookDocumentBlock? = blocks.firstOrNull { it.block.id == id }
}

internal data class PreparedBookDocumentBlock(
    val block: BookDocumentBlock,
    val renderedText: Spanned,
    val disclosureBody: List<PreparedBookDocumentBlock> = emptyList(),
) {
    init {
        val semanticBody = (block.content as? BookDocumentBlockContent.Disclosure)?.body.orEmpty()
        require(disclosureBody.map(PreparedBookDocumentBlock::block) == semanticBody) {
            "prepared disclosure blocks must match the semantic disclosure body"
        }
    }
}
