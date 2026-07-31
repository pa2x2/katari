package mihon.entry.interactions.catalogue

import eu.kanade.tachiyomi.source.entry.EntryFilterList

sealed interface EntryCatalogueFiltersResult {
    data class Available(val filters: EntryFilterList) : EntryCatalogueFiltersResult

    data class Unavailable(val reason: EntryCatalogueUnavailableReason) : EntryCatalogueFiltersResult

    data class Failed(val cause: Throwable) : EntryCatalogueFiltersResult
}
