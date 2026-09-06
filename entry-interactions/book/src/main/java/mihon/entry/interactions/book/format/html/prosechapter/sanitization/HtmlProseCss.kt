package mihon.entry.interactions.book.format.html.prosechapter.sanitization

import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import org.jsoup.nodes.Element

internal const val DOCUMENT_STYLE_ATTRIBUTE_PREFIX = "data-katari-style-"

internal class HtmlProseCss private constructor(
    private val tagRules: Map<String, List<Rule>>,
    private val classRules: Map<String, List<Rule>>,
    private val idRules: Map<String, List<Rule>>,
) {
    fun applyTo(element: Element) {
        val matching = buildList {
            addAll(tagRules[element.normalName()].orEmpty())
            element.classNames().forEach { className -> addAll(classRules[className].orEmpty()) }
            element.id().takeIf(String::isNotEmpty)?.let { id -> addAll(idRules[id].orEmpty()) }
        }.sortedWith(compareBy(Rule::specificity, Rule::order))
        val declarations = linkedMapOf<String, String>()
        matching.forEach { declarations.putAll(it.declarations) }
        element.attr("style").takeIf(String::isNotBlank)?.let { inline ->
            declarations.putAll(parseDeclarations(inline))
        }
        declarations.forEach { (property, value) ->
            element.attr("$DOCUMENT_STYLE_ATTRIBUTE_PREFIX$property", value)
        }
    }

    private data class Rule(
        val selector: Selector,
        val declarations: Map<String, String>,
        val order: Int,
    ) {
        val specificity: Int
            get() = when (selector) {
                is Selector.Tag -> 1
                is Selector.Class -> 10
                is Selector.Id -> 100
            }
    }

    private sealed interface Selector {
        val name: String

        data class Tag(override val name: String) : Selector
        data class Class(override val name: String) : Selector
        data class Id(override val name: String) : Selector
    }

    companion object {
        fun parse(styleSheets: List<String>): HtmlProseCss {
            val rules = mutableListOf<Rule>()
            val rulePattern = Regex("([^{}]+)\\{([^{}]*)\\}")
            styleSheets.forEach { css ->
                val withoutComments = css.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
                rulePattern.findAll(withoutComments).forEach { match ->
                    val declarations = parseDeclarations(match.groupValues[2])
                    if (declarations.isEmpty()) return@forEach
                    match.groupValues[1].split(',').forEach { rawSelector ->
                        val selector = parseSelector(rawSelector) ?: return@forEach
                        if (rules.size >= HtmlProseChapterContract.MAX_CSS_RULES) {
                            throw HtmlProseLimitExceededException("HTML contains too many supported CSS rules")
                        }
                        rules += Rule(selector, declarations, rules.size)
                    }
                }
            }
            return HtmlProseCss(
                tagRules = rules.filter { it.selector is Selector.Tag }.groupBy { it.selector.name },
                classRules = rules.filter { it.selector is Selector.Class }.groupBy { it.selector.name },
                idRules = rules.filter { it.selector is Selector.Id }.groupBy { it.selector.name },
            )
        }

        private fun parseSelector(raw: String): Selector? {
            val selector = raw.trim()
            if (!selector.matches(Regex("[#.]?[A-Za-z][A-Za-z0-9_-]{0,127}"))) return null
            return when (selector.first()) {
                '#' -> Selector.Id(selector.drop(1))
                '.' -> Selector.Class(selector.drop(1))
                else -> Selector.Tag(selector.lowercase())
            }
        }
    }
}

private val supportedProperties = setOf(
    "background-color",
    "border-color",
    "border-style",
    "border-width",
    "color",
    "font-family",
    "font-size",
    "font-style",
    "font-weight",
    "line-height",
    "margin-bottom",
    "margin-top",
    "padding",
    "text-align",
    "text-decoration",
    "text-indent",
    "direction",
    "vertical-align",
    "white-space",
)

private fun parseDeclarations(raw: String): Map<String, String> {
    val pieces = raw.split(';')
    if (pieces.size > HtmlProseChapterContract.MAX_CSS_DECLARATIONS_PER_RULE) {
        throw HtmlProseLimitExceededException("A CSS rule contains too many declarations")
    }
    return buildMap {
        pieces.forEach { declaration ->
            val separator = declaration.indexOf(':')
            if (separator <= 0) return@forEach
            val property = declaration.substring(0, separator).trim().lowercase()
            val value = declaration.substring(separator + 1)
                .substringBefore("!important", missingDelimiterValue = declaration.substring(separator + 1))
                .trim()
                .take(256)
            if (property in supportedProperties && value.isNotEmpty()) put(property, value)
        }
    }
}
