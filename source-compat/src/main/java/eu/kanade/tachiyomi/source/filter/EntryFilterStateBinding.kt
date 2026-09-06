package eu.kanade.tachiyomi.source.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One gate per source, shared by search and filter callbacks, including cached filter graphs. */
class EntryFilterBinding {
    internal val mutex = Mutex()

    suspend fun captureFilters(factory: () -> EntryFilterList): EntryFilterList =
        mutex.withLock { factory().detachedCopy(this) }
}

/** Restores source-owned instances after a callback, including failures. */
internal fun <T> withBoundFilterValues(filters: List<EntryFilter<*>>, block: () -> T): T {
    val restore = bindFilterValues(filters)
    return try {
        block()
    } finally {
        restore()
    }
}

suspend fun <T> EntryFilterList.withSourceFilterValues(block: suspend (EntryFilterList) -> T): T {
    val binding = filterIsInstance<DetachedEntryFilter>().firstOrNull()?.binding
    suspend fun invoke(): T {
        val restore = bindFilterValues(this)
        return try {
            block(EntryFilterList(map { (it as? DetachedEntryFilter)?.sourceFilter ?: it }))
        } finally {
            restore()
        }
    }
    return if (binding == null) invoke() else binding.mutex.withLock { invoke() }
}

/** Schedule state-dependent synchronous source callbacks without blocking the UI thread. */
suspend fun <T> EntryFilter.PagedGroup<*>.withProjectedState(block: (EntryFilter.PagedGroup<*>) -> T): T =
    EntryFilterList(this).withSourceFilterValues { block(it.single() as EntryFilter.PagedGroup<*>) }

private fun bindFilterValues(filters: List<EntryFilter<*>>): () -> Unit {
    val restores = mutableListOf<() -> Unit>()
    fun bind(filter: EntryFilter<*>) {
        if (filter is EntryFilter.Group<*>) {
            filter.state.filterIsInstance<EntryFilter<*>>().forEach(::bind)
        } else {
            val source = (filter as? DetachedEntryFilter)?.sourceFilter ?: return
            val previous = source.state
            assignState(source, filter.state)
            restores += { assignState(source, previous) }
        }
    }
    filters.forEach(::bind)
    return { restores.asReversed().forEach { it() } }
}

@Suppress("UNCHECKED_CAST")
private fun assignState(filter: EntryFilter<*>, state: Any?) {
    (filter as EntryFilter<Any?>).state = state
}
