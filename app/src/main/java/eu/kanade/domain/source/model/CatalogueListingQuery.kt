package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import mihon.entry.interactions.EntryCatalogueListing

const val CATALOGUE_POPULAR_QUERY = "eu.kanade.domain.source.interactor.CATALOG_POPULAR"
const val CATALOGUE_LATEST_QUERY = "eu.kanade.domain.source.interactor.CATALOG_LATEST"

fun catalogueListing(query: String, filters: EntryFilterList): EntryCatalogueListing {
    return when (query) {
        CATALOGUE_POPULAR_QUERY -> EntryCatalogueListing.Popular
        CATALOGUE_LATEST_QUERY -> EntryCatalogueListing.Latest
        else -> EntryCatalogueListing.Search(query, filters)
    }
}
