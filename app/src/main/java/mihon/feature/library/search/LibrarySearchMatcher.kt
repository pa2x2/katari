package mihon.feature.library.search

import mihon.domain.library.model.search.AndNode
import mihon.domain.library.model.search.ComparisonField
import mihon.domain.library.model.search.ComparisonQueryNode
import mihon.domain.library.model.search.EmptyQueryNode
import mihon.domain.library.model.search.EntryField
import mihon.domain.library.model.search.ExactEntryIdQueryNode
import mihon.domain.library.model.search.ExactSourceQueryNode
import mihon.domain.library.model.search.FieldQueryNode
import mihon.domain.library.model.search.GeneralQueryNode
import mihon.domain.library.model.search.NotNode
import mihon.domain.library.model.search.OrNode
import mihon.domain.library.model.search.QueryNode
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.source.local.LocalSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

internal class LibrarySearchMatcher(
    query: String,
    private val categoryNamesById: Map<Long, String>,
    private val sourceDisplayName: (LibraryItem) -> String = LibraryItem::sourceName,
    private val sourceNames: (LibraryItem) -> List<String> = { listOf(it.sourceName) },
) {
    private val root = QueryNode.from(query)

    fun matches(item: LibraryItem): Boolean = root.matches(item)

    private fun QueryNode.matches(item: LibraryItem): Boolean {
        return when (this) {
            is AndNode -> children.all { it.matches(item) }
            is OrNode -> children.any { it.matches(item) }
            is NotNode -> !child.matches(item)
            is EmptyQueryNode -> true
            is GeneralQueryNode -> matches(item)
            is ExactEntryIdQueryNode -> item.entry.id == value.toLongOrNull()
            is ExactSourceQueryNode -> matchesExactSource(item)
            is FieldQueryNode -> matches(item)
            is ComparisonQueryNode -> matches(item)
        }
    }

    private fun GeneralQueryNode.matches(item: LibraryItem): Boolean {
        val entry = item.entry
        val categoryNames = item.categories.mapNotNull(categoryNamesById::get)
        val match = listOfNotNull(
            entry.title,
            entry.displayName,
            entry.author,
            entry.artist,
            entry.description,
            sourceDisplayName(item),
            *entry.genre.orEmpty().toTypedArray(),
            *categoryNames.toTypedArray(),
        ).any { it.contains(value, ignoreCase = true) } ||
            entry.genre.orEmpty().any { genre -> genre.equals(value, ignoreCase = true) }
        return if (negated) !match else match
    }

    private fun ExactSourceQueryNode.matchesExactSource(item: LibraryItem): Boolean {
        return when {
            value.equals(LOCAL_SOURCE_ID_ALIAS, ignoreCase = true) -> item.displaySourceId == LocalSource.ID
            value.equals(MULTI_SOURCE_ID_ALIAS, ignoreCase = true) ->
                item.displaySourceId == LibraryItem.MULTI_SOURCE_ID
            else -> value.toLongOrNull() in item.sourceIds
        }
    }

    private fun FieldQueryNode.matches(item: LibraryItem): Boolean {
        val entry = item.entry
        val match = when (field) {
            EntryField.TITLE -> matchText(listOf(entry.title, entry.displayName))
            EntryField.AUTHOR -> matchText(listOf(entry.author))
            EntryField.ARTIST -> matchText(listOf(entry.artist))
            EntryField.DESCRIPTION -> matchText(listOf(entry.description))
            EntryField.GENRE -> matchText(entry.genre.orEmpty())
            EntryField.NOTES -> matchText(listOf(entry.notes))
            EntryField.LANGUAGE -> matchText(listOf(item.sourceLanguage))
            EntryField.SOURCE -> {
                matchText(sourceNames(item)) ||
                    (value.equals(LOCAL_SOURCE_ID_ALIAS, ignoreCase = true) && LocalSource.ID in item.sourceIds) ||
                    (
                        value.equals(MULTI_SOURCE_ID_ALIAS, ignoreCase = true) &&
                            item.displaySourceId == LibraryItem.MULTI_SOURCE_ID
                        )
            }
            EntryField.SOURCE_ID -> value.toLongOrNull() in item.sourceIds
        }

        return if (negated) !match else match
    }

    private fun FieldQueryNode.matchText(values: List<String?>): Boolean {
        return if (value.isEmpty()) {
            values.all { it.isNullOrEmpty() }
        } else {
            values.any { it?.contains(value, ignoreCase = true) == true }
        }
    }

    private fun ComparisonQueryNode.matches(item: LibraryItem): Boolean {
        val entry = item.entry

        fun compareDates(timestamp: Long): Boolean? {
            val inputDate = runCatching { LocalDate.parse(value) }.getOrNull() ?: return null
            val entryDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            return queryComparator.apply(entryDate, inputDate)
        }

        val match = when (field) {
            ComparisonField.ID -> value.toLongOrNull()?.let { queryComparator.apply(entry.id, it) }
            ComparisonField.DATE_ADDED -> compareDates(entry.dateAdded)
            ComparisonField.FETCH_INTERVAL -> value.toIntOrNull()
                ?.let { queryComparator.apply(abs(entry.fetchInterval), it) }
            ComparisonField.NEXT_UPDATE -> compareDates(entry.nextUpdate)
            ComparisonField.UNREAD -> item.unconsumedCount?.let { count ->
                value.toLongOrNull()?.let { queryComparator.apply(count, it) }
            }
            ComparisonField.READ -> item.consumedCount?.let { count ->
                value.toLongOrNull()?.let { queryComparator.apply(count, it) }
            }
            ComparisonField.TOTAL -> item.totalCount?.let { count ->
                value.toLongOrNull()?.let { queryComparator.apply(count, it) }
            }
        } ?: false

        return if (negated) !match else match
    }

    private companion object {
        const val LOCAL_SOURCE_ID_ALIAS = "local"
        const val MULTI_SOURCE_ID_ALIAS = "multi"
    }
}
