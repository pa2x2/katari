package mihon.entry.interactions.book.prose

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import java.net.URI
import java.nio.charset.StandardCharsets

internal const val UNSUPPORTED_CONTENT_BLOCK_TEXT = "-- unsupported content block --"

/**
 * Converts provider HTML into the passive prose contract before semantic parsing.
 *
 * Raw CSS is never returned. A bounded reviewed subset is translated into
 * Katari-owned data attributes, and executable or website-only elements are
 * removed before the safelist is applied.
 */
internal fun sanitizeProseDocument(bytes: ByteArray): Element {
    val parsed = Jsoup.parse(bytes.inputStream(), null, "")
    normalizeUnsupportedBlocks(parsed)
    removeWebsiteMaterial(parsed)
    translateSafeStyles(parsed)
    val cleaned = Cleaner(PROSE_SAFELIST).clean(parsed)
    cleaned.outputSettings()
        .charset(StandardCharsets.UTF_8)
        .prettyPrint(false)
    cleanLinks(cleaned)
    cleanResourceReferences(cleaned)
    return cleaned.body()
}

private fun normalizeUnsupportedBlocks(document: Document) {
    val candidates = document.body().select("*").filter(Element::isUnsupportedContentBlock)
    candidates.forEach { element ->
        if (element.parents().any(Element::isUnsupportedContentBlock)) return@forEach
        val diagnosticType = element.unsupportedDiagnosticType()
        val replacement = Element("div")
            .attr("data-katari-unsupported", diagnosticType)
            .text(UNSUPPORTED_CONTENT_BLOCK_TEXT)
        element.id().takeIf(String::isNotBlank)?.let { replacement.attr("id", it) }
        element.replaceWith(replacement)
    }
}

private fun Element.isUnsupportedContentBlock(): Boolean {
    val tag = tagName().lowercase()
    if (tag in UNSUPPORTED_TAGS) return true
    if (tag == "math") return true
    val classes = classNames().map(String::lowercase)
    return hasAttr("data-chart-format") ||
        attr("data-format").equals("latex", ignoreCase = true) ||
        classes.any { className ->
            className == "math" ||
                className.contains("math-equation") ||
                className.contains("specialized-chart")
        }
}

private fun Element.unsupportedDiagnosticType(): String {
    val tag = tagName().lowercase()
    return when {
        tag == "div" && hasAttr("data-chart-format") -> "chart"
        tag == "div" && (
            attr("data-format").equals("latex", ignoreCase = true) ||
                classNames().any { it.contains("math", ignoreCase = true) }
            ) -> "math"
        else -> tag
    }.take(MAX_DIAGNOSTIC_LENGTH)
}

private fun removeWebsiteMaterial(document: Document) {
    document.select(
        "script, noscript, form, input, button, select, textarea, option, " +
            "link, meta, base, template",
    ).remove()
    document.body().select("*").filter { element ->
        element.classNames().any { className ->
            val normalized = className.lowercase()
            normalized == "ad" ||
                normalized == "ads" ||
                normalized == "advert" ||
                normalized == "advertisement" ||
                normalized.startsWith("ads-") ||
                normalized.endsWith("-ads")
        }
    }.forEach(Element::remove)
    document.select("*").forEach { element ->
        element.attributes().asList()
            .filter { it.key.startsWith("on", ignoreCase = true) }
            .forEach { element.removeAttr(it.key) }
    }
}

private fun translateSafeStyles(document: Document) {
    val styleSheets = document.select("style").joinToString("\n", transform = Element::data)
    val fontFaces = parseFontFaces(styleSheets)
    val rules = parseStyleRules(styleSheets)
    document.select("*").forEach { element ->
        val declarations = linkedMapOf<String, String>()
        rules.filter { it.matches(element) }.forEach { declarations.putAll(it.declarations) }
        declarations.putAll(parseDeclarations(element.attr("style")))
        element.removeAttr("style")
        applySafeStyle(element, declarations, fontFaces)
    }
    document.select("style").remove()
}

