package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType

data class EntryCatalogueSourceInfo(
    val id: Long,
    val name: String,
    val language: String,
    val supportedEntryTypes: Set<EntryType>?,
    val itemOrientation: EntryItemOrientation,
    val supportsLatest: Boolean,
    val usesAsyncFilters: Boolean,
)

sealed interface EntryCatalogueSourceResolution {
    val sourceId: Long

    data class Available(val source: EntryCatalogueSourceInfo) : EntryCatalogueSourceResolution {
        override val sourceId: Long = source.id
    }

    data class Missing(override val sourceId: Long) : EntryCatalogueSourceResolution

    data class Unsupported(override val sourceId: Long) : EntryCatalogueSourceResolution
}

enum class EntryCatalogueUnavailableReason {
    SOURCE_MISSING,
    CATALOGUE_UNSUPPORTED,
    LATEST_UNSUPPORTED,
}
