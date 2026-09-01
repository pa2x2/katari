package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentImage
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentStyle
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

internal fun HtmlProseBlockParser.addParagraphBlocks(
    element: Element,
    role: BookDocumentBlockRole,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
    destination: MutableList<HtmlProseParsedBlock>,
): Boolean {
    val images = element.select("img")
    if (images.isEmpty()) {
        return addTextBlock(element.childNodes(), element, role, style, inheritedFragments, destination)
    }
    val emittedImages = mutableSetOf<Element>()
    val inlineNodes = mutableListOf<Node>()
    var assignedParagraphFragments = false
    var added = false

    fun ownedFragments(): List<String> = if (assignedParagraphFragments) emptyList() else inheritedFragments
    fun flushText() {
        val textAdded = addTextBlock(
            nodes = inlineNodes.toList(),
            element = element,
            role = role,
            style = style,
            inheritedFragments = ownedFragments(),
            destination = destination,
            includeElementFragments = !assignedParagraphFragments,
        )
        if (textAdded) assignedParagraphFragments = true
        added = added || textAdded
        inlineNodes.clear()
    }
    element.childNodes().forEach { node ->
        val isolatedImages = (node as? Element)?.let { child ->
            when {
                child.normalName() == "img" -> listOf(child)
                child.text().isBlank() -> child.select("img")
                else -> emptyList()
            }
        }.orEmpty()
        if (isolatedImages.isEmpty()) {
            inlineNodes.add(node)
        } else {
            flushText()
            isolatedImages.forEach { image ->
                val imageAdded = addFigureBlock(image, style, ownedFragments(), destination)
                if (imageAdded) assignedParagraphFragments = true
                added = added || imageAdded
                emittedImages.add(image)
            }
        }
    }
    flushText()
    images.filterNot(emittedImages::contains).forEach { image ->
        added = addFigureBlock(image, style, ownedFragments(), destination) || added
        assignedParagraphFragments = true
    }
    return added
}

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
