package mihon.entry.interactions.book.format.html.prosechapter.css

/** One style rule as written: its selector list, its declaration block, and the `@media` it sits in. */
internal class CssParsedRule(val selectors: String, val declarations: String, val media: List<CssMediaQuery>)

/**
 * Splits a style sheet into style rules. `@media`, `@supports` and `@layer` blocks contribute the rules inside
 * them (a `@supports` condition is assumed true, as the page was written for browsers that pass it); `@font-face`
 * is read separately, and other at-rules (`@import`, `@keyframes`, `@page`, …) are skipped.
 */
internal object CssStyleSheetParser {
    fun parse(
        css: String,
    ): List<CssParsedRule> = buildList { parseBlock(Scanner(css), media = emptyList(), into = this) }

    private fun parseBlock(scanner: Scanner, media: List<CssMediaQuery>, into: MutableList<CssParsedRule>) {
        while (true) {
            scanner.skipSpaceAndComments()
            if (scanner.atEnd || scanner.peek() == '}') return
            if (scanner.peek() == '@') {
                val prelude = scanner.until(setOf('{', ';'))
                val name = prelude.substringBefore(' ').substringBefore('(').lowercase()
                if (scanner.atEnd) return
                if (scanner.next() == ';') continue
                when (name) {
                    "@media" -> {
                        val queries = CssMediaQuery.parseList(prelude.trim().drop(name.length))
                        parseBlock(scanner, media + queries, into)
                        scanner.closeBlock()
                    }
                    "@supports", "@layer", "@document", "@container" -> {
                        parseBlock(scanner, media, into)
                        scanner.closeBlock()
                    }
                    else -> scanner.skipBlock()
                }
                continue
            }
            val selectors = scanner.until(setOf('{', '}'))
            if (scanner.atEnd) return
            if (scanner.next() == '}') return
            val declarations = scanner.blockContent()
            if (selectors.isNotBlank()) into += CssParsedRule(selectors.trim(), declarations, media)
        }
    }

    private class Scanner(private val text: String) {
        private var position = 0
        val atEnd: Boolean get() = position >= text.length

        fun peek(): Char = text[position]

        fun next(): Char = text[position++]

        fun skipSpaceAndComments() {
            while (!atEnd) {
                when {
                    text[position].isWhitespace() -> position++
                    text.startsWith("/*", position) -> skipComment()
                    text.startsWith("<!--", position) -> position += 4
                    text.startsWith("-->", position) -> position += 3
                    else -> return
                }
            }
        }

        /** Reads up to (not including) the first top-level character of [stops], dropping comments. */
        fun until(stops: Set<Char>): String {
            val output = StringBuilder()
            var depth = 0
            while (!atEnd) {
                val char = text[position]
                when {
                    text.startsWith("/*", position) -> {
                        skipComment()
                        continue
                    }
                    char == '"' || char == '\'' -> {
                        output.append(quoted())
                        continue
                    }
                    char == '(' || char == '[' -> depth++
                    char == ')' || char == ']' -> depth = (depth - 1).coerceAtLeast(0)
                    depth == 0 && char in stops -> return output.toString()
                }
                output.append(char)
                position++
            }
            return output.toString()
        }

        /** After an opening brace: the block's text up to its matching closing brace, which is consumed. */
        fun blockContent(): String {
            val output = StringBuilder()
            var depth = 1
            while (!atEnd) {
                val char = text[position]
                when {
                    text.startsWith("/*", position) -> {
                        skipComment()
                        continue
                    }
                    char == '"' || char == '\'' -> {
                        output.append(quoted())
                        continue
                    }
                    char == '{' -> depth++
                    char == '}' -> if (--depth == 0) {
                        position++
                        return output.toString()
                    }
                }
                output.append(char)
                position++
            }
            return output.toString()
        }

        fun skipBlock() {
            blockContent()
        }

        fun closeBlock() {
            skipSpaceAndComments()
            if (!atEnd && text[position] == '}') position++
        }

        private fun skipComment() {
            val end = text.indexOf("*/", position + 2)
            position = if (end < 0) text.length else end + 2
        }

        private fun quoted(): String {
            val start = position
            val quote = text[position++]
            while (!atEnd) {
                val char = text[position++]
                if (char == '\\' && !atEnd) {
                    position++
                } else if (char == quote || char == '\n') {
                    break
                }
            }
            return text.substring(start, position)
        }
    }
}

