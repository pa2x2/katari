package mihon.entry.interactions.book.prose

import android.text.SpannableString
import android.text.SpannableStringBuilder
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentStyle
import org.jsoup.nodes.Element

internal fun StructuredHtmlProseParser.addDisclosure(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
) {
    val summaryElement = element.children().firstOrNull { it.tagName() == "summary" }
    val summaryRendered = summaryElement
        ?.let(::renderHtml)
        ?.trim()
        ?.takeIf { rendered -> rendered.text.any(Char::isReadableDocumentCharacter) }
        ?: RenderedFragment(
            text = SpannableString(DISCLOSURE_SUMMARY_FALLBACK),
            anchorOffsets = emptyMap(),
        )
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
    val rendered = SpannableStringBuilder(summaryRendered.text).append('\n')
    val bodyStartWithinBlock = rendered.length
    rendered.append(body.combinedText)
    if (!rendered.endsWith("\n\n")) rendered.append("\n\n")
    val localAnchors = buildMap {
        element.ownFragments().forEach { put(it, 0) }
        summaryRendered.anchorOffsets.forEach { (fragment, offset) ->
            putIfAbsent(fragment, offset)
        }
        body.document.anchors.forEach { (fragment, position) ->
            val bodyOffset = body.document.logicalOffset(position) ?: return@forEach
            putIfAbsent(fragment, bodyStartWithinBlock + bodyOffset)
        }
    }
    parsedBlocks += ParsedBlock(
        renderedText = SpannableString(rendered),
        logicalPlainText = rendered.toString().trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.DISCLOSURE),
        content = BookDocumentBlockContent.Disclosure(
            summary = summaryRendered.toRichText(rangeStart = 0),
            body = body.document.content,
            bodyStartWithinBlock = bodyStartWithinBlock,
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
