package eu.kanade.tachiyomi.source.entry

/**
 * Mutable search-filter state presented by a catalogue source.
 *
 * @property name user-visible filter label.
 * @property state current value selected by the user.
 */
sealed class EntryFilter<T>(val name: String, var state: T) {
    /** Non-interactive heading displayed between filters. */
    open class Header(name: String) : EntryFilter<Any>(name, 0)

    /** Visual separator displayed between filters. */
    open class Separator(name: String = "") : EntryFilter<Any>(name, 0)

    /**
     * Single-choice filter backed by an index into [values].
     *
     * @property values available values in display order.
     */
    abstract class Select<V>(name: String, val values: Array<V>, state: Int = 0) : EntryFilter<Int>(
        name,
        state,
    )

    /** Free-form text filter. */
    abstract class Text(name: String, state: String = "") : EntryFilter<String>(name, state)

    /**
     * Free-form text filter that can load source-defined suggestions for the text around the current selection.
     *
     * The host owns request scheduling, cancellation, and presentation. Implementations own their input grammar:
     * [getSuggestionQuery] extracts the lookup value and [applySuggestion] returns the complete text edit after a
     * user selects a result.
     *
     * The durable [state] remains a plain string, so searches and saved filter presets do not depend on transient
     * suggestion results.
     *
     * @property options request policy applied by the host while this filter is focused.
     */
    abstract class Autocomplete(
        name: String,
        state: String = "",
        val options: EntryFilterAutocompleteOptions = EntryFilterAutocompleteOptions(),
    ) : Text(name, state) {

        /**
         * Extracts the lookup value for [input].
         *
         * Return `null` when suggestions do not apply at the current selection, such as while editing an unsupported
         * operator or metadata expression.
         */
        abstract fun getSuggestionQuery(input: EntryFilterTextInput): String?

        /**
         * Loads suggestions for [query].
         *
         * [input] is the snapshot from which [query] was extracted and may be used to distinguish source-defined
         * syntax contexts. Results should use stable [EntryFilterSuggestion.id] values.
         */
        abstract suspend fun getSuggestions(
            input: EntryFilterTextInput,
            query: String,
        ): List<EntryFilterSuggestion>

        /**
         * Applies [suggestion] to [input].
         *
         * Implementations must return the complete resulting text and selection. This lets the source preserve or
         * insert its own separators, quoting, grouping, exclusion, and other syntax without host interpretation.
         */
        abstract fun applySuggestion(
            input: EntryFilterTextInput,
            suggestion: EntryFilterSuggestion,
        ): EntryFilterTextEdit
    }

    /**
     * Lazily loaded collection of source-defined filter controls.
     *
     * [state] is the only durable truth. Loaded page items and their projected controls are disposable views of that
     * state and must not be used by implementations to retain selections. The host owns paging, search scheduling,
     * cancellation, refresh presentation, and failure handling.
     *
     * Implementations must use an immutable [S]. [encodeState] and [decodeState] allow the host to persist that typed
     * state without knowing its representation.
     */
    abstract class PagedGroup<S>(
        name: String,
        initialState: S,
        val options: EntryFilterPagingOptions = EntryFilterPagingOptions(),
    ) : EntryFilter<S>(name, initialState) {
        private val initialState = initialState

        /** Loads one source-ordered page for the requested scope and optional search query. */
        abstract suspend fun getPage(request: EntryFilterPageRequest): EntryFilterPage

        /**
         * Projects [item] into an interactive leaf filter.
         *
         * [previous] is the prior projection for the same stable item ID when one remains visible. Implementations may
         * update and return it to preserve editing/focus state, or return a replacement control.
         */
        abstract fun projectItem(
            item: EntryFilterPageItem,
            previous: EntryFilter<*>?,
        ): EntryFilter<*>

        /** Folds an updated projected control back into a new immutable durable state. */
        abstract fun reduceItemUpdate(
            item: EntryFilterPageItem,
            updatedFilter: EntryFilter<*>,
        ): S

        /** Applies one projected control update without exposing [S] to a star-projected host caller. */
        fun applyItemUpdate(
            item: EntryFilterPageItem,
            updatedFilter: EntryFilter<*>,
        ) {
            state = reduceItemUpdate(item, updatedFilter)
        }

        /** Returns the number of source-defined active items represented by [state]. */
        abstract fun selectedItemCount(state: S): Int

        /** Returns the active-item count for the current durable state. */
        fun currentSelectedItemCount(): Int = selectedItemCount(state)

        /** Encodes [state] for app-owned filter preset persistence. This must be deterministic and side-effect free. */
        abstract fun encodeState(state: S): String

        /** Decodes persisted state, or returns `null` when it is unknown or invalid. */
        abstract fun decodeState(value: String): S?

        /** Encodes the current durable state without exposing [S] to a star-projected host caller. */
        fun encodeCurrentState(): String = encodeState(state)

        /** Restores encoded state and reports whether decoding succeeded. */
        fun restoreEncodedState(value: String): Boolean {
            val decoded = decodeState(value) ?: return false
            state = decoded
            return true
        }

        /** Restores the source-defined initial state without retaining loaded page controls. */
        fun resetState() {
            state = initialState
        }
    }

    /** Boolean filter rendered as a check box. */
    abstract class CheckBox(name: String, state: Boolean = false) : EntryFilter<Boolean>(name, state)

    /** Three-state filter that can ignore, include, or exclude a value. */
    abstract class TriState(name: String, state: Int = STATE_IGNORE) : EntryFilter<Int>(name, state) {
        /** Returns whether the filter has no effect. */
        fun isIgnored() = state == STATE_IGNORE

        /** Returns whether matching items should be included. */
        fun isIncluded() = state == STATE_INCLUDE

        /** Returns whether matching items should be excluded. */
        fun isExcluded() = state == STATE_EXCLUDE

        /** Integer states used by [TriState]. */
        companion object {
            /** Ignore this filter. */
            const val STATE_IGNORE = 0

            /** Include matching items. */
            const val STATE_INCLUDE = 1

            /** Exclude matching items. */
            const val STATE_EXCLUDE = 2
        }
    }

    /** Filter that groups a list of child filter values under one label. */
    abstract class Group<V>(name: String, state: List<V>) : EntryFilter<List<V>>(name, state)

    /**
     * Sort filter backed by an index into [values] and an ascending-direction flag.
     *
     * @property values available sort labels in display order.
     */
    abstract class Sort(name: String, val values: Array<String>, state: Selection? = null) :
        EntryFilter<Sort.Selection?>(name, state) {
        /**
         * Selected sort option.
         *
         * @property index index into [Sort.values].
         * @property ascending whether the selected ordering is ascending.
         */
        data class Selection(val index: Int, val ascending: Boolean)
    }

    /** Compares filter name and current state. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntryFilter<*>) return false

        return name == other.name && state == other.state
    }

    /** Returns a hash derived from filter name and current state. */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (state?.hashCode() ?: 0)
        return result
    }
}
