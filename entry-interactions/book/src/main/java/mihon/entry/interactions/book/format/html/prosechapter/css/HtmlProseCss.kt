package mihon.entry.interactions.book.format.html.prosechapter.css

import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.IdentityHashMap

internal const val DOCUMENT_STYLE_ATTRIBUTE_PREFIX = "data-katari-style-"

/**
 * The cascade over a chapter's style sheets: which declarations win on each element, by `!important`, origin
 * (style sheet or `style` attribute), specificity and order, with custom properties inherited and `var()`
 * resolved. Each element gets its winning longhands as [DOCUMENT_STYLE_ATTRIBUTE_PREFIX] attributes, which the
 * document parser reads; inheritance of ordinary properties is the parser's job.
 *
 * Styles are computed parents first and kept for the cascade's lifetime, so one instance serves one document.
 */
internal class HtmlProseCss private constructor(
    private val rulesById: Map<String, List<Rule>>,
    private val rulesByClass: Map<String, List<Rule>>,
    private val rulesByTag: Map<String, List<Rule>>,
    private val universalRules: List<Rule>,
) {
    private val computed = IdentityHashMap<Element, ElementStyle>()

    fun applyTo(element: Element) {
        style(element).longhands.forEach { (property, value) ->
            element.attr("$DOCUMENT_STYLE_ATTRIBUTE_PREFIX$property", value)
        }
    }

    // Ancestors outside the visited subtree, such as <html> for :root variables, are computed on the way.
    private fun style(element: Element): ElementStyle = computed[element] ?: compute(element)

    private fun compute(element: Element): ElementStyle {
        val parent = element.parent()?.takeUnless { it is Document }?.let(::style)
        val candidates = buildList {
            element.id().takeIf(String::isNotEmpty)?.let { addAll(rulesById[it].orEmpty()) }
            element.classNames().forEach { addAll(rulesByClass[it].orEmpty()) }
            addAll(rulesByTag[element.normalName()].orEmpty())
            addAll(universalRules)
        }.distinct().filter { it.selector.matches(element) }

        val inline = element.attr("style").takeIf(String::isNotBlank)?.let(CssDeclarations::parse).orEmpty()
        val ordered = buildList {
            candidates.sortedWith(compareBy(Rule::specificity, Rule::order)).forEach { rule ->
                rule.declarations.forEach { add(Cascaded(it, important = it.important)) }
            }
            inline.forEach { add(Cascaded(it, important = it.important)) }
        }.sortedBy { if (it.important) 1 else 0 } // stable: keeps origin, specificity and order within each level

        val custom = LinkedHashMap(parent?.custom.orEmpty())
        ordered.filter { it.declaration.property.startsWith("--") }.forEach { cascaded ->
            custom[cascaded.declaration.property] = cascaded.declaration.value
        }
        custom.replaceAll { _, value -> resolveVariables(value, custom, depth = 0) ?: "" }

        val longhands = LinkedHashMap<String, String>()
        ordered.forEach { cascaded ->
            val property = cascaded.declaration.property
            if (property.startsWith("--")) return@forEach
            val value = resolveVariables(cascaded.declaration.value, custom, depth = 0) ?: return@forEach
            CssShorthands.expand(property, value).forEach { (longhand, longhandValue) ->
                when (longhandValue.trim().lowercase()) {
                    "inherit" -> parent?.longhands?.get(longhand)?.let { longhands[longhand] = it }
                        ?: longhands.remove(longhand)
                    "initial", "unset", "revert", "revert-layer" -> longhands.remove(longhand)
                    else -> longhands[longhand] = longhandValue
                }
            }
        }
        return ElementStyle(custom, longhands).also { computed[element] = it }
    }

    // A var() that can't resolve makes the whole declaration invalid, as if it weren't there.
    private fun resolveVariables(value: String, custom: Map<String, String>, depth: Int): String? {
        if (!value.contains("var(", ignoreCase = true)) return value
        if (depth > MAX_VARIABLE_DEPTH) return null
        val output = StringBuilder()
        var index = 0
        while (index < value.length) {
            val start = value.indexOf("var(", index, ignoreCase = true)
            if (start < 0) {
                output.append(value, index, value.length)
                break
            }
            output.append(value, index, start)
            var depthCount = 1
            var end = start + 4
            while (end < value.length && depthCount > 0) {
                when (value[end]) {
                    '(' -> depthCount++
                    ')' -> depthCount--
                }
                end++
            }
            if (depthCount != 0) return null
            val inner = value.substring(start + 4, end - 1)
            val name = inner.substringBefore(',').trim()
            val fallback = if (',' in inner) inner.substringAfter(',').trim() else null
            val replacement = custom[name]?.takeIf(String::isNotEmpty)
                ?: fallback?.let { resolveVariables(it, custom, depth + 1) }
                ?: return null
            output.append(resolveVariables(replacement, custom, depth + 1) ?: return null)
            index = end
        }
        return output.toString()
    }

    private class Rule(val selector: CssSelector, val declarations: List<CssDeclaration>, val order: Int) {
        val specificity: CssSpecificity get() = selector.specificity
    }

    private class Cascaded(val declaration: CssDeclaration, val important: Boolean)

    private class ElementStyle(val custom: Map<String, String>, val longhands: Map<String, String>)

    companion object {
        /**
         * Parses [bookStyleSheets] and then the document's [styleSheets], so the document wins ties. Each group has
         * its own rule budget, so a book-wide sheet can't push a chapter's own styles over the limit.
         */
        fun parse(
            styleSheets: List<String>,
            viewport: CssViewport,
            bookStyleSheets: List<String> = emptyList(),
        ): HtmlProseCss {
            val rules = mutableListOf<Rule>()
            fun addRules(sheets: List<String>, limitMessage: String) {
                var count = 0
                sheets.forEach { css ->
                    CssStyleSheetParser.parse(css).forEach { parsed ->
                        if (parsed.media.isNotEmpty() && parsed.media.none { it.matches(viewport) }) return@forEach
                        val declarations = CssDeclarations.parse(parsed.declarations)
                        if (declarations.isEmpty()) return@forEach
                        parsed.selectors.splitTopLevel(',').forEach { raw ->
                            val selector = CssSelector.parse(raw) ?: return@forEach
                            if (count++ >= HtmlProseChapterContract.MAX_CSS_RULES) {
                                throw HtmlProseLimitExceededException(limitMessage)
                            }
                            rules += Rule(selector, declarations, rules.size)
                        }
                    }
                }
            }
            addRules(bookStyleSheets, "The book's style sheet contains too many supported CSS rules")
            addRules(styleSheets, "HTML contains too many supported CSS rules")
            val byId = mutableMapOf<String, MutableList<Rule>>()
            val byClass = mutableMapOf<String, MutableList<Rule>>()
            val byTag = mutableMapOf<String, MutableList<Rule>>()
            val universal = mutableListOf<Rule>()
            rules.forEach { rule ->
                val subject = rule.selector.subject
                when {
                    subject.id != null -> byId.getOrPut(subject.id) { mutableListOf() } += rule
                    subject.classes.isNotEmpty() -> byClass.getOrPut(subject.classes.first()) { mutableListOf() } +=
                        rule
                    subject.tag != null -> byTag.getOrPut(subject.tag) { mutableListOf() } += rule
                    else -> universal += rule
                }
            }
            return HtmlProseCss(byId, byClass, byTag, universal)
        }

        private const val MAX_VARIABLE_DEPTH = 16
    }
}
