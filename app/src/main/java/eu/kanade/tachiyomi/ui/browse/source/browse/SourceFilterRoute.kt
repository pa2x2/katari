package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.runtime.saveable.Saver
import eu.kanade.tachiyomi.source.entry.EntryFilter

internal sealed interface SourceFilterRoute {
    data object Root : SourceFilterRoute

    /**
     * Navigation holds a stable path, never a mutable filter snapshot.
     *
     * The draft list mutates filter state in place and replaces detached graphs on reload.
     * Holding the filter object would freeze the paged sheet on the instance captured at
     * navigation time, so the sheet always resolves [path] against the current draft list.
     */
    data class PagedGroup(val path: List<Int>) : SourceFilterRoute
}

/** Paths only restore navigation within the same session's filter tree, never saved presets. */
internal fun sourceFilterRouteSaver() = Saver<SourceFilterRoute, List<Int>>(
    save = { route ->
        when (route) {
            SourceFilterRoute.Root -> emptyList()
            is SourceFilterRoute.PagedGroup -> route.path
        }
    },
    restore = { path ->
        if (path.isEmpty()) SourceFilterRoute.Root else SourceFilterRoute.PagedGroup(path)
    },
)

internal fun List<EntryFilter<*>>.pathTo(target: EntryFilter<*>): List<Int>? {
    forEachIndexed { index, filter ->
        if (filter === target) return listOf(index)
        if (filter is EntryFilter.Group<*>) {
            filter.state.filterIsInstance<EntryFilter<*>>().pathTo(target)?.let { return listOf(index) + it }
        }
    }
    return null
}

/** Resolves the live draft element for a navigated path, or null when the tree no longer contains it. */
internal fun List<EntryFilter<*>>.resolvePagedGroup(path: List<Int>): EntryFilter.PagedGroup<*>? {
    var children: List<EntryFilter<*>> = this
    var target: EntryFilter<*>? = null
    path.forEach { index ->
        target = children.getOrNull(index) ?: return null
        children = (target as? EntryFilter.Group<*>)?.state?.filterIsInstance<EntryFilter<*>>().orEmpty()
    }
    return target as? EntryFilter.PagedGroup<*>
}
