package eu.kanade.tachiyomi.ui.browse.source.browse

import eu.kanade.domain.source.model.FilterRestoreIssue
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationIssue
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.group.isSearchable
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.isOrdering
import mihon.presentation.kit.list.RowGroupPosition

/** One row of the root filter sheet list, planned before composition so the sheet can locate filters by index. */
internal sealed interface SourceFilterRow {
    data object Loading : SourceFilterRow

    data class LoadError(val message: String) : SourceFilterRow

    data class RepairIntro(val showSave: Boolean, val saveEnabled: Boolean) : SourceFilterRow

    data class RepairIssue(val issue: FilterRestoreIssue) : SourceFilterRow

    data object NoChanges : SourceFilterRow

    /** Separates the ordering block from the filters that narrow results. */
    data object OrderingDivider : SourceFilterRow

    /** A top-level source filter at [index] in the source's list. */
    class Filter(
        val index: Int,
        val filter: EntryFilter<*>,
        val changedOnlyChildren: Boolean,
    ) : SourceFilterRow

    /** The option search of the open, searchable top-level group at [index], pinned while its options scroll. */
    class GroupTools(val index: Int, val group: EntryFilter.Group<*>, val changedOnly: Boolean) : SourceFilterRow

    /** The options of the open top-level group at [index]. */
    class GroupBody(val index: Int, val group: EntryFilter.Group<*>, val changedOnly: Boolean) : SourceFilterRow

    /** Ends the pinned tools of the group at [index] so they scroll away with the group's last option. */
    data class GroupEnd(val index: Int) : SourceFilterRow
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
    isGroupExpanded: (Int) -> Boolean,
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
            val shown = filters.withIndex().filter { (_, filter) ->
                !changedOnly || changes[filter].isChanged || filter.validationIssues().isNotEmpty()
            }
            if (changedOnly && shown.isEmpty()) add(SourceFilterRow.NoChanges)
            // Ordering filters lead the sheet as one block; every other filter keeps the source's order and headers.
            val (ordering, constraints) = shown.partition { it.value.isOrdering }
            fun addFilter(index: Int, filter: EntryFilter<*>) {
                add(
                    SourceFilterRow.Filter(
                        index = index,
                        filter = filter,
                        changedOnlyChildren = changedOnly && !filter.isOrdering,
                    ),
                )
                if (filter is EntryFilter.Group<*> && isGroupExpanded(index)) {
                    val searchable = filter.isSearchable()
                    val groupChangedOnly = changedOnly && !filter.isOrdering
                    if (searchable) add(SourceFilterRow.GroupTools(index, filter, groupChangedOnly))
                    add(SourceFilterRow.GroupBody(index, filter, groupChangedOnly))
                    if (searchable) add(SourceFilterRow.GroupEnd(index))
                }
            }
            ordering.forEach { (index, filter) -> addFilter(index, filter) }
            if (ordering.isNotEmpty() && constraints.isNotEmpty() &&
                constraints.first().value !is EntryFilter.Separator
            ) {
                add(SourceFilterRow.OrderingDivider)
            }
            constraints.forEach { (index, filter) -> addFilter(index, filter) }
        }
    }
}

/**
 * Where each row sits in the cards of the root page: the filters between the source's headers and separators share a
 * card, an open group's search and options stay in its card, and every other row sits between cards with null.
 */
internal fun sourceFilterCardPositions(rows: List<SourceFilterRow>): List<RowGroupPosition?> {
    val positions = arrayOfNulls<RowGroupPosition>(rows.size)
    var card = mutableListOf<Int>()
    fun closeCard() {
        card.forEachIndexed { position, row -> positions[row] = RowGroupPosition.of(position, card.size) }
        card = mutableListOf()
    }
    rows.forEachIndexed { index, row ->
        when {
            // Only ends a group's pinned search; the card goes on.
            row is SourceFilterRow.GroupEnd -> Unit
            row.isInCard -> card += index
            else -> closeCard()
        }
    }
    closeCard()
    return positions.asList()
}

private val SourceFilterRow.isInCard: Boolean
    get() = when (this) {
        is SourceFilterRow.Filter -> filter !is EntryFilter.Header && filter !is EntryFilter.Separator
        is SourceFilterRow.GroupTools, is SourceFilterRow.GroupBody -> true
        else -> false
    }
