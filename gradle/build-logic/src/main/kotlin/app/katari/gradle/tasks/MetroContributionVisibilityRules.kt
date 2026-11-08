package app.katari.gradle.tasks

/*
 * Metro adds a contribution (@ContributesTo, @ContributesBinding, @ContributesIntoSet, @ContributesIntoMap, …) to a
 * graph in another module only when the contributing declaration is itself public: an internal or private one is
 * skipped without a warning, and the app fails at run time on the missing binding ("Unknown model class" for a
 * ViewModel). The graphs live in the app and desktop modules, so every contribution elsewhere must be declared public.
 * A public declaration nested in an internal one still reaches the graphs, so only the declaration's own modifiers
 * count.
 */

internal data class MetroContributionSource(val relativePath: String, val content: String)

internal data class MetroContributionFinding(val relativePath: String, val lineNumber: Int, val reason: String)

internal fun checkMetroContributionVisibility(sources: List<MetroContributionSource>): List<MetroContributionFinding> {
    return sources
        .filter { source -> source.relativePath.isContributingModuleSource() }
        .flatMap { source ->
            hiddenContributions(source.content.withoutCommentsAndStrings()).map { contribution ->
                MetroContributionFinding(
                    relativePath = source.relativePath,
                    lineNumber = contribution.lineNumber,
                    reason = "${contribution.name} carries @${contribution.annotation} but is not public, so Metro " +
                        "leaves it out of the app's graphs; make it public",
                )
            }
        }
}

private fun String.isContributingModuleSource(): Boolean {
    if (!endsWith(".kt")) return false
    val module = substringBefore("/src/", missingDelimiterValue = "")
    val sourceSet = substringAfter("/src/", missingDelimiterValue = "").substringBefore("/")
    return module.isNotEmpty() &&
        module !in GRAPH_MODULES &&
        !module.startsWith("gradle/") &&
        !sourceSet.contains("test", ignoreCase = true)
}

private val GRAPH_MODULES = setOf("app", "desktop")

private class HiddenContribution(val name: String, val annotation: String, val lineNumber: Int)

/** Finds the declarations in [code], already blanked of comments and strings, that contribute but aren't public. */
private fun hiddenContributions(code: String): List<HiddenContribution> {
    val tokens = code.metroTokens()
    val found = mutableListOf<HiddenContribution>()
    var parenDepth = 0
    val modifiers = mutableSetOf<String>()
    val annotations = mutableListOf<String>()
    var index = 0
    while (index < tokens.size) {
        val token = tokens[index]
        when {
            token.text == "@" -> {
                index = tokens.skipAnnotation(index, annotations)
                continue
            }
            token.text in MODIFIERS -> modifiers += token.text
            // Inside parentheses `val` declares a constructor property, and `object` or `fun` an expression.
            token.text in DECLARATION_KEYWORDS && parenDepth == 0 && tokens.getOrNull(index - 1)?.text != "::" -> {
                val keyword = if (token.text == "fun" && tokens.getOrNull(index + 1)?.text == "interface") {
                    index++
                    "interface"
                } else {
                    token.text
                }
                val name = tokens.getOrNull(index + 1)?.takeIf { it.isIdentifier }?.text
                    ?: if (keyword == "object") "companion object" else keyword
                val contribution = annotations.firstOrNull { it.startsWith("Contributes") }
                if (contribution != null && modifiers.any { it in HIDING_MODIFIERS }) {
                    found += HiddenContribution(name, contribution, token.lineNumber)
                }
                modifiers.clear()
                annotations.clear()
            }
            else -> {
                when (token.text) {
                    "(" -> parenDepth++
                    ")" -> parenDepth--
                }
                modifiers.clear()
                annotations.clear()
            }
        }
        index++
    }
    return found
}

/** Records the name of the annotation starting at [start] and returns the index after it, its arguments included. */
private fun List<MetroToken>.skipAnnotation(start: Int, annotations: MutableList<String>): Int {
    var index = start + 1
    // A qualified name (`@dev.zacsweers.metro.ContributesTo`) or a use-site target (`@field:Named`) is named by its
    // last part.
    while (getOrNull(index)?.isIdentifier == true) {
        val part = this[index].text
        index++
        val separator = getOrNull(index)?.text
        if ((separator == "." || separator == ":") && getOrNull(index + 1)?.isIdentifier == true) {
            index++
        } else {
            annotations += part
            break
        }
    }
    if (getOrNull(index)?.text == "(") {
        var depth = 0
        do {
            when (this[index].text) {
                "(" -> depth++
                ")" -> depth--
            }
            index++
        } while (depth > 0 && index < size)
    }
    return index
}

private class MetroToken(val text: String, val lineNumber: Int) {
    val isIdentifier: Boolean get() = text.first().isJavaIdentifierStart()
}

private fun String.metroTokens(): List<MetroToken> {
    val tokens = mutableListOf<MetroToken>()
    var line = 1
    var index = 0
    while (index < length) {
        val char = this[index]
        when {
            char == '\n' -> {
                line++
                index++
            }
            char.isWhitespace() -> index++
            char.isJavaIdentifierStart() -> {
                val end = (index until length).firstOrNull { !this[it].isJavaIdentifierPart() } ?: length
                tokens += MetroToken(substring(index, end), line)
                index = end
            }
            char == '`' -> {
                val end = indexOf('`', index + 1).let { if (it < 0) length else it + 1 }
                tokens += MetroToken(substring(index, end).trim('`').ifEmpty { "_" }, line)
                index = end
            }
            startsWith("::", index) -> {
                tokens += MetroToken("::", line)
                index += 2
            }
            else -> {
                tokens += MetroToken(char.toString(), line)
                index++
            }
        }
    }
    return tokens
}

private val DECLARATION_KEYWORDS = setOf("class", "interface", "object", "fun", "val", "var", "typealias")

private val HIDING_MODIFIERS = setOf("private", "internal", "protected")

private val MODIFIERS = HIDING_MODIFIERS + setOf(
    "public", "abstract", "open", "final", "sealed", "data", "inner", "enum", "annotation", "value", "inline",
    "override", "suspend", "operator", "infix", "tailrec", "external", "const", "lateinit", "expect", "actual",
    "companion",
)
