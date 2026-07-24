package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

data class EntryCatalogueSearchRequest(
    val sourceId: Long,
    val query: String,
    val requiredType: EntryType? = null,
)

sealed interface EntryCatalogueSearchResult {
    data class Success(val entries: List<Entry>) : EntryCatalogueSearchResult

    data class Unavailable(val reason: EntryCatalogueUnavailableReason) : EntryCatalogueSearchResult

    data class Failed(val cause: Throwable) : EntryCatalogueSearchResult
}
