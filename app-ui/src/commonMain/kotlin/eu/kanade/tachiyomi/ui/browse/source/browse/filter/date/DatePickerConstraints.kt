package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate

internal fun EntryDateFilter.containsPeriod(value: EntryPartialDate): Boolean =
    minimum.let { it == null || value.latestDayKey() >= it.earliestDayKey() } &&
        maximum.let { it == null || value.earliestDayKey() <= it.latestDayKey() }

/** Validate a detached typed candidate, including source rules, without changing the filter draft. */
internal fun PartialDateEditorState.validationIssues(filter: EntryDateFilter) = EntryDateFilter(
    name = filter.name,
    filterMetadata = filter.filterMetadata,
    allowedPrecisions = filter.allowedPrecisions,
    minimum = filter.minimum,
    maximum = filter.maximum,
    required = filter.required,
).also { it.state = candidateText }.let { candidate ->
    candidate.validateFilter().ifEmpty { filter.validateFilter(listOf(candidate)) }
}

internal fun PartialDateEditorState.canConfirm(filter: EntryDateFilter): Boolean =
    (value != null || (typing && raw.isBlank() && !filter.required)) && validationIssues(filter).isEmpty()
