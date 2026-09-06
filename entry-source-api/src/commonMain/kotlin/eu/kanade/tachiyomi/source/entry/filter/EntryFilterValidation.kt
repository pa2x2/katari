package eu.kanade.tachiyomi.source.entry.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList

/** Optional, side-effect-free validation of a field or related fields owned by a group. */
interface EntryFilterValidator {
    fun validateFilter(values: List<EntryFilter<*>> = emptyList()): List<EntryFilterValidationIssue>
}

data class EntryFilterValidationIssue(
    val filterIds: Set<String>,
    val code: EntryFilterValidationCode,
    val message: String? = null,
)

enum class EntryFilterValidationCode { REQUIRED, INVALID_DATE, DATE_PRECISION, DATE_BOUNDS, DATE_RANGE, SOURCE }

fun EntryFilterList.validationIssues(): List<EntryFilterValidationIssue> =
    flatMap { it.validationIssues() } + identityIssues()

fun EntryFilter<*>.validationIssues(): List<EntryFilterValidationIssue> =
    (this as? EntryFilterValidator)?.validateFilter(
        (this as? EntryFilter.Group<*>)?.state?.filterIsInstance<EntryFilter<*>>() ?: listOf(this),
    ).orEmpty() +
        (this as? EntryFilter.Group<*>)?.state?.filterIsInstance<EntryFilter<*>>()
            ?.flatMap { it.validationIssues() }.orEmpty()

class EntryFilterValidationException(val issues: List<EntryFilterValidationIssue>) :
    IllegalArgumentException(issues.joinToString { it.message ?: it.code.name })

fun EntryFilterList.requireValidFilters() {
    val issues = validationIssues()
    if (issues.isNotEmpty()) throw EntryFilterValidationException(issues)
}
