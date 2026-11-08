package mihon.entry.interactions.reader.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A reader's rule for text that repeats in a book's chapters, such as a site's or translator's notice. Where [find]
 * occurs within a paragraph it is hidden, or replaced by [replaceWith] when that is set. [find] is plain text, where
 * any run of spaces also matches a line break, unless [regex] makes it a regular expression.
 */
@Serializable
data class BookTextRule(
    val id: String,
    val find: String,
    val scope: BookTextRuleScope,
    val regex: Boolean = false,
    val replaceWith: String? = null,
    val enabled: Boolean = true,
) {
    fun appliesTo(sourceId: Long, entryUrl: String): Boolean = enabled && scope.covers(sourceId, entryUrl)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun encodeList(rules: List<BookTextRule>): String = json.encodeToString(rules)

        /** An unreadable value gives no rules rather than failing the reader. */
        fun decodeList(value: String): List<BookTextRule> =
            runCatching { json.decodeFromString<List<BookTextRule>>(value) }.getOrDefault(emptyList())
    }
}

/**
 * What a [BookTextRule] covers, named by source and entry URL rather than database ids so it still holds after a
 * backup is restored on another device. The names are what the rule list shows.
 */
@Serializable
sealed interface BookTextRuleScope {
    val sourceId: Long

    fun covers(sourceId: Long, entryUrl: String): Boolean

    @Serializable
    @SerialName("series")
    data class Series(override val sourceId: Long, val entryUrl: String, val title: String) : BookTextRuleScope {
        override fun covers(sourceId: Long, entryUrl: String) = sourceId == this.sourceId && entryUrl == this.entryUrl
    }

    @Serializable
    @SerialName("source")
    data class Source(override val sourceId: Long, val name: String) : BookTextRuleScope {
        override fun covers(sourceId: Long, entryUrl: String) = sourceId == this.sourceId
    }
}
