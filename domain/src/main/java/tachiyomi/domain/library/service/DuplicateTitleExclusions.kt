package tachiyomi.domain.library.service

import java.util.Locale

object DuplicateTitleExclusions {
    val defaultPatterns: List<String> = listOf("[*]", "(*)", "{*}", "<*>")

    fun normalizePattern(pattern: String): String {
        return pattern.trim()
    }

    fun sanitizePatterns(patterns: List<String>): List<String> {
        val seen = linkedSetOf<String>()
        return patterns.asSequence()
            .map(::normalizePattern)
            .filter(String::isNotBlank)
            .filterNot(::isCatchAllPattern)
            .filter { seen.add(it.lowercase(Locale.ROOT)) }
            .toList()
    }

    fun isCatchAllPattern(pattern: String): Boolean {
        val normalized = normalizePattern(pattern)
        return normalized.isNotEmpty() && normalized.all { it.isWhitespace() || it == '*' }
    }

    fun compilePatterns(patterns: List<String>): List<CompiledPattern> {
        return sanitizePatterns(patterns).map { pattern ->
            CompiledPattern(
                pattern = pattern,
                regex = wildcardToRegex(pattern),
            )
        }
    }

    private fun wildcardToRegex(pattern: String): Regex {
        val builder = StringBuilder()
        pattern.forEachIndexed { index, char ->
            if (char == '*') {
                val hasRemainingLiteral = pattern.substring(index + 1).any { it != '*' }
                builder.append(if (hasRemainingLiteral) ".*?" else ".*")
            } else {
                builder.append(Regex.escape(char.toString()))
            }
        }
        return Regex(builder.toString(), RegexOption.IGNORE_CASE)
    }

    data class CompiledPattern(
        val pattern: String,
        val regex: Regex,
    )
}
