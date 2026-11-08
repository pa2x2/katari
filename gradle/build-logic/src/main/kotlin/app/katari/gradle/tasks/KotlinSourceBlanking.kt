package app.katari.gradle.tasks

/** Blanks comments and string literal contents while keeping line breaks, so findings keep their line numbers. */
internal fun String.withoutCommentsAndStrings(): String {
    val out = StringBuilder(length)
    var index = 0
    while (index < length) {
        val char = this[index]
        when {
            startsWith("//", index) -> {
                while (index < length && this[index] != '\n') {
                    out.append(' ')
                    index++
                }
            }
            startsWith("/*", index) -> {
                val end = indexOf("*/", index + 2).let { if (it < 0) length else it + 2 }
                while (index < end) {
                    out.append(if (this[index] == '\n') '\n' else ' ')
                    index++
                }
            }
            startsWith("\"\"\"", index) -> {
                val end = indexOf("\"\"\"", index + 3).let { if (it < 0) length else it + 3 }
                while (index < end) {
                    out.append(if (this[index] == '\n') '\n' else ' ')
                    index++
                }
            }
            char == '"' || char == '\'' -> {
                out.append(' ')
                index++
                while (index < length && this[index] != char && this[index] != '\n') {
                    if (this[index] == '\\') {
                        out.append(' ')
                        index++
                    }
                    if (index < length) {
                        out.append(' ')
                        index++
                    }
                }
                if (index < length && this[index] == char) {
                    out.append(' ')
                    index++
                }
            }
            else -> {
                out.append(char)
                index++
            }
        }
    }
    return out.toString()
}
