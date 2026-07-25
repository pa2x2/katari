package eu.kanade.tachiyomi.source.entry

/**
 * Host request policy for an [EntryFilter.Autocomplete] filter.
 *
 * @property debounceMillis delay after the latest text or selection change before requesting suggestions.
 * @property minimumQueryLength minimum [EntryFilter.Autocomplete.getSuggestionQuery] length required for a request.
 * @property requestOnFocus whether focusing the field may request suggestions before the user edits it.
 * @property maximumResults maximum number of source results the host presents.
 */
data class EntryFilterAutocompleteOptions(
    val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
    val minimumQueryLength: Int = DEFAULT_MINIMUM_QUERY_LENGTH,
    val requestOnFocus: Boolean = false,
    val maximumResults: Int = DEFAULT_MAXIMUM_RESULTS,
) {
    init {
        require(debounceMillis >= 0) { "debounceMillis must not be negative" }
        require(minimumQueryLength >= 0) { "minimumQueryLength must not be negative" }
        require(maximumResults > 0) { "maximumResults must be positive" }
    }

    /** Default request-policy values. */
    companion object {
        /** Default delay used to coalesce ordinary type-ahead input. */
        const val DEFAULT_DEBOUNCE_MILLIS = 300L

        /** Default number of characters required before a lookup. */
        const val DEFAULT_MINIMUM_QUERY_LENGTH = 1

        /** Default maximum number of suggestions presented by the host. */
        const val DEFAULT_MAXIMUM_RESULTS = 20
    }
}

/**
 * Current text and selection supplied to an [EntryFilter.Autocomplete].
 *
 * Selection offsets are UTF-16 string indices and may be reversed.
 *
 * @property text complete current filter text.
 * @property selectionStart first selection endpoint.
 * @property selectionEnd second selection endpoint.
 */
data class EntryFilterTextInput(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
) {
    init {
        require(selectionStart in 0..text.length) { "selectionStart must be within text" }
        require(selectionEnd in 0..text.length) { "selectionEnd must be within text" }
    }
}

/**
 * Source-provided autocomplete result.
 *
 * @property id stable identity used by the host while presenting the result.
 * @property label user-visible result label.
 * @property value source-defined value available to [EntryFilter.Autocomplete.applySuggestion].
 */
data class EntryFilterSuggestion(
    val id: String,
    val label: String,
    val value: String = id,
) {
    init {
        require(id.isNotEmpty()) { "id must not be empty" }
        require(label.isNotEmpty()) { "label must not be empty" }
    }
}

/**
 * Complete text-field update returned after applying an [EntryFilterSuggestion].
 *
 * Selection offsets are UTF-16 string indices and may be reversed.
 *
 * @property text complete replacement filter text.
 * @property selectionStart first resulting selection endpoint.
 * @property selectionEnd second resulting selection endpoint.
 */
data class EntryFilterTextEdit(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int = selectionStart,
) {
    init {
        require(selectionStart in 0..text.length) { "selectionStart must be within text" }
        require(selectionEnd in 0..text.length) { "selectionEnd must be within text" }
    }
}
