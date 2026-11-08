package eu.kanade.domain.source.model

import app.katari.entry.interactions.catalogue.EntryCatalogueListing
import eu.kanade.tachiyomi.source.entry.EntryFilterList

fun catalogueListing(query: String, filters: EntryFilterList): EntryCatalogueListing {
    return when (query) {
        CATALOGUE_POPULAR_QUERY -> EntryCatalogueListing.Popular
        CATALOGUE_LATEST_QUERY -> EntryCatalogueListing.Latest
        else -> EntryCatalogueListing.Search(query, filters)
    }
}
