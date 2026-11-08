package app.katari.gradle.preferencescope

/** A key or key prefix one of sync's `PreferenceScopeTable`s marks person or device. */
internal class PreferenceScopeMark(
    val relativePath: String,
    val lineNumber: Int,
    val value: String,
    val isPrefix: Boolean,
)

/** The marks in the sources' `PreferenceScopeTable { … }` declarations, and anything in them that isn't a literal. */
internal class PreferenceScopeMarks(val marks: List<PreferenceScopeMark>, val malformed: List<PreferenceScopeFinding>)

internal fun preferenceScopeMarks(files: List<KotlinSourceText>): PreferenceScopeMarks {
    val marks = mutableListOf<PreferenceScopeMark>()
    val malformed = mutableListOf<PreferenceScopeFinding>()
    files.filter { TABLE.containsMatchIn(it.codeOutsideStrings) }.forEach { file ->
        MARK_CALL.findAll(file.codeOutsideStrings).forEach call@{ call ->
            if (FUNCTION_BEFORE.containsMatchIn(file.codeOutsideStrings.substring(0, call.range.first))) return@call
            val line = file.lineOf(call.range.first)
            val arguments = file.argumentsFrom(call.range.last + 1)?.values.orEmpty()
            arguments.forEach { argument ->
                val literal = LITERAL.matchEntire(argument)?.groupValues?.get(1)
                if (literal == null) {
                    malformed += PreferenceScopeFinding(
                        file.relativePath,
                        line,
                        "$argument is not a string literal; the build reads marks from the source, so write the key",
                    )
                } else {
                    val isPrefix = call.groupValues[1].endsWith("Prefix")
                    marks += PreferenceScopeMark(file.relativePath, line, literal, isPrefix)
                }
            }
        }
    }
    return PreferenceScopeMarks(marks, malformed)
}

private val TABLE = Regex("""\bPreferenceScopeTable\s*\{""")
private val MARK_CALL = Regex("""(?<![\w.])(person|device|personPrefix|devicePrefix)\s*\(""")
private val FUNCTION_BEFORE = Regex("""\bfun\s+$""")
private val LITERAL = Regex(""""([^"\\$]*)"""")