private fun applySafeStyle(
    element: Element,
    declarations: Map<String, String>,
    fontFaces: Map<String, String>,
) {
    declarations["text-align"]
        ?.trim()
        ?.lowercase()
        ?.takeIf { it in SAFE_ALIGNMENTS }
        ?.let { element.attr("data-katari-align", it) }
    declarations["white-space"]
        ?.trim()
        ?.lowercase()
        ?.takeIf { it in SAFE_WHITE_SPACE }
        ?.let { element.attr("data-katari-white-space", it) }
    parseColor(declarations["color"])?.let { element.attr("data-katari-color", it) }
    parseColor(declarations["background-color"] ?: declarations["background"])
        ?.let { element.attr("data-katari-background", it) }
    parseBorder(declarations["border"])?.let { border ->
        element.attr("data-katari-border", border)
    }
    parsePaddingEm(declarations["padding"])?.let { padding ->
        element.attr("data-katari-padding-em", padding.toString())
    }
    parseFontScale(declarations["font-size"])?.let { scale ->
        element.attr("data-katari-font-scale", scale.toString())
    }
    declarations["font-weight"]
        ?.trim()
        ?.lowercase()
        ?.takeIf { it == "bold" || it.toIntOrNull()?.let { weight -> weight >= 600 } == true }
        ?.let { element.attr("data-katari-bold", "true") }

    val requestedFamily = declarations["font-family"]
        ?.substringBefore(',')
        ?.trim()
        ?.trim('"', '\'')
        ?.takeIf(String::isNotBlank)
    val normalizedFamily = requestedFamily?.lowercase()
    when {
        requestedFamily != null && fontFaces[normalizedFamily] != null ->
            element.attr("data-katari-font-resource", checkNotNull(fontFaces[normalizedFamily]))
        normalizedFamily in SERIF_FAMILIES -> element.attr("data-katari-font-generic", "serif")
        normalizedFamily in SANS_SERIF_FAMILIES -> element.attr("data-katari-font-generic", "sans-serif")
        normalizedFamily in MONOSPACE_FAMILIES -> element.attr("data-katari-font-generic", "monospace")
    }
}

private fun parseFontFaces(styleSheets: String): Map<String, String> = buildMap {
    FONT_FACE_REGEX.findAll(styleSheets).forEach { match ->
        val declarations = parseDeclarations(match.groupValues[1])
        val family = declarations["font-family"]
            ?.trim()
            ?.trim('"', '\'')
            ?.lowercase()
            ?.takeIf(String::isNotBlank)
            ?: return@forEach
        val resourceId = declarations["src"]
            ?.let(URL_FUNCTION_REGEX::find)
            ?.groupValues
            ?.get(2)
            ?.trim()
            ?.takeIf(::isSafeResourceReference)
            ?: return@forEach
        put(family, resourceId)
    }
}

private fun parseStyleRules(styleSheets: String): List<SafeCssRule> =
    STYLE_RULE_REGEX.findAll(styleSheets.replace(FONT_FACE_REGEX, ""))
        .flatMap { match ->
            val declarations = parseDeclarations(match.groupValues[2])
            match.groupValues[1].split(',').asSequence().mapNotNull { selector ->
                SafeCssSelector.parse(selector.trim())?.let { SafeCssRule(it, declarations) }
            }
        }
        .take(MAX_STYLE_RULES)
        .toList()

private fun parseDeclarations(value: String): Map<String, String> =
    value.split(';')
        .asSequence()
        .mapNotNull { declaration ->
            val property = declaration.substringBefore(':', "").trim().lowercase()
            val rawValue = declaration.substringAfter(':', "").trim()
            if (property.isBlank() || rawValue.isBlank()) null else property to rawValue
        }
        .take(MAX_DECLARATIONS)
        .toMap()

