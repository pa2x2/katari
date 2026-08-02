package mihon.feature.migration.discovery

import mihon.feature.migration.discovery.model.SourceMigrationSearchDepth
import java.util.Locale

internal class SourceMigrationSearchQueryFactory {

    fun queries(title: String, depth: SourceMigrationSearchDepth): List<String> {
        if (depth == SourceMigrationSearchDepth.STANDARD) return listOf(title.trim())

        val normalized = normalize(title)
        val words = normalized.split(' ').filter(String::isNotBlank)
        if (words.isEmpty()) return listOf(title.trim())

        val longestWords = words.sortedByDescending(String::length)
        return listOf(
            normalized,
            longestWords.take(2).joinToString(" "),
            longestWords.first(),
            words.take(2).joinToString(" "),
            words.first(),
            title.trim(),
        ).filter(String::isNotBlank).distinct()
    }

    fun normalize(title: String): String {
        return title
            .lowercase(Locale.ROOT)
            .replace(BRACKETED_TEXT, " ")
            .replace(NON_LETTER_OR_NUMBER, " ")
            .trim()
            .replace(CONSECUTIVE_WHITESPACE, " ")
    }

    private companion object {
        val BRACKETED_TEXT = Regex("[({<\\[].*?[)}>\\]]")
        val NON_LETTER_OR_NUMBER = Regex("[^\\p{L}\\p{N}]+")
        val CONSECUTIVE_WHITESPACE = Regex("\\s+")
    }
}
