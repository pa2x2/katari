package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.runtime.saveable.Saver
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList

internal sealed interface SourceFilterRoute {
    data object Root : SourceFilterRoute

    data class PagedGroup(val filter: EntryFilter.PagedGroup<*>) : SourceFilterRoute
}

/** Paths only restore navigation within the same session's filter tree, never saved presets. */
internal fun sourceFilterRouteSaver(filters: EntryFilterList) = Saver<SourceFilterRoute, List<Int>>(
    save = { route ->
        when (route) {
            SourceFilterRoute.Root -> emptyList()
            is SourceFilterRoute.PagedGroup -> filters.pathTo(route.filter).orEmpty()
        }
    },
    restore = { path ->
        var children: List<EntryFilter<*>> = filters
        var target: EntryFilter<*>? = null
        path.forEach { index ->
            target = children.getOrNull(index)
            children = (target as? EntryFilter.Group<*>)?.state?.filterIsInstance<EntryFilter<*>>().orEmpty()
        }
        (target as? EntryFilter.PagedGroup<*>)?.let { SourceFilterRoute.PagedGroup(it) } ?: SourceFilterRoute.Root
    },
)

private fun List<EntryFilter<*>>.pathTo(target: EntryFilter<*>): List<Int>? {
    forEachIndexed { index, filter ->
        if (filter === target) return listOf(index)
        if (filter is EntryFilter.Group<*>) {
            filter.state.filterIsInstance<EntryFilter<*>>().pathTo(target)?.let { return listOf(index) + it }
        }
    }
    return null
}
