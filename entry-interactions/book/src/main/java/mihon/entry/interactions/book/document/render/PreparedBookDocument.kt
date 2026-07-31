package mihon.entry.interactions.book.document.render

import android.text.Spanned
import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockId

/**
 * Android rendering projection of the processor-neutral [BookDocument].
 *
 * Prepared spans remain an Android-owned projection of the shared semantic document contract.
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
        require(combinedText.toString() == document.content.text) {
            "prepared text must exactly match canonical semantic document text"
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
        val semanticBody = (block.content as? BookDocumentBlockContent.Disclosure)?.body?.blocks.orEmpty()
        require(disclosureBody.map(PreparedBookDocumentBlock::block) == semanticBody) {
            "prepared disclosure blocks must match the semantic disclosure body"
        }
    }
}
