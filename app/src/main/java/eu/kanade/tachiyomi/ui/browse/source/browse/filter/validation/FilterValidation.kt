package eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationIssue
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.metadata

/** The draft's validation issues and the fields they name, so each named field can mark itself. */
@Immutable
internal class FilterValidation(val issues: List<EntryFilterValidationIssue>) {
    private val namedIds = issues.flatMapTo(HashSet()) { it.filterIds }

    val isValid: Boolean get() = issues.isEmpty()

    /** Whether an issue names [filter], or [filter] reports an issue of its own. */
    fun isInvalid(filter: EntryFilter<*>): Boolean =
        filter.metadata?.id?.let { it in namedIds } == true ||
            (filter !is EntryFilter.Group<*> && filter.validationIssues().isNotEmpty())

    /** Index of the first top-level filter that has or contains an issue. */
    fun firstInvalidIndex(filters: List<EntryFilter<*>>): Int? =
        filters.indexOfFirst { it.validationIssues().isNotEmpty() }.takeIf { it >= 0 }

    companion object {
        val Valid = FilterValidation(emptyList())
    }
}

internal val LocalFilterValidation = compositionLocalOf { FilterValidation.Valid }
