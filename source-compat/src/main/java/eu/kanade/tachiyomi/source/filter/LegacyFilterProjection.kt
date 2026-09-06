package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList

fun FilterList.toEntryFilterList(): EntryFilterList =
    EntryFilterList(map { it.toEntryFilter() })

fun EntryFilterList.toLegacyFilterList(): FilterList =
    FilterList(map { it.toLegacyFilter() })

private fun Filter<*>.toEntryFilter(): EntryFilter<*> {
    val original = this
    return when (this) {
        is Filter.Header -> object : EntryFilter.Header(name), LegacySourceFilter {
            override val legacyFilter = original
        }
        is Filter.Separator -> object : EntryFilter.Separator(name), LegacySourceFilter {
            override val legacyFilter = original
        }
        is Filter.Select<*> ->
            object :
                EntryFilter.Select<Any?>(
                    name,
                    values.map {
                        it
                    }.toTypedArray(),
                    state,
                ),
                LegacySourceFilter {
                override val legacyFilter = original
            }
        is Filter.Text -> object : EntryFilter.Text(name, state), LegacySourceFilter {
            override val legacyFilter = original
        }
        is Filter.CheckBox -> object : EntryFilter.CheckBox(name, state), LegacySourceFilter {
            override val legacyFilter = original
        }
        is Filter.TriState -> object : EntryFilter.TriState(name, state), LegacySourceFilter {
            override val legacyFilter = original
        }
        is Filter.Group<*> ->
            object :
                EntryFilter.Group<EntryFilter<*>>(
                    name,
                    state.filterIsInstance<Filter<*>>().map { it.toEntryFilter() },
                ),
                LegacySourceFilter {
                override val legacyFilter = original
            }
        is Filter.Sort ->
            object :
                EntryFilter.Sort(
                    name,
                    values.copyOf(),
                    state?.let { EntryFilter.Sort.Selection(it.index, it.ascending) },
                ),
                LegacySourceFilter {
                override val legacyFilter = original
            }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun EntryFilter<*>.toLegacyFilter(): Filter<*> = when (this) {
    is EntryFilter.Header -> Filter.Header(name)
    is EntryFilter.Separator -> Filter.Separator(name)
    is EntryFilter.Select<*> -> object : Filter.Select<Any?>(name, values as Array<Any?>, state) {}
    is EntryFilter.Text -> object : Filter.Text(name, state) {}
    is EntryFilter.CheckBox -> object : Filter.CheckBox(name, state) {}
    is EntryFilter.TriState -> object : Filter.TriState(name, state) {}
    is EntryFilter.Group<*> -> object : Filter.Group<Filter<*>>(
        name,
        state.filterIsInstance<EntryFilter<*>>().map { it.toLegacyFilter() },
    ) {}
    is EntryFilter.Sort -> object : Filter.Sort(
        name,
        values,
        state?.let { Filter.Sort.Selection(it.index, it.ascending) },
    ) {}
    is EntryFilter.PagedGroup<*> -> error("Paged Entry filters cannot be converted to the legacy source API")
}
