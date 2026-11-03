package eu.kanade.tachiyomi.ui.browse.source.browse.filter.group

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue

/** Whether a filter group is open, what its option search holds, and whether it lists only selected options. */
@Stable
internal class FilterGroupUiState(expanded: Boolean = false, query: String = "", selectedOnly: Boolean = false) {
    var expanded by mutableStateOf(expanded)
    var query by mutableStateOf(query)
    var selectedOnly by mutableStateOf(selectedOnly)

    companion object {
        val Saver: Saver<FilterGroupUiState, Any> = listSaver(
            save = { listOf(it.expanded, it.query, it.selectedOnly) },
            restore = { FilterGroupUiState(it[0] as Boolean, it[1] as String, it[2] as Boolean) },
        )
    }
}

/**
 * States of the sheet's top-level groups, keyed by the group's index in the source's filter list.
 *
 * The sheet lays a top-level group out as several list rows, so their shared state lives here instead of inside one
 * composable. The map only caches stable state objects; everything observable is held by the objects themselves.
 */
@Stable
internal class FilterGroupUiStates private constructor(private val states: MutableMap<Int, FilterGroupUiState>) {
    constructor(expandedIndex: Int?) : this(
        expandedIndex?.let { mutableMapOf(it to FilterGroupUiState(expanded = true)) } ?: mutableMapOf(),
    )

    fun of(index: Int): FilterGroupUiState = states.getOrPut(index) { FilterGroupUiState() }

    companion object {
        val Saver: Saver<FilterGroupUiStates, Any> = listSaver(
            save = { holder ->
                holder.states.flatMap { (index, state) ->
                    listOf(index, state.expanded, state.query, state.selectedOnly)
                }
            },
            restore = { saved ->
                FilterGroupUiStates(
                    saved.chunked(4).associateTo(mutableMapOf()) { (index, expanded, query, selectedOnly) ->
                        index as Int to
                            FilterGroupUiState(expanded as Boolean, query as String, selectedOnly as Boolean)
                    },
                )
            },
        )
    }
}
