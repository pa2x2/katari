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
