package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryFilterList

data class EntryCatalogueBrowseRequest(
    val sourceId: Long,
    val listing: EntryCatalogueListing,
)

sealed interface EntryCatalogueListing {
    data object Popular : EntryCatalogueListing

    data object Latest : EntryCatalogueListing

    data class Search(
        val query: String,
        val filters: EntryFilterList,
    ) : EntryCatalogueListing
}

class EntryCatalogueNoResultsException : Exception()

class EntryCatalogueUnavailableException(
    val sourceId: Long,
    val reason: EntryCatalogueUnavailableReason,
) : Exception("Catalogue operation unavailable for source $sourceId: $reason")
