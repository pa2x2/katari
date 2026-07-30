package mihon.entry.interactions.book.prose

import android.text.SpannableString
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentImage
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import org.jsoup.nodes.Element

internal fun StructuredHtmlProseParser.addImage(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
    caption: String? = null,
    figureElement: Element = element,
) {
    val resource = element.attr("src").trim()
    val alt = element.attr("alt").trim().ifBlank {
        element.attr("title").trim().ifBlank { null }
    }
    if (resource.isBlank()) {
        val fallback = alt ?: IMAGE_UNAVAILABLE_TEXT
        val rendered = SpannableString(fallback.withParagraphTerminator())
        parsedBlocks += ParsedBlock(
            renderedText = rendered,
            logicalPlainText = fallback,
            role = BookDocumentBlockRole(BookDocumentBlockKind.FIGURE),
            content = BookDocumentBlockContent.Text(),
            style = style,
            explicitId = figureElement.id().ifBlank { null },
            fragments = (inheritedFragments + figureElement.fragments()).distinct(),
        )
        return
    }
    val image = BookDocumentImage(
        resourceId = resource,
        alternativeText = alt,
        width = element.positiveDimension("width"),
        height = element.positiveDimension("height"),
    )
    val logicalText = listOfNotNull(alt ?: IMAGE_UNAVAILABLE_TEXT, caption)
        .joinToString("\n")
        .withParagraphTerminator()
    parsedBlocks += ParsedBlock(
        renderedText = SpannableString(logicalText),
        logicalPlainText = logicalText.trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.FIGURE),
        content = BookDocumentBlockContent.Figure(image, caption),
        style = style,
        explicitId = figureElement.id().ifBlank { null },
        fragments = (inheritedFragments + figureElement.fragments()).distinct(),
    )
}

internal fun StructuredHtmlProseParser.addFigure(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
) {
    val image = element.selectFirst("img")
    val caption = element.selectFirst("figcaption")?.text()?.trim()?.ifBlank { null }
    if (image == null) {
        addTextBlock(
            element,
            BookDocumentBlockRole(BookDocumentBlockKind.FIGURE),
            style,
            inheritedFragments,
        )
        return
    }
    addImage(image, style, inheritedFragments, caption, element)
}
