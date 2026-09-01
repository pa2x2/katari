package mihon.entry.interactions.book.format.html.prosechapter.sanitization

import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.toBookDocumentLinkTarget
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.parser.Parser
import java.io.ByteArrayInputStream

internal object HtmlProseSanitizer {
    fun sanitize(
        bytes: ByteArray,
        policy: HtmlProseSanitizationPolicy = HtmlProseSanitizationPolicy(),
    ): Element {
        require(bytes.isNotEmpty()) { "The prose chapter is empty" }
        require(bytes.size <= HtmlProseChapterContract.MAX_RAW_BYTES) {
            "The prose chapter exceeds its byte limit"
        }
        val parser = if (policy.xmlSyntax) Parser.xmlParser() else Parser.htmlParser()
        val document = ByteArrayInputStream(bytes).use { input -> Jsoup.parse(input, null, "", parser) }
        validateShape(document)

        val css = HtmlProseCss.parse(document.select("style").map(Element::data) + policy.additionalStyleSheets)
        document.select(ACTIVE_ELEMENTS).remove()
        document.select("svg").forEach { element -> replaceInlineImage(element, policy) }
        document.select(UNSUPPORTED_PASSIVE_ELEMENTS).forEach(::replaceUnsupportedElement)
        document.select("script, style, link, meta, base, title, noscript").remove()
        document.body().getAllElements().forEach { element ->
            css.applyTo(element)
            val family = element.attr("${DOCUMENT_STYLE_ATTRIBUTE_PREFIX}font-family")
            policy.resolveFontResource(family)?.let { resourceId ->
                element.attr("${DOCUMENT_STYLE_ATTRIBUTE_PREFIX}font-resource", resourceId.take(2_048))
            }
        }
        preserveDocumentLanguage(document)
        sanitizeElements(document.body(), policy)
        validateShape(document.body())
        return document.body()
    }

    private fun preserveDocumentLanguage(document: Document) {
        val body = document.body()
        if (body.attr("lang").isNotBlank()) return
        val documentElement = document.selectFirst("html") ?: return
        val documentLanguage = documentElement.attr("lang")
            .ifBlank { documentElement.attr("xml:lang") }
        if (documentLanguage.isNotBlank()) body.attr("lang", documentLanguage)
    }

    private fun sanitizeElements(body: Element, policy: HtmlProseSanitizationPolicy) {
        val comments = mutableListOf<Comment>()
        val nodes = ArrayDeque<Node>()
        nodes.addAll(body.childNodes())
        while (nodes.isNotEmpty()) {
            val node = nodes.removeLast()
            if (node is Comment) comments += node else nodes.addAll(node.childNodes())
        }
        comments.forEach(Node::remove)
        body.getAllElements().asReversed().forEach { element ->
            if (element === body) return@forEach
            if (element.normalName() !in ALLOWED_ELEMENTS) {
                element.unwrap()
                return@forEach
            }
            val allowed = GLOBAL_ATTRIBUTES + TAG_ATTRIBUTES[element.normalName()].orEmpty()
            element.attributes().asList().forEach { attribute ->
                if (attribute.key !in allowed && !attribute.key.startsWith(DOCUMENT_STYLE_ATTRIBUTE_PREFIX)) {
                    element.removeAttr(attribute.key)
                }
            }
            element.attr("id").takeIf(String::isNotEmpty)?.let { element.attr("id", it.take(256)) }
            element.attr("name").takeIf(String::isNotEmpty)?.let { element.attr("name", it.take(256)) }
            when (element.normalName()) {
                "a" -> sanitizeLink(element, policy)
                "img" -> sanitizeImage(element, policy)
            }
        }
    }

    private fun sanitizeLink(element: Element, policy: HtmlProseSanitizationPolicy) {
        val href = element.attr("href").trim()
        element.removeAttr(BOOK_RESOURCE_ID_ATTRIBUTE)
        element.removeAttr(BOOK_RESOURCE_FRAGMENT_ATTRIBUTE)
        element.removeAttr(BOOK_RESOURCE_REFERENCE_ATTRIBUTE)
        val resolvedTarget = policy.resolveLink(href)
        val target = if (element.isContextualReference()) resolvedTarget?.asContextualReference() else resolvedTarget
        when (target) {
            is BookDocumentLinkTarget.Anchor -> element.attr("href", "#${target.fragment}".take(2_048))
            is BookDocumentLinkTarget.External -> element.attr("href", target.url.take(2_048))
            is BookDocumentLinkTarget.Resource -> {
                element.removeAttr("href")
                element.attr(BOOK_RESOURCE_ID_ATTRIBUTE, target.resourceId.take(2_048))
                target.fragment?.let { fragment ->
                    element.attr(BOOK_RESOURCE_FRAGMENT_ATTRIBUTE, fragment.take(256))
                }
            }
            is BookDocumentLinkTarget.Reference -> {
                element.removeAttr("href")
                target.resourceId?.let { element.attr(BOOK_RESOURCE_ID_ATTRIBUTE, it.take(2_048)) }
                element.attr(BOOK_RESOURCE_FRAGMENT_ATTRIBUTE, target.fragment.take(256))
                element.attr(BOOK_RESOURCE_REFERENCE_ATTRIBUTE, "true")
            }
            null -> element.removeAttr("href")
        }
    }

    private fun Element.isContextualReference(): Boolean =
        attr("role").split(Regex("\\s+")).any { it.equals("doc-noteref", true) } ||
            attr("epub:type").split(Regex("\\s+")).any { it.equals("noteref", true) }