private fun parseColor(value: String?): String? {
    val normalized = value?.trim()?.lowercase() ?: return null
    val hex = normalized.removePrefix("#")
    return when {
        normalized in NAMED_COLORS -> NAMED_COLORS[normalized]
        hex.length == 3 && hex.all(Char::isSafeHexDigit) ->
            "#ff${hex.flatMap { listOf(it, it) }.joinToString("")}"
        hex.length == 6 && hex.all(Char::isSafeHexDigit) -> "#ff$hex"
        hex.length == 8 && hex.all(Char::isSafeHexDigit) -> "#$hex"
        else -> null
    }
}

private fun parseBorder(value: String?): String? {
    val parts = value?.trim()?.split(Regex("\\s+")).orEmpty()
    val width = parts.firstNotNullOfOrNull(::parseCssLengthDp)?.coerceIn(0.5f, 8f) ?: return null
    val style = parts.firstOrNull { it.lowercase() in SAFE_BORDER_STYLES }?.lowercase() ?: return null
    val color = parts.firstNotNullOfOrNull(::parseColor)
    return listOf(width.toString(), style, color.orEmpty()).joinToString("|")
}

private fun parsePaddingEm(value: String?): Float? {
    val first = value?.trim()?.substringBefore(' ') ?: return null
    return when {
        first.endsWith("rem", ignoreCase = true) ->
            first.dropLast(3).toFloatOrNull()
        first.endsWith("em", ignoreCase = true) ->
            first.dropLast(2).toFloatOrNull()
        first.endsWith("px", ignoreCase = true) ->
            first.dropLast(2).toFloatOrNull()?.div(16f)
        else -> null
    }?.coerceIn(0f, 4f)
}

private fun parseFontScale(value: String?): Float? {
    val normalized = value?.trim()?.lowercase() ?: return null
    return when {
        normalized.endsWith("%") -> normalized.dropLast(1).toFloatOrNull()?.div(100f)
        normalized.endsWith("rem") -> normalized.dropLast(3).toFloatOrNull()
        normalized.endsWith("em") -> normalized.dropLast(2).toFloatOrNull()
        else -> null
    }?.coerceIn(0.75f, 1.5f)
}

private fun parseCssLengthDp(value: String): Float? {
    val normalized = value.lowercase()
    return when {
        normalized.endsWith("px") -> normalized.dropLast(2).toFloatOrNull()
        normalized.endsWith("dp") -> normalized.dropLast(2).toFloatOrNull()
        normalized.endsWith("em") -> normalized.dropLast(2).toFloatOrNull()?.times(16f)
        else -> null
    }
}

private fun cleanLinks(document: Document) {
    document.select("a[href]").forEach { link ->
        val href = link.attr("href").trim()
        val safe = when {
            href.startsWith("#") -> href.length in 2..MAX_REFERENCE_LENGTH
            else -> isSafeExternalUrl(href)
        }
        if (!safe) link.removeAttr("href") else link.attr("href", href)
    }
}

private fun cleanResourceReferences(document: Document) {
    document.select("img[src]").forEach { image ->
        val source = image.attr("src").trim()
        if (!isSafeResourceReference(source)) image.removeAttr("src") else image.attr("src", source)
    }
    document.select("[data-katari-font-resource]").forEach { element ->
        val resourceId = element.attr("data-katari-font-resource").trim()
        if (!isSafeResourceReference(resourceId)) element.removeAttr("data-katari-font-resource")
    }
}

private fun isSafeExternalUrl(value: String): Boolean = runCatching {
    if (value.length > MAX_REFERENCE_LENGTH || value.any(Char::isISOControl)) return@runCatching false
    val uri = URI(value)
    uri.scheme?.lowercase() in setOf("http", "https") &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null
}.getOrDefault(false)

private fun isSafeResourceReference(value: String): Boolean {
    if (value.isBlank() || value.length > MAX_REFERENCE_LENGTH || value.any(Char::isISOControl)) return false
    val scheme = runCatching { URI(value).scheme?.lowercase() }.getOrNull()
    return scheme == null || scheme == "https"
}

