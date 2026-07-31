package mihon.entry.interactions.book.document.preparation

import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentFontFamily
import mihon.entry.interactions.book.BookResourceRequirement
import mihon.entry.interactions.book.document.resource.PROSE_FONT_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT

/** Derives offline validation constraints from resources referenced by a canonical document. */
internal fun BookDocument.resourceRequirements(): Map<String, BookResourceRequirement> = buildMap {
    fun register(resourceId: String, requirement: BookResourceRequirement) {
        val existing = get(resourceId)
        require(existing == null || existing == requirement) {
            "Document resource $resourceId is used with incompatible validation constraints"
        }
        put(resourceId, requirement)
    }

    fun collect(blocks: List<BookDocumentBlock>) {
        blocks.forEach { block ->
            (block.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId?.let { resourceId ->
                register(resourceId, PROSE_FONT_RESOURCE_REQUIREMENT)
            }
            block.inlineStyles.forEach { inline ->
                (inline.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId?.let { resourceId ->
                    register(resourceId, PROSE_FONT_RESOURCE_REQUIREMENT)
                }
            }
            when (val content = block.content) {
                is BookDocumentBlockContent.Figure ->
                    register(content.image.resourceId, PROSE_IMAGE_RESOURCE_REQUIREMENT)
                is BookDocumentBlockContent.Disclosure -> collect(content.body.blocks)
                else -> Unit
            }
        }
    }
    collect(blocks)
}
