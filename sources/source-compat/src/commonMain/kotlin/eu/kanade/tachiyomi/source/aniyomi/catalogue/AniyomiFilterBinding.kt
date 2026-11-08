package eu.kanade.tachiyomi.source.aniyomi.catalogue

import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.filter.DetachedEntryFilter

/**
 * Runs [block] with the values of [this] bound into the extension's own filter objects, and puts their previous values
 * back afterwards: extensions keep and reuse those objects, so a search must not leave its values in them. Callers
 * serialize searches of one source, as the objects are shared.
 */
internal suspend fun <T> EntryFilterList.withAnimeFilterValues(block: suspend (AnimeFilterList) -> T): T {
    val restores = mutableListOf<() -> Unit>()
    fun bind(value: EntryFilter<*>): AnimeFilter<*> {
        val projection = (value as? DetachedEntryFilter)?.sourceFilter ?: value
        val target = (projection as? AniyomiSourceFilter)?.animeFilter ?: return value.toAnimeFilter()
        val previous = target.state
        restores += { target.assignState(previous) }
        val selected = when (value) {
            is EntryFilter.Group<*> -> value.state.filterIsInstance<EntryFilter<*>>().map(::bind)
            is EntryFilter.Sort -> value.state?.let { AnimeFilter.Sort.Selection(it.index, it.ascending) }
            else -> value.state
        }
        target.assignState(selected)
        return target
    }
    return try {
        block(AnimeFilterList(map(::bind)))
    } finally {
        restores.asReversed().forEach { it() }
    }
}

@Suppress("UNCHECKED_CAST")
private fun AnimeFilter<*>.assignState(value: Any?) {
    (this as AnimeFilter<Any?>).state = value
}