private fun Char.isSafeHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private sealed interface SafeCssSelector {
    fun matches(element: Element): Boolean

    data class Tag(val name: String) : SafeCssSelector {
        override fun matches(element: Element): Boolean = element.tagName().equals(name, ignoreCase = true)
    }

    data class Class(val name: String) : SafeCssSelector {
        override fun matches(element: Element): Boolean = element.hasClass(name)
    }

    data class Id(val value: String) : SafeCssSelector {
        override fun matches(element: Element): Boolean = element.id() == value
    }

    companion object {
        fun parse(value: String): SafeCssSelector? = when {
            value.matches(SIMPLE_TAG_SELECTOR) -> Tag(value.lowercase())
            value.matches(SIMPLE_CLASS_SELECTOR) -> Class(value.drop(1))
            value.matches(SIMPLE_ID_SELECTOR) -> Id(value.drop(1))
            else -> null
        }
    }
}

private data class SafeCssRule(
    val selector: SafeCssSelector,
    val declarations: Map<String, String>,
) {
    fun matches(element: Element): Boolean = selector.matches(element)
}

private val PROSE_SAFELIST = Safelist.none()
    .addTags(
        "a", "article", "aside", "b", "blockquote", "br", "caption", "cite", "code", "col", "colgroup", "dd",
        "details", "div", "dl", "dt", "em", "figcaption", "figure", "h1", "h2", "h3", "h4", "h5", "h6",
        "hr", "i", "img", "li", "ol", "p", "pre", "q", "s", "section", "small", "span", "strike", "strong",
        "sub", "summary", "sup", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "u", "ul",
    )
    .addAttributes(
        ":all",
        "id",
        "name",
        "role",
        "aria-label",
        "data-katari-unsupported",
        "data-katari-align",
        "data-katari-white-space",
        "data-katari-color",
        "data-katari-background",
        "data-katari-border",
        "data-katari-padding-em",
        "data-katari-font-resource",
        "data-katari-font-generic",
        "data-katari-font-scale",
        "data-katari-bold",
    )
    .addAttributes("a", "href", "title")
    .addAttributes("details", "open")
    .addAttributes("img", "src", "alt", "title", "width", "height")
    .addAttributes("ol", "start", "type")
    .addAttributes("td", "colspan", "rowspan")
    .addAttributes("th", "colspan", "rowspan", "scope")

private val UNSUPPORTED_TAGS = setOf("audio", "video", "iframe", "object", "embed", "canvas", "svg")
private val SAFE_ALIGNMENTS = setOf("left", "start", "center", "right", "end")
private val SAFE_WHITE_SPACE = setOf("normal", "pre-wrap", "pre")
private val SAFE_BORDER_STYLES = setOf("solid", "dashed", "dotted")
private val SERIF_FAMILIES = setOf("serif", "times", "times new roman", "georgia")
private val SANS_SERIF_FAMILIES = setOf("sans-serif", "arial", "helvetica", "roboto")
private val MONOSPACE_FAMILIES = setOf("monospace", "courier", "courier new")
private val NAMED_COLORS = mapOf(
    "black" to "#ff000000",
    "white" to "#ffffffff",
    "red" to "#ffff0000",
    "green" to "#ff008000",
    "blue" to "#ff0000ff",
    "transparent" to "#00000000",
)
private val FONT_FACE_REGEX = Regex("""(?is)@font-face\s*\{([^}]*)\}""")
private val STYLE_RULE_REGEX = Regex("""(?is)([^{}]+)\{([^{}]*)\}""")
private val URL_FUNCTION_REGEX = Regex("""(?is)url\(\s*(['"]?)([^'")]+)\1\s*\)""")
private val SIMPLE_TAG_SELECTOR = Regex("""[A-Za-z][A-Za-z0-9-]*""")
private val SIMPLE_CLASS_SELECTOR = Regex("""\.[A-Za-z_][A-Za-z0-9_-]*""")
private val SIMPLE_ID_SELECTOR = Regex("""#[A-Za-z_][A-Za-z0-9_-]*""")
private const val MAX_STYLE_RULES = 256
private const val MAX_DECLARATIONS = 64
private const val MAX_REFERENCE_LENGTH = 2_048
private const val MAX_DIAGNOSTIC_LENGTH = 64
