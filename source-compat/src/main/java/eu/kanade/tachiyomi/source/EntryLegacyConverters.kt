package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SourceItemOrientation
import eu.kanade.tachiyomi.source.model.UpdateStrategy

fun FilterList.toEntryFilterList(): EntryFilterList =
    EntryFilterList(map { it.toEntryFilter() })

fun EntryFilterList.toLegacyFilterList(): FilterList =
    FilterList(map { it.toLegacyFilter() })

fun UpdateStrategy.toEntryUpdateStrategy(): EntryUpdateStrategy = when (this) {
    UpdateStrategy.ALWAYS_UPDATE -> EntryUpdateStrategy.ALWAYS_UPDATE
    UpdateStrategy.ONLY_FETCH_ONCE -> EntryUpdateStrategy.ONLY_FETCH_ONCE
}

fun EntryUpdateStrategy.toLegacyUpdateStrategy(): UpdateStrategy = when (this) {
    EntryUpdateStrategy.ALWAYS_UPDATE -> UpdateStrategy.ALWAYS_UPDATE
    EntryUpdateStrategy.ONLY_FETCH_ONCE -> UpdateStrategy.ONLY_FETCH_ONCE
}

fun Page.asEntryImagePage(): EntryImagePage =
    EntryImagePage(
        index = index,
        url = url,
        imageUrl = imageUrl,
    )

fun EntryImagePage.toLegacyPage(): Page =
    Page(
        index = index,
        url = url,
        imageUrl = imageUrl,
    )

fun EntryImagePage.toLegacyPage(progress: ProgressListener?): Page {
    if (progress == null) {
        return toLegacyPage()
    }
    return object : Page(index, url, imageUrl) {
        override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
            super.update(bytesRead, contentLength, done)
            progress.update(bytesRead, contentLength, done)
        }
    }
}

fun SourceItemOrientation.toEntryItemOrientation(): EntryItemOrientation = when (this) {
    SourceItemOrientation.VERTICAL -> EntryItemOrientation.VERTICAL
    SourceItemOrientation.HORIZONTAL -> EntryItemOrientation.HORIZONTAL
}

fun EntryItemOrientation.toLegacySourceItemOrientation(): SourceItemOrientation = when (this) {
    EntryItemOrientation.VERTICAL -> SourceItemOrientation.VERTICAL
    EntryItemOrientation.HORIZONTAL -> SourceItemOrientation.HORIZONTAL
}

@Suppress("UNCHECKED_CAST")
private fun Filter<*>.toEntryFilter(): EntryFilter<*> = when (this) {
    is Filter.Header -> EntryFilter.Header(name)
    is Filter.Separator -> EntryFilter.Separator(name)
    is Filter.Select<*> -> object : EntryFilter.Select<Any?>(name, values as Array<Any?>, state) {}
    is Filter.Text -> object : EntryFilter.Text(name, state) {}
    is Filter.CheckBox -> object : EntryFilter.CheckBox(name, state) {}
    is Filter.TriState -> object : EntryFilter.TriState(name, state) {}
    is Filter.Group<*> -> object : EntryFilter.Group<EntryFilter<*>>(
        name,
        state.filterIsInstance<Filter<*>>().map { it.toEntryFilter() },
    ) {}
    is Filter.Sort -> object : EntryFilter.Sort(
        name,
        values,
        state?.let { EntryFilter.Sort.Selection(it.index, it.ascending) },
    ) {}
}

@Suppress("UNCHECKED_CAST")
private fun EntryFilter<*>.toLegacyFilter(): Filter<*> = when (this) {
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
}
