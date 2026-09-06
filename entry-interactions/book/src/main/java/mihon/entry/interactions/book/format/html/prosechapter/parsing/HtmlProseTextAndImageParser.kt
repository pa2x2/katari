package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentStyle
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

internal fun HtmlProseBlockParser.addTextAndImageBlocks(
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
    var assignedElementFragments = false
    var added = false
    val pendingFragments = linkedSetOf<String>()

    fun ownedFragments(): List<String> = pendingFragments.toList() +
        if (assignedElementFragments) emptyList() else inheritedFragments + element.fragments()
    fun flushText() {
        val textAdded = addTextBlock(
            nodes = inlineNodes.toList(),
            element = element,
            role = role,
            style = style,
            inheritedFragments = ownedFragments(),
            destination = destination,
            includeElementFragments = !assignedElementFragments,
        )
        if (textAdded) {
            assignedElementFragments = true
            pendingFragments.clear()
        } else {
            inlineNodes.filterIsInstance<Element>().forEach { node ->
                node.getAllElements().forEach { pendingFragments += it.fragments() }
            }
        }
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
                if (imageAdded) {
                    assignedElementFragments = true
                    pendingFragments.clear()
                }
                added = added || imageAdded
                emittedImages.add(image)
            }
        }
    }
    flushText()
    images.filterNot(emittedImages::contains).forEach { image ->
        added = addFigureBlock(image, style, ownedFragments(), destination) || added
        assignedElementFragments = true
    }
    return added
}
