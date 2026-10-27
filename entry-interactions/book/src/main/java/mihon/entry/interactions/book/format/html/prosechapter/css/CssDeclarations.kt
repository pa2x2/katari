package mihon.entry.interactions.book.format.html.prosechapter.css

import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException

/** One declaration as written; custom properties (`--name`) keep their case and raw value. */
internal class CssDeclaration(val property: String, val value: String, val important: Boolean)

internal object CssDeclarations {
    fun parse(block: String): List<CssDeclaration> {
        val pieces = block.splitTopLevel(';')
        if (pieces.size > HtmlProseChapterContract.MAX_CSS_DECLARATIONS_PER_RULE) {
            throw HtmlProseLimitExceededException("A CSS rule contains too many declarations")
        }
        return pieces.mapNotNull { piece ->
            val separator = piece.indexOf(':')
            if (separator <= 0) return@mapNotNull null
            val name = piece.substring(0, separator).trim()
            var value = piece.substring(separator + 1).trim()
            val important = IMPORTANT.find(value)?.also { value = value.substring(0, it.range.first).trim() } != null
            val property = if (name.startsWith("--")) name else name.lowercase()
            if (property.isEmpty() || value.isEmpty() || value.length > MAX_VALUE_LENGTH) return@mapNotNull null
            CssDeclaration(property, value, important)
        }
    }

    private val IMPORTANT = Regex("""!\s*important\s*$""", RegexOption.IGNORE_CASE)

    // Long enough for a gradient with many stops or a font stack; longer values are hostile or generated.
    private const val MAX_VALUE_LENGTH = 1_024
}

/** Splits at top-level [separator]s, outside quotes, parentheses and brackets. */
internal fun String.splitTopLevel(separator: Char): List<String> {
    val parts = mutableListOf<String>()
    var depth = 0
    var quote: Char? = null
    var start = 0
    forEachIndexed { index, char ->
        when {
            quote != null -> if (char == quote) quote = null
            char == '"' || char == '\'' -> quote = char
            char == '(' || char == '[' -> depth++
            char == ')' || char == ']' -> depth = (depth - 1).coerceAtLeast(0)
            char == separator && depth == 0 -> {
                parts += substring(start, index)
                start = index + 1
            }
        }
    }
    parts += substring(start)
    return parts
}

/** Splits a value into its space-separated components, keeping functions such as `rgb(0 0 0)` whole. */
internal fun String.cssComponents(): List<String> {
    val parts = mutableListOf<String>()
    var depth = 0
    var quote: Char? = null
    val current = StringBuilder()
    for (char in this) {
        when {
            quote != null -> {
                current.append(char)
                if (char == quote) quote = null
            }
            char == '"' || char == '\'' -> {
                quote = char
                current.append(char)
            }
            char == '(' -> {
                depth++
                current.append(char)
            }
            char == ')' -> {
                depth = (depth - 1).coerceAtLeast(0)
                current.append(char)
            }
            char.isWhitespace() && depth == 0 -> if (current.isNotEmpty()) {
                parts += current.toString()
                current.clear()
            }
            else -> current.append(char)
        }
    }
    if (current.isNotEmpty()) parts += current.toString()
    return parts
}
