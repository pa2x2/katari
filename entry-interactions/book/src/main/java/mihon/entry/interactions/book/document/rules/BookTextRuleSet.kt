package mihon.entry.interactions.book.document.rules

import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentTextEdit
import mihon.book.api.document.withTextBlockEdits
import mihon.entry.interactions.reader.settings.BookTextRule

/**
 * The enabled text rules for one series, ready to run over its chapters' paragraphs. A regular expression that doesn't
 * compile is passed over, so one bad rule leaves the others working.
 */
internal class BookTextRuleSet private constructor(private val rules: List<Pair<Regex, String>>) {

    fun apply(document: BookDocument): BookDocument = document.withTextBlockEdits { _, text -> edits(text) }

    /** Where rules match in [text], earliest first; where two overlap, the one starting first wins. */
    fun edits(text: String): List<BookDocumentTextEdit> {
        val found = rules.flatMap { (pattern, replacement) ->
            pattern.findAll(text)
                .filter { it.range.last >= it.range.first }
                .map { BookDocumentTextEdit(it.range.first, it.range.last + 1, replacement) }
        }.sortedWith(compareBy<BookDocumentTextEdit> { it.start }.thenByDescending { it.endExclusive })
        return buildList {
            found.forEach { edit -> if (edit.start >= (lastOrNull()?.endExclusive ?: 0)) add(edit) }
        }
    }

    companion object {
        /** Null when no rule covers the series, so its chapters are left as they are. */
        fun forSeries(rules: List<BookTextRule>, sourceId: Long, entryUrl: String): BookTextRuleSet? {
            val compiled = rules.filter { it.appliesTo(sourceId, entryUrl) && it.find.isNotBlank() }
                .mapNotNull { rule -> rule.pattern()?.let { it to rule.replaceWith.orEmpty() } }
            return compiled.takeIf { it.isNotEmpty() }?.let(::BookTextRuleSet)
        }

        private fun BookTextRule.pattern(): Regex? = if (regex) {
            runCatching { Regex(find) }.getOrNull()
        } else {
            // Selected text comes back with its line breaks as they show, so any spacing matches any other.
            Regex(find.trim().split(WHITESPACE).joinToString("\\s+") { Regex.escape(it) })
        }

        private val WHITESPACE = Regex("\\s+")
    }
}
