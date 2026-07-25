package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryFilterSuggestion

/** Normalized result of resolving and loading source-provided filter suggestions. */
sealed interface EntryCatalogueFilterSuggestionsResult {

    /** Suggestions loaded for the current filter input. */
    data class Available(
        val suggestions: List<EntryFilterSuggestion>,
    ) : EntryCatalogueFilterSuggestionsResult

    /** The source-defined query is absent or shorter than the configured minimum length. */
    data object NotApplicable : EntryCatalogueFilterSuggestionsResult

    /** The source cannot currently execute catalogue interactions. */
    data class Unavailable(
        val reason: EntryCatalogueUnavailableReason,
    ) : EntryCatalogueFilterSuggestionsResult

    /** The source failed while extracting or loading suggestions. */
    data class Failed(
        val cause: Throwable,
    ) : EntryCatalogueFilterSuggestionsResult
}
