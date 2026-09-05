package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentImage
import mihon.book.api.document.BookDocumentStyle
import org.jsoup.nodes.Element

internal fun HtmlProseBlockParser.addFigureBlock(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
    destination: MutableList<HtmlProseParsedBlock>,
): Boolean {
    val image = if (element.normalName() == "img") element else element.selectFirst("img") ?: return false
    val resourceId = image.attr("src").trim()
    require(resourceId.isNotEmpty()) { "HTML image does not reference a readable publication resource" }
    val alt = image.attr("alt").trim().takeIf(String::isNotEmpty)?.let { text ->
        HtmlProseInlineFragment(text, emptyList(), emptyList(), emptyMap())
    }
    val decorative = image.hasAttr("alt") && image.attr("alt").isBlank()
    val captionElement = element.takeIf { it.normalName() == "figure" }
        ?.children()
        ?.firstOrNull { it.normalName() == "figcaption" }
    val caption = captionElement?.let { parseInline(it.childNodes()) }?.takeIf { it.text.isNotBlank() }

    claimSemanticUnit()
    val canonical = StringBuilder()
    val altRich = alt?.let { value ->
        canonical.append(value.text)
        value.toRichText()
    }
    if (canonical.isEmpty()) canonical.append('\uFFFC')
    val captionRich = caption?.let { value ->
        canonical.append('\n')
        val start = canonical.length
        canonical.append(value.text)
        value.toRichText(start)
    }
    canonical.append("\n\n")
    claimCanonicalText(canonical.length)
    val fragments = linkedSetOf<String>().apply {
        addAll(inheritedFragments)
        addAll(element.fragments())
        addAll(image.fragments())
        addAll(captionElement?.fragments().orEmpty())
        addAll(caption?.anchors?.keys.orEmpty())
    }
    val captionStart = if (captionRich == null) 0 else captionRich.range.start
    val anchors = linkedMapOf<String, Int>().apply {
        fragments.forEach { putIfAbsent(it, 0) }
        caption?.anchors?.forEach { (fragment, offset) -> put(fragment, captionStart + offset) }
    }
    destination += HtmlProseParsedBlock(
        text = canonical.toString(),
        plainText = listOfNotNull(alt?.text, caption?.text).joinToString("\n").trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.FIGURE),
        content = BookDocumentBlockContent.Figure(
            image = BookDocumentImage.withAccessibility(
                resourceId = resourceId,
                alternativeText = altRich,
                decorative = decorative,
                width = image.attr("width").toIntOrNull()?.takeIf { it in 1..32_768 },
                height = image.attr("height").toIntOrNull()?.takeIf { it in 1..32_768 },
            ),
            caption = captionRich,
        ),
        style = style,
        explicitId = element.fragments().firstOrNull() ?: image.fragments().firstOrNull()
            ?: inheritedFragments.firstOrNull(),
        fragments = fragments.toList(),
        anchors = anchors,
    )
    return true
}
