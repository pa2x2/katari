package mihon.entry.interactions.book.format.html.prosechapter.sanitization

import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.io.ByteArrayInputStream

internal object HtmlProseSanitizer {
    fun sanitize(bytes: ByteArray): Element {
        require(bytes.isNotEmpty()) { "The prose chapter is empty" }
        require(bytes.size <= HtmlProseChapterContract.MAX_RAW_BYTES) {
            "The prose chapter exceeds its byte limit"
        }
        val document = ByteArrayInputStream(bytes).use { input -> Jsoup.parse(input, null, "") }
        validateShape(document)

        val css = HtmlProseCss.parse(document.select("style").map(Element::data))
        document.select(ACTIVE_ELEMENTS).remove()
        document.select(UNSUPPORTED_PASSIVE_ELEMENTS).forEach(::replaceUnsupportedElement)
        document.select("script, style, link, meta, base, title, noscript").remove()
        document.body().getAllElements().forEach(css::applyTo)
        preserveDocumentLanguage(document)
        sanitizeElements(document.body())
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

    private fun sanitizeElements(body: Element) {
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
                "a" -> sanitizeLink(element)
                "img" -> sanitizeImage(element)
            }
        }
    }

    private fun sanitizeLink(element: Element) {
        val href = element.attr("href").trim()
        if (
            !(href.startsWith("#") && href.length > 1) &&
            !href.startsWith("https://", ignoreCase = true) &&
            !href.startsWith("http://", ignoreCase = true)
        ) {
            element.removeAttr("href")
        } else {
            element.attr("href", href.take(2_048))
        }
    }

    private fun sanitizeImage(element: Element) {
        val source = element.attr("src").trim()
        val unsafe = source.isEmpty() || source.startsWith("data:", true) || source.startsWith("javascript:", true)
        if (unsafe) element.removeAttr("src") else element.attr("src", source.take(2_048))
        element.attr("alt").takeIf(String::isNotEmpty)?.let { element.attr("alt", it.take(2_048)) }
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

private const val ACTIVE_ELEMENTS = "script, iframe, object, embed, canvas"
private const val UNSUPPORTED_PASSIVE_ELEMENTS = "audio, video, svg, math"

private val ALLOWED_ELEMENTS = setOf(
    "a", "abbr", "address", "article", "aside", "b", "blockquote", "br", "caption", "cite",
    "code", "dd", "del", "details", "dfn", "div", "dl", "dt", "em", "figcaption", "figure",
    "footer", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr", "i", "img", "ins", "kbd",
    "li", "main", "mark", "nav", "ol", "p", "pre", "q", "s", "samp", "section", "small",
    "span", "strong", "sub", "summary", "sup", "table", "tbody", "td", "tfoot", "th", "thead",
    "tr", "u", "ul", "var",
)

private val GLOBAL_ATTRIBUTES = setOf("id", "role", "dir", "lang", "data-katari-unsupported")
private val TAG_ATTRIBUTES = mapOf(
    "a" to setOf("href", "name"),
    "img" to setOf("src", "alt", "width", "height"),
    "ol" to setOf("start", "type"),
    "li" to setOf("value"),
    "td" to setOf("colspan", "rowspan", "scope"),
    "th" to setOf("colspan", "rowspan", "scope"),
    "details" to setOf("open"),
)
