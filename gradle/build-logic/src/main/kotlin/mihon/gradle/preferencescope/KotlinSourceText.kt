package mihon.gradle.preferencescope

/**
 * A Kotlin file read the way the preference scope check needs it: [code] has its comments blanked, and
 * [codeOutsideStrings] its string and character literals too, both keeping every offset and line break, so a match in
 * one is read from the other at the same place.
 */
internal class KotlinSourceText(val relativePath: String, content: String) {
    val code: String
    val codeOutsideStrings: String

    init {
        val withoutComments = StringBuilder(content.length)
        val withoutStrings = StringBuilder(content.length)
        var index = 0
        fun keep(count: Int, inString: Boolean) {
            repeat(count) {
                val char = content[index]
                withoutComments.append(char)
                withoutStrings.append(if (inString && char != '\n') ' ' else char)
                index++
            }
        }
        fun blank(until: Int) {
            while (index < until) {
                val char = if (content[index] == '\n') '\n' else ' '
                withoutComments.append(char)
                withoutStrings.append(char)
                index++
            }
        }
        while (index < content.length) {
            when {
                content.startsWith("//", index) -> blank(content.indexOf('\n', index).orEnd(content))
                content.startsWith("/*", index) -> blank(content.indexOf("*/", index + 2).orEnd(content, past = 2))
                content.startsWith("\"\"\"", index) -> {
                    keep(content.indexOf("\"\"\"", index + 3).orEnd(content, past = 3) - index, inString = true)
                }
                content[index] == '"' || content[index] == '\'' ->
                    keep(literalEnd(content, index) - index, inString = true)
                else -> keep(1, inString = false)
            }
        }
        code = withoutComments.toString()
        codeOutsideStrings = withoutStrings.toString()
    }

    fun lineOf(offset: Int): Int = code.substring(0, offset.coerceAtMost(code.length)).count { it == '\n' } + 1

    /**
     * The arguments of the call whose opening parenthesis is just before [start], each trimmed, with the offset just
     * past the closing parenthesis; null when the parentheses never close.
     */
    fun argumentsFrom(start: Int): Arguments? {
        val arguments = mutableListOf<String>()
        var depth = 0
        var argumentStart = start
        var index = start
        while (index < code.length) {
            val char = codeOutsideStrings[index]
            when {
                char == '(' || char == '[' || char == '{' -> depth++
                char == ')' || char == ']' || char == '}' -> {
                    if (depth == 0) {
                        // A trailing comma leaves nothing after it
                        val last = code.substring(argumentStart, index).trim()
                        if (last.isNotEmpty()) arguments += last
                        return Arguments(arguments, index + 1)
                    }
                    depth--
                }
                char == ',' && depth == 0 -> {
                    arguments += code.substring(argumentStart, index).trim()
                    argumentStart = index + 1
                }
            }
            index++
        }
        return null
    }

    class Arguments(val values: List<String>, val end: Int)
}

/** This offset moved [past] what was found at it, or the end of [content] when nothing was found. */
private fun Int.orEnd(content: String, past: Int = 0): Int = if (this < 0) content.length else this + past

/** The offset just past the string or character literal opening at [start], `${…}` templates included. */
private fun literalEnd(content: String, start: Int): Int {
    val quote = content[start]
    var index = start + 1
    while (index < content.length) {
        val char = content[index]
        when {
            char == '\\' -> index += 2
            char == quote -> return index + 1
            char == '\n' -> return index
            quote == '"' && content.startsWith("\${", index) -> {
                var depth = 1
                index += 2
                while (index < content.length && depth > 0) {
                    when (content[index]) {
                        '{' -> depth++
                        '}' -> depth--
                        '"' -> index = literalEnd(content, index) - 1
                    }
                    index++
                }
            }
            else -> index++
        }
    }
    return index
}
