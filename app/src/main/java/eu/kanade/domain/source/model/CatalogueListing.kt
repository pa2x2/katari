package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import mihon.entry.interactions.catalogue.EntryCatalogueListing

fun catalogueListing(query: String, filters: EntryFilterList): EntryCatalogueListing {
    return when (query) {
        CATALOGUE_POPULAR_QUERY -> EntryCatalogueListing.Popular
        CATALOGUE_LATEST_QUERY -> EntryCatalogueListing.Latest
        else -> EntryCatalogueListing.Search(query, filters)
    }
}
