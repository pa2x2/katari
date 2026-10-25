package eu.kanade.tachiyomi.source.aniyomi.catalogue

import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList

/*
 * Each Aniyomi filter kind has an Entry counterpart. A projected filter remembers the extension's object, whose values
 * a search binds before calling the extension (AniyomiFilterBinding), since extensions read their own filter subclasses.
 */

/** The extension's filter object a projected filter came from. */
internal interface AniyomiSourceFilter {
    val animeFilter: AnimeFilter<*>
}

internal fun AnimeFilterList.toEntryFilterList(): EntryFilterList = EntryFilterList(map { it.toEntryFilter() })

private fun AnimeFilter<*>.toEntryFilter(): EntryFilter<*> {
    val original = this
    return when (this) {
        is AnimeFilter.Header -> object : EntryFilter.Header(name), AniyomiSourceFilter {
            override val animeFilter = original
        }
        is AnimeFilter.Separator -> object : EntryFilter.Separator(name), AniyomiSourceFilter {
            override val animeFilter = original
        }
        is AnimeFilter.Select<*> ->
            object : EntryFilter.Select<Any?>(name, values.map { it }.toTypedArray(), state), AniyomiSourceFilter {
                override val animeFilter = original
            }
        is AnimeFilter.Text -> object : EntryFilter.Text(name, state), AniyomiSourceFilter {
            override val animeFilter = original
        }
        is AnimeFilter.CheckBox -> object : EntryFilter.CheckBox(name, state), AniyomiSourceFilter {
            override val animeFilter = original
        }
        is AnimeFilter.TriState -> object : EntryFilter.TriState(name, state), AniyomiSourceFilter {
            override val animeFilter = original
        }
        is AnimeFilter.Group<*> ->
            object :
                EntryFilter.Group<EntryFilter<*>>(
                    name,
                    state.filterIsInstance<AnimeFilter<*>>().map { it.toEntryFilter() },
                ),
                AniyomiSourceFilter {
                override val animeFilter = original
            }
        is AnimeFilter.Sort ->
            object :
                EntryFilter.Sort(
                    name,
                    values.copyOf(),
                    state?.let { EntryFilter.Sort.Selection(it.index, it.ascending) },
                ),
                AniyomiSourceFilter {
                override val animeFilter = original
            }
    }
}

/** A filter the host made itself, with no extension object behind it. */
@Suppress("UNCHECKED_CAST")
internal fun EntryFilter<*>.toAnimeFilter(): AnimeFilter<*> = when (this) {
    is EntryFilter.Header -> AnimeFilter.Header(name)
    is EntryFilter.Separator -> AnimeFilter.Separator(name)
    is EntryFilter.Select<*> -> object : AnimeFilter.Select<Any?>(name, values as Array<Any?>, state) {}
    is EntryFilter.Text -> object : AnimeFilter.Text(name, state) {}
    is EntryFilter.CheckBox -> object : AnimeFilter.CheckBox(name, state) {}
    is EntryFilter.TriState -> object : AnimeFilter.TriState(name, state) {}
    is EntryFilter.Group<*> -> object : AnimeFilter.Group<AnimeFilter<*>>(
        name,
        state.filterIsInstance<EntryFilter<*>>().map { it.toAnimeFilter() },
    ) {}
    is EntryFilter.Sort -> object : AnimeFilter.Sort(
        name,
        values,
        state?.let { AnimeFilter.Sort.Selection(it.index, it.ascending) },
    ) {}
    is EntryFilter.PagedGroup<*> -> error("Paged Entry filters cannot be converted to Aniyomi's filters")
}
