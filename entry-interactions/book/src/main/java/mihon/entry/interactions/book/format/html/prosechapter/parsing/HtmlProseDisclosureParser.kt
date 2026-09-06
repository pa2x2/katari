package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentStyle
import org.jsoup.nodes.Element

internal fun HtmlProseBlockParser.addDisclosureBlock(
    element: Element,
    style: BookDocumentStyle,
    noteContext: Boolean,
    inheritedFragments: List<String>,
    destination: MutableList<HtmlProseParsedBlock>,
): Boolean {
    val summaryElement = element.children().firstOrNull { it.normalName() == "summary" }
    val summary = summaryElement?.let { parseInline(it.childNodes()) }
        ?.takeIf { it.text.isNotBlank() }
        ?: HtmlProseInlineFragment("Details", emptyList(), emptyList(), emptyMap())
    val nested = collectNested(
        parent = element,
        style = style,
        noteContext = noteContext,
        excludedChildren = setOfNotNull(summaryElement),
    )
    if (nested.isEmpty()) return false
    val body = assembleContent(nested)
    claimSemanticUnit()
    val bodyStart = summary.text.length + 1
    claimCanonicalText(bodyStart)
    val text = summary.text + "\n" + body.text
    val fragments = linkedSetOf<String>().apply {
        addAll(inheritedFragments)
        addAll(element.fragments())
        addAll(summaryElement?.fragments().orEmpty())
        addAll(summary.anchors.keys)
        addAll(nested.flatMap(HtmlProseParsedBlock::fragments))
    }
    val anchors = linkedMapOf<String, Int>().apply {
        fragments.forEach { putIfAbsent(it, 0) }
        summary.anchors.forEach { (fragment, offset) -> put(fragment, offset) }
        body.anchors.forEach { (fragment, position) ->
            val block = body.blocks.first { it.id == position.blockId }
            put(fragment, bodyStart + block.logicalStart + position.offsetWithinBlock)
        }
    }
    destination += HtmlProseParsedBlock(
        text = text,
        plainText = (summary.text + "\n" + body.text.trim()).trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.DISCLOSURE),
        content = BookDocumentBlockContent.Disclosure(
            summary = summary.toRichText(),
            body = body,
            bodyStartWithinBlock = bodyStart,
            initiallyExpanded = element.hasAttr("open"),
        ),
        style = style,
        explicitId = element.fragments().firstOrNull() ?: inheritedFragments.firstOrNull(),
        fragments = fragments.toList(),
        anchors = anchors,
    )
    return true
}