    private fun BookDocumentLinkTarget.asContextualReference(): BookDocumentLinkTarget? = when (this) {
        is BookDocumentLinkTarget.Anchor -> BookDocumentLinkTarget.Reference(fragment = fragment)
        is BookDocumentLinkTarget.Resource -> fragment?.let { value ->
            BookDocumentLinkTarget.Reference(resourceId, value)
        }
        is BookDocumentLinkTarget.Reference -> this
        is BookDocumentLinkTarget.External -> null
    }

    private fun sanitizeImage(element: Element, policy: HtmlProseSanitizationPolicy) {
        if (element.attr(BOOK_RESOLVED_IMAGE_ATTRIBUTE) == "true") return
        val source = element.attr("src").trim()
        val resolved = policy.resolveImageResource(source)
        if (resolved.isNullOrBlank()) element.removeAttr("src") else element.attr("src", resolved.take(2_048))
        element.attr("alt").takeIf(String::isNotEmpty)?.let { element.attr("alt", it.take(2_048)) }
    }

    private fun replaceInlineImage(element: Element, policy: HtmlProseSanitizationPolicy) {
        val resourceId = policy.resolveInlineImage(element.outerHtml())
        if (resourceId.isNullOrBlank()) {
            replaceUnsupportedElement(element)
            return
        }
        val replacement = Element("img")
            .attr("src", resourceId.take(2_048))
            .attr(BOOK_RESOLVED_IMAGE_ATTRIBUTE, "true")
        element.attr("aria-label").trim().ifBlank {
            element.selectFirst("title")?.text()?.trim().orEmpty()
        }.takeIf(String::isNotBlank)?.let { replacement.attr("alt", it.take(2_048)) }
        element.replaceWith(replacement)
    }

    private fun replaceUnsupportedElement(element: Element) {
        val replacement = Element("div")
            .attr("data-katari-unsupported", element.normalName().take(64))
            .text("Unsupported ${element.normalName()} content")
        element.replaceWith(replacement)
    }

    private fun validateShape(root: Element) {
        val elements = root.getAllElements()
        if (elements.size > HtmlProseChapterContract.MAX_DOM_NODES) {
            throw HtmlProseLimitExceededException("HTML contains too many DOM nodes")
        }
        val stack = ArrayDeque<Pair<Node, Int>>()
        stack.add(root to 0)
        while (stack.isNotEmpty()) {
            val (node, depth) = stack.removeLast()
            if (depth > HtmlProseChapterContract.MAX_DOM_DEPTH) {
                throw HtmlProseLimitExceededException("HTML nesting exceeds the supported depth")
            }
            node.childNodes().forEach { child -> stack.add(child to depth + 1) }
        }
    }
}

internal data class HtmlProseSanitizationPolicy(
    val xmlSyntax: Boolean = false,
    val additionalStyleSheets: List<String> = emptyList(),
    val resolveLink: (String) -> BookDocumentLinkTarget? = String::toBookDocumentLinkTarget,
    val resolveImageResource: (String) -> String? = { source ->
        source.trim().takeUnless { value ->
            value.isEmpty() || value.startsWith("data:", true) || value.startsWith("javascript:", true)
        }
    },
    val resolveInlineImage: (String) -> String? = { null },
    val resolveFontResource: (String) -> String? = { null },
)

internal const val BOOK_RESOURCE_ID_ATTRIBUTE = "data-katari-book-resource-id"
internal const val BOOK_RESOURCE_FRAGMENT_ATTRIBUTE = "data-katari-book-resource-fragment"
internal const val BOOK_RESOURCE_REFERENCE_ATTRIBUTE = "data-katari-book-resource-reference"
private const val BOOK_RESOLVED_IMAGE_ATTRIBUTE = "data-katari-book-resolved-image"

private const val ACTIVE_ELEMENTS = "script, iframe, object, embed, canvas"
private const val UNSUPPORTED_PASSIVE_ELEMENTS = "audio, video, math"

private val ALLOWED_ELEMENTS = setOf(
    "a", "abbr", "address", "article", "aside", "b", "blockquote", "br", "caption", "cite",
    "code", "dd", "del", "details", "dfn", "div", "dl", "dt", "em", "figcaption", "figure",
    "footer", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr", "i", "img", "ins", "kbd",
    "li", "main", "mark", "nav", "ol", "p", "pre", "q", "s", "samp", "section", "small",
    "span", "strong", "sub", "summary", "sup", "table", "tbody", "td", "tfoot", "th", "thead",
    "tr", "u", "ul", "var",
)

private val GLOBAL_ATTRIBUTES = setOf(
    "id",
    "role",
    "dir",
    "lang",
    "xml:lang",
    "epub:type",
    "data-katari-unsupported",
    BOOK_RESOURCE_ID_ATTRIBUTE,
    BOOK_RESOURCE_FRAGMENT_ATTRIBUTE,
    BOOK_RESOURCE_REFERENCE_ATTRIBUTE,
    BOOK_RESOLVED_IMAGE_ATTRIBUTE,
)
private val TAG_ATTRIBUTES = mapOf(
    "a" to setOf("href", "name"),
    "img" to setOf("src", "alt", "width", "height"),
    "ol" to setOf("start", "type"),
    "li" to setOf("value"),
    "td" to setOf("colspan", "rowspan", "scope"),
    "th" to setOf("colspan", "rowspan", "scope"),
    "details" to setOf("open"),
)
