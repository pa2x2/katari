package mihon.entry.interactions.book.prose

import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import org.jsoup.nodes.Element

internal fun StructuredHtmlProseParser.addBlockElement(
    element: Element,
    inheritedStyle: BookDocumentStyle,
    noteContext: Boolean,
    inheritedFragments: List<String>,
): Boolean {
    val before = parsedBlocks.size
    val style = inheritedStyle.merge(element.documentStyle())
    val tag = element.tagName()
    when {
        element.hasAttr("data-katari-unsupported") -> addUnsupportedBlock(element, style, inheritedFragments)
        tag == "hr" -> addThematicBreak(element, style, inheritedFragments)
        tag == "figure" -> addFigure(element, style, inheritedFragments)
        tag == "img" -> addImage(element, style, inheritedFragments)
        tag == "table" -> addTable(element, style, inheritedFragments)
        tag == "details" -> addDisclosure(element, style, inheritedFragments)
        tag == "ol" || tag == "ul" -> addList(element, style, inheritedFragments)
        tag == "pre" -> addPreformatted(element, style, inheritedFragments)
        tag in HEADING_TAGS -> addTextBlock(
            element,
            BookDocumentBlockRole(BookDocumentBlockKind.HEADING, level = tag.drop(1).toInt()),
            style,
            inheritedFragments,
        )
        tag == "blockquote" -> addTextBlock(
            element,
            BookDocumentBlockRole(BookDocumentBlockKind.QUOTE),
            style,
            inheritedFragments,
        )
        tag == "figcaption" || tag == "caption" -> addTextBlock(
            element,
            BookDocumentBlockRole(BookDocumentBlockKind.CAPTION),
            style,
            inheritedFragments,
        )
        tag == "p" -> addParagraph(element, style, noteContext, inheritedFragments)
        tag in CONTAINER_TAGS -> collectChildren(element, style, noteContext)
        else -> addTextBlock(
            element,
            BookDocumentBlockRole(
                if (noteContext) BookDocumentBlockKind.NOTE else BookDocumentBlockKind.OTHER,
            ),
            style,
            inheritedFragments,
        )
    }
    if (parsedBlocks.size > before && inheritedFragments.isNotEmpty()) {
        val index = before
        parsedBlocks[index] = parsedBlocks[index].copy(
            explicitId = parsedBlocks[index].explicitId ?: inheritedFragments.first(),
            fragments = (inheritedFragments + parsedBlocks[index].fragments).distinct(),
        )
    }
    return parsedBlocks.size > before
}
