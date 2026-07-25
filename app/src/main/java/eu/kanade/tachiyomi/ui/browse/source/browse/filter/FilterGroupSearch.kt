package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import com.aallam.similarity.NormalizedLevenshtein
import eu.kanade.tachiyomi.source.entry.EntryFilter
import java.util.Locale

private const val SEARCHABLE_OPTION_THRESHOLD = 10
private const val MIN_FUZZY_QUERY_LENGTH = 3
private const val MIN_FUZZY_SIMILARITY = 0.7

private val normalizedLevenshtein = NormalizedLevenshtein()
private val nonSearchTextRegex = Regex("[^\\p{L}\\p{N}]+")

internal fun List<EntryFilter<*>>.hasSearchableOptionList(): Boolean {
    return count(EntryFilter<*>::isSearchableOption) > SEARCHABLE_OPTION_THRESHOLD
}

internal fun List<EntryFilter<*>>.filterGroupOptions(query: String): List<EntryFilter<*>> {
    val normalizedQuery = query.normalizeForFilterSearch()
    if (normalizedQuery.isEmpty()) return this

    return filter { filter ->
        !filter.isSearchableOption() || filter.name.matchesFilterQuery(normalizedQuery)
    }
}

private fun EntryFilter<*>.isSearchableOption(): Boolean {
    return this is EntryFilter.CheckBox || this is EntryFilter.TriState
}

private fun String.matchesFilterQuery(normalizedQuery: String): Boolean {
    val normalizedName = normalizeForFilterSearch()
    if (normalizedName.contains(normalizedQuery)) return true

    val compactQuery = normalizedQuery.replace(" ", "")
    val compactName = normalizedName.replace(" ", "")
    if (compactQuery.isNotEmpty() && compactName.contains(compactQuery)) return true

    val queryTokens = normalizedQuery.split(' ')
    val nameTokens = normalizedName.split(' ')
    return queryTokens.all { queryToken ->
        nameTokens.any { nameToken ->
            nameToken.startsWith(queryToken) ||
                (
                    queryToken.length >= MIN_FUZZY_QUERY_LENGTH &&
                        normalizedLevenshtein.similarity(queryToken, nameToken) >= MIN_FUZZY_SIMILARITY
                    )
        }
    }
}

private fun String.normalizeForFilterSearch(): String {
    return lowercase(Locale.ROOT)
        .replace(nonSearchTextRegex, " ")
        .trim()
}
