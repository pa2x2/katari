package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentStyle
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

internal fun HtmlProseBlockParser.addTextBlock(
    nodes: List<Node>,
    element: Element,
    role: BookDocumentBlockRole,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
    destination: MutableList<HtmlProseParsedBlock>,
    preserveWhitespace: Boolean = false,
    includeElementFragments: Boolean = true,
): Boolean {
    val rendered = parseInline(nodes, preserveWhitespace)
    if (rendered.text.isBlank()) return false
    val canonical = rendered.withParagraphTerminator()
    claimSemanticUnit()
    claimCanonicalText(canonical.text.length)
    val elementFragments = element.fragments().takeIf { includeElementFragments }.orEmpty()
    val fragments = (inheritedFragments + elementFragments + canonical.anchors.keys).distinct()
    destination += HtmlProseParsedBlock(
        text = canonical.text,
        plainText = rendered.text.trim(),
        role = role,
        content = BookDocumentBlockContent.Text(canonical.toRichText(), preserveWhitespace),
        style = style,
        explicitId = elementFragments.firstOrNull() ?: inheritedFragments.firstOrNull(),
        fragments = fragments,
        anchors = canonical.anchors,
    )
    return true
}

internal fun HtmlProseBlockParser.addThematicBreak(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
    destination: MutableList<HtmlProseParsedBlock>,
): Boolean {
    claimSemanticUnit()
    claimCanonicalText(3)
    val fragments = (inheritedFragments + element.fragments()).distinct()
    destination += HtmlProseParsedBlock(
        text = "\uFFFC\n\n",
        plainText = "",
        role = BookDocumentBlockRole(BookDocumentBlockKind.THEMATIC_BREAK),
        content = BookDocumentBlockContent.ThematicBreak,
        style = style,
        explicitId = fragments.firstOrNull(),
        fragments = fragments,
        anchors = fragments.associateWith { 0 },
    )
    return true
}

internal fun HtmlProseBlockParser.addUnsupportedBlock(
    type: String,
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
    destination: MutableList<HtmlProseParsedBlock>,
) {
    claimSemanticUnit()
    val fragments = (inheritedFragments + element.fragments()).distinct()
    val plain = "Unsupported ${type.take(64)} content"
    claimCanonicalText(plain.length + 2)
    destination += HtmlProseParsedBlock(
        text = "$plain\n\n",
        plainText = plain,
        role = BookDocumentBlockRole(BookDocumentBlockKind.UNSUPPORTED),
        content = BookDocumentBlockContent.Unsupported(type.take(64)),
        style = style,
        explicitId = fragments.firstOrNull(),
        fragments = fragments,
        anchors = fragments.associateWith { 0 },
    )
}
