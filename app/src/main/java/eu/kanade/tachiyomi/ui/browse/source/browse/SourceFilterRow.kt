package eu.kanade.tachiyomi.ui.browse.source.browse

import eu.kanade.domain.source.model.FilterRestoreIssue
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationIssue
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.isOrdering

/** One row of the root filter sheet list, planned before composition so the sheet can locate filters by index. */
internal sealed interface SourceFilterRow {
    data object Loading : SourceFilterRow

    data class LoadError(val message: String) : SourceFilterRow

    data class RepairIntro(val showSave: Boolean, val saveEnabled: Boolean) : SourceFilterRow

    data class RepairIssue(val issue: FilterRestoreIssue) : SourceFilterRow

    data class ValidationSummary(val issues: List<EntryFilterValidationIssue>) : SourceFilterRow

    data object NoChanges : SourceFilterRow

    /** Separates the ordering block from the filters that narrow results. */
    data object OrderingDivider : SourceFilterRow

    /**
     * A top-level source filter at [index] in the source's list.
     *
     * [leadingDivider] is false when the row above already separates it: the list start or a source separator.
     */
    class Filter(
        val index: Int,
        val filter: EntryFilter<*>,
        val leadingDivider: Boolean,
        val changedOnlyChildren: Boolean,
    ) : SourceFilterRow
}

internal fun sourceFilterRows(
    filters: EntryFilterList,
    isLoading: Boolean,
    errorMessage: String?,
    validation: List<EntryFilterValidationIssue>,
    repairIssues: List<FilterRestoreIssue>,
    repairNeedsSave: Boolean,
    hasPendingEdits: Boolean,
    changes: FilterChanges,
    changedOnly: Boolean,
): List<SourceFilterRow> = buildList {
    when {
        isLoading -> add(SourceFilterRow.Loading)
        errorMessage != null -> add(SourceFilterRow.LoadError(errorMessage))
        else -> {
            if (repairIssues.isNotEmpty() || repairNeedsSave) {
                add(
                    SourceFilterRow.RepairIntro(
                        showSave = repairIssues.isEmpty(),
                        saveEnabled = !hasPendingEdits && validation.isEmpty(),
                    ),
                )
                repairIssues.forEach { add(SourceFilterRow.RepairIssue(it)) }
            }
            if (validation.isNotEmpty()) add(SourceFilterRow.ValidationSummary(validation))
            val shown = filters.withIndex().filter { (_, filter) ->
                !changedOnly || changes[filter].isChanged || filter.validationIssues().isNotEmpty()
            }
            if (changedOnly && shown.isEmpty()) add(SourceFilterRow.NoChanges)
            // Ordering filters lead the sheet as one block; every other filter keeps the source's order and headers.
            val (ordering, constraints) = shown.partition { it.value.isOrdering }
            var separated = true
            fun addFilter(index: Int, filter: EntryFilter<*>) {
                add(
                    SourceFilterRow.Filter(
                        index = index,
                        filter = filter,
                        leadingDivider = !separated,
                        changedOnlyChildren = changedOnly && !filter.isOrdering,
                    ),
                )
                separated = filter is EntryFilter.Separator
            }
            ordering.forEach { (index, filter) -> addFilter(index, filter) }
            if (ordering.isNotEmpty() && constraints.isNotEmpty() &&
                constraints.first().value !is EntryFilter.Separator
            ) {
                add(SourceFilterRow.OrderingDivider)
                separated = true
            }
            constraints.forEach { (index, filter) -> addFilter(index, filter) }
        }
    }
}
