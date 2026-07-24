package mihon.feature.migration.list.search

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryCatalogueFeature
import mihon.entry.interactions.EntryCatalogueSearchRequest
import mihon.entry.interactions.EntryCatalogueSearchResult
import mihon.entry.interactions.EntryCatalogueSourceInfo
import tachiyomi.domain.entry.model.Entry

class SmartSourceSearchEngine(
    extraSearchParams: String?,
    private val catalogueFeature: EntryCatalogueFeature,
) : BaseSmartSearchEngine<Entry>(extraSearchParams) {

    override fun getTitle(result: Entry) = result.title

    suspend fun regularSearch(source: EntryCatalogueSourceInfo, title: String, type: EntryType): Entry? {
        return regularSearch(makeSearchAction(source, type), title)
    }

    suspend fun deepSearch(source: EntryCatalogueSourceInfo, title: String, type: EntryType): Entry? {
        return deepSearch(makeSearchAction(source, type), title)
    }

    private fun makeSearchAction(source: EntryCatalogueSourceInfo, type: EntryType): SearchAction<Entry> = { query ->
        when (
            val result = catalogueFeature.search(
                EntryCatalogueSearchRequest(source.id, query, requiredType = type),
            )
        ) {
            is EntryCatalogueSearchResult.Success -> result.entries
            is EntryCatalogueSearchResult.Unavailable,
            is EntryCatalogueSearchResult.Failed,
            -> emptyList()
        }
    }
}
