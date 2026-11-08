package eu.kanade.tachiyomi.source.entry.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter

/**
 * Typed date access with a text-compatible backing value for legacy preset decoding and incomplete editor drafts.
 * Hosts must validate before dispatching to a source. Existing Text filters never acquire date semantics implicitly.
 */
open class EntryDateFilter(
    name: String,
    override val filterMetadata: EntryFilterMetadata,
    initialValue: EntryPartialDate? = null,
    val allowedPrecisions: Set<EntryDatePrecision> = EntryDatePrecision.entries.toSet(),
    val minimum: EntryPartialDate? = null,
    val maximum: EntryPartialDate? = null,
    val required: Boolean = false,
) : EntryFilter.Text(name, initialValue?.toString().orEmpty()), EntryFilterMetadataProvider, EntryFilterValidator {
    init {
        require(allowedPrecisions.isNotEmpty()) { "A date must support at least one precision" }
        require(minimum == null || maximum == null || minimum.earliestDayKey() <= maximum.latestDayKey()) {
            "The minimum date must not exceed the maximum date"
        }
    }

    var dateValue: EntryPartialDate?
        get() = if (state.isBlank()) null else requireNotNull(EntryPartialDate.parse(state)) { "Invalid date: $name" }
        set(value) {
            state = value?.toString().orEmpty()
        }

    override fun validateFilter(values: List<EntryFilter<*>>): List<EntryFilterValidationIssue> {
        val text = (values.singleOrNull() as? EntryFilter.Text)?.state ?: state
        val value = EntryPartialDate.parse(text)
        val code = when {
            text.isBlank() -> if (required) EntryFilterValidationCode.REQUIRED else null
            value == null -> EntryFilterValidationCode.INVALID_DATE
            value.precision !in allowedPrecisions -> EntryFilterValidationCode.DATE_PRECISION
            minimum != null && value.latestDayKey() < minimum.earliestDayKey() -> EntryFilterValidationCode.DATE_BOUNDS
            maximum != null && value.earliestDayKey() > maximum.latestDayKey() -> EntryFilterValidationCode.DATE_BOUNDS
            else -> null
        }
        return code?.let { listOf(EntryFilterValidationIssue(setOfNotNull(filterMetadata.id), code)) }.orEmpty()
    }
}
