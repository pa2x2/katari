package mihon.feature.migration.list.search

import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SEntry
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.model.Entry

class SmartSourceSearchEngine(extraSearchParams: String?) : BaseSmartSearchEngine<SEntry>(extraSearchParams) {

    override fun getTitle(result: SEntry) = result.title

    suspend fun regularSearch(source: EntryCatalogueSource, title: String, type: EntryType): Entry? {
        return regularSearch(makeSearchAction(source, type), title).let {
            it?.toEntry(source.id)
        }
    }

    suspend fun deepSearch(source: EntryCatalogueSource, title: String, type: EntryType): Entry? {
        return deepSearch(makeSearchAction(source, type), title).let {
            it?.toEntry(source.id)
        }
    }

    private fun makeSearchAction(source: EntryCatalogueSource, type: EntryType): SearchAction<SEntry> = { query ->
        source.getSearchContent(1, query, source.getFilterList()).items
            .filter { it.type == type }
    }
}
