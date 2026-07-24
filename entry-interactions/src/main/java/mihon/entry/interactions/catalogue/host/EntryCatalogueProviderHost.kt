package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.source.model.EntrySourceDescription

internal interface EntryCatalogueProviderHost {
    val isInitialized: StateFlow<Boolean>

    fun sources(): List<EntryCatalogueHostSource>

    fun source(sourceId: Long): EntryCatalogueHostSourceResolution

    fun describe(sourceId: Long): EntrySourceDescription

    fun describe(source: UnifiedSource): EntrySourceDescription

    suspend fun filters(sourceId: Long): EntryFilterList

    fun backgroundFilters(sourceId: Long): EntryFilterList

    suspend fun page(
        sourceId: Long,
        page: Int,
        listing: EntryCatalogueListing,
    ): EntryPageResult<SEntry>
}

internal data class EntryCatalogueHostSource(
    val id: Long,
    val name: String,
    val description: EntrySourceDescription,
    val usesAsyncFilters: Boolean,
)

internal sealed interface EntryCatalogueHostSourceResolution {
    data class Available(val source: EntryCatalogueHostSource) : EntryCatalogueHostSourceResolution

    data object Missing : EntryCatalogueHostSourceResolution

    data object Unsupported : EntryCatalogueHostSourceResolution
}