/** The page the reader lays a chapter out on, in CSS pixels. */
internal data class CssViewport(val widthPx: Float, val heightPx: Float)

/**
 * One media query of an `@media` list: an optional `not`, a media type and `and`-joined features. The reader is a
 * screen; width, height, orientation and aspect ratio come from [CssViewport]; unknown features never match.
 */
internal class CssMediaQuery private constructor(
    private val negated: Boolean,
    private val type: String,
    private val features: List<String>,
) {
    fun matches(viewport: CssViewport): Boolean {
        val result = (type == "all" || type == "screen") && features.all { featureMatches(it, viewport) }
        return result != negated
    }

    private fun featureMatches(feature: String, viewport: CssViewport): Boolean {
        val text = feature.trim().removePrefix("(").removeSuffix(")").trim().lowercase()
        RANGE.matchEntire(text)?.let { match ->
            val (name, operator, value) = match.destructured
            val actual = viewport.dimension(name) ?: return false
            val expected = value.toCssPixels() ?: return false
            return compare(actual, operator, expected)
        }
        REVERSED_RANGE.matchEntire(text)?.let { match ->
            val (value, operator, name) = match.destructured
            val actual = viewport.dimension(name) ?: return false
            val expected = value.toCssPixels() ?: return false
            return compare(expected, operator, actual)
        }
        val name = text.substringBefore(':').trim()
        val value = text.substringAfter(':', "").trim()
        return when (name) {
            "min-width" -> value.toCssPixels()?.let { viewport.widthPx >= it } == true
            "max-width" -> value.toCssPixels()?.let { viewport.widthPx <= it } == true
            "width" -> value.toCssPixels()?.let { viewport.widthPx == it } == true
            "min-height" -> value.toCssPixels()?.let { viewport.heightPx >= it } == true
            "max-height" -> value.toCssPixels()?.let { viewport.heightPx <= it } == true
            "orientation" -> value == if (viewport.heightPx >= viewport.widthPx) "portrait" else "landscape"
            "hover" -> value == "none"
            "pointer", "any-pointer" -> value == "coarse"
            "color" -> true
            else -> false
        }
    }

    private fun CssViewport.dimension(name: String): Float? = when (name) {
        "width" -> widthPx
        "height" -> heightPx
        else -> null
    }

    private fun compare(left: Float, operator: String, right: Float): Boolean = when (operator) {
        "<" -> left < right
        "<=" -> left <= right
        ">" -> left > right
        ">=" -> left >= right
        "=" -> left == right
        else -> false
    }

    companion object {
        fun parseList(raw: String): List<CssMediaQuery> {
            val queries = raw.split(',').mapNotNull { query -> parse(query.trim().lowercase()) }
            // A list where nothing parses applies to nothing.
            return queries.ifEmpty { listOf(CssMediaQuery(negated = false, type = "print", features = emptyList())) }
        }

        private fun parse(query: String): CssMediaQuery? {
            if (query.isEmpty()) return CssMediaQuery(false, "all", emptyList())
            var rest = query
            val negated = rest.startsWith("not ")
            rest = rest.removePrefix("not ").removePrefix("only ").trim()
            val type = if (rest.startsWith("(")) "all" else rest.substringBefore(' ')
            rest = if (rest.startsWith("(")) rest else rest.substringAfter(' ', "").trim().removePrefix("and").trim()
            val features = if (rest.isEmpty()) emptyList() else rest.split(Regex("\\)\\s*and\\s*\\(")).map { it.trim() }
            return CssMediaQuery(negated, type, features)
        }

        private val RANGE = Regex("""(width|height)\s*(<=|>=|<|>|=)\s*(.+)""")
        private val REVERSED_RANGE = Regex("""(.+?)\s*(<=|>=|<|>|=)\s*(width|height)""")
    }
}

/** A media-query length in CSS pixels; `em` and `rem` use the 16px default, as browsers do in media queries. */
private fun String.toCssPixels(): Float? {
    val value = trim()
    return when {
        value.endsWith("px") -> value.removeSuffix("px").trim().toFloatOrNull()
        value.endsWith("rem") -> value.removeSuffix("rem").trim().toFloatOrNull()?.times(16f)
        value.endsWith("em") -> value.removeSuffix("em").trim().toFloatOrNull()?.times(16f)
        value == "0" -> 0f
        else -> null
    }
}
