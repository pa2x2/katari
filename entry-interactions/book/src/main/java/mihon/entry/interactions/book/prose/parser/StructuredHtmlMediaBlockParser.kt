package mihon.entry.interactions.book.prose

import android.text.SpannableString
import android.text.SpannableStringBuilder
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentImage
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentTextRange
import org.jsoup.nodes.Element

internal fun StructuredHtmlProseParser.addImage(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
    caption: RenderedFragment? = null,
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
            content = BookDocumentBlockContent.Text(
                value = BookDocumentRichText(
                    text = rendered.toString(),
                    range = BookDocumentTextRange(0, rendered.length),
                ),
            ),
            style = style,
            explicitId = figureElement.id().ifBlank { null },
            fragments = (inheritedFragments + figureElement.fragments()).distinct(),
        )
        return
    }
    val alternativeText = alt ?: IMAGE_UNAVAILABLE_TEXT
    val rendered = SpannableStringBuilder(alternativeText)
    val anchorOffsets = linkedMapOf<String, Int>().apply {
        figureElement.ownFragments().forEach { put(it, 0) }
        element.ownFragments().forEach { putIfAbsent(it, 0) }
    }
    val alternativeRichText = BookDocumentRichText(
        text = alternativeText,
        range = BookDocumentTextRange(0, alternativeText.length),
    )
    val captionRichText = caption?.let { value ->
        rendered.append('\n')
        val captionStart = rendered.length
        rendered.append(value.text)
        value.anchorOffsets.forEach { (fragment, offset) ->
            anchorOffsets.putIfAbsent(fragment, captionStart + offset)
        }
        value.toRichText(captionStart)
    }
    if (!rendered.endsWith("\n\n")) rendered.append("\n\n")
    val image = BookDocumentImage(
        resourceId = resource,
        alternativeText = alternativeRichText,
        width = element.positiveDimension("width"),
        height = element.positiveDimension("height"),
    )
    parsedBlocks += ParsedBlock(
        renderedText = SpannableString(rendered),
        logicalPlainText = rendered.toString().trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.FIGURE),
        content = BookDocumentBlockContent.Figure(image, captionRichText),
        style = style,
        explicitId = figureElement.id().ifBlank { null },
        fragments = (inheritedFragments + figureElement.fragments()).distinct(),
        localAnchorOffsets = anchorOffsets,
    )
}

internal fun StructuredHtmlProseParser.addFigure(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
) {
    val image = element.selectFirst("img")
    val caption = element.selectFirst("figcaption")
        ?.let(::renderHtml)
        ?.trim()
        ?.takeIf { rendered -> rendered.text.any(Char::isReadableDocumentCharacter) }
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
