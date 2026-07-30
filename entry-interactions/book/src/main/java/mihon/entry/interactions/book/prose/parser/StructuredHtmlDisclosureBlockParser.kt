package mihon.entry.interactions.book.prose

import android.text.SpannableString
import android.text.SpannableStringBuilder
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import org.jsoup.nodes.Element

internal fun StructuredHtmlProseParser.addDisclosure(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
) {
    val summaryElement = element.children().firstOrNull { it.tagName() == "summary" }
    val summary = summaryElement?.text()?.trim()?.ifBlank { null } ?: DISCLOSURE_SUMMARY_FALLBACK
    val bodyElement = element.clone().also { clone ->
        clone.children().firstOrNull { it.tagName() == "summary" }?.remove()
        clone.removeDocumentStyleAttributes()
    }
    val body = runCatching {
        StructuredHtmlProseParser(
            resourceId = "$resourceId#disclosure",
            revision = revision,
            body = bodyElement,
        ).parse()
    }.getOrNull()
    if (body == null) {
        addTextBlock(
            summaryElement ?: element,
            BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
            style,
            inheritedFragments,
        )
        return
    }
    val summaryPrefix = "$summary\n"
    val rendered = SpannableStringBuilder(summaryPrefix).append(body.combinedText)
    if (!rendered.endsWith("\n\n")) rendered.append("\n\n")
    val localAnchors = buildMap {
        element.ownFragments().forEach { put(it, 0) }
        body.document.anchors.forEach { (fragment, position) ->
            val bodyOffset = body.document.logicalOffset(position) ?: return@forEach
            putIfAbsent(fragment, summaryPrefix.length + bodyOffset)
        }
    }
    parsedBlocks += ParsedBlock(
        renderedText = SpannableString(rendered),
        logicalPlainText = rendered.toString().trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.DISCLOSURE),
        content = BookDocumentBlockContent.Disclosure(
            summary = summary,
            body = body.document.blocks,
            initiallyExpanded = element.hasAttr("open"),
        ),
        style = style,
        explicitId = element.id().ifBlank { null },
        fragments = (inheritedFragments + element.fragments()).distinct(),
        localAnchorOffsets = localAnchors,
        disclosureBody = body.blocks,
        referencedResources = body.document.resourceIds,
    )
}
