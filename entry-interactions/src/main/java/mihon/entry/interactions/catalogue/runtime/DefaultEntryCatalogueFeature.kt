package mihon.entry.interactions

import kotlinx.coroutines.CancellationException
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.identity
import tachiyomi.domain.source.model.EntrySourceDescription

internal class DefaultEntryCatalogueFeature(
    private val host: EntryCatalogueProviderHost,
    private val graphStateValidator: EntryCatalogueGraphStateValidator,
    private val networkToLocalEntry: NetworkToLocalEntry,
) : EntryCatalogueFeature {
    override val isInitialized = host.isInitialized

    override fun sources(): List<EntryCatalogueSourceInfo> {
        return host.sources().map { source ->
            graphStateValidator.validate(source.description)
            source.toInfo()
        }
    }

    override fun source(sourceId: Long): EntryCatalogueSourceResolution {
        return when (val resolution = host.source(sourceId)) {
            is EntryCatalogueHostSourceResolution.Available -> {
                graphStateValidator.validate(resolution.source.description)
                EntryCatalogueSourceResolution.Available(resolution.source.toInfo())
            }
            EntryCatalogueHostSourceResolution.Missing -> EntryCatalogueSourceResolution.Missing(sourceId)
            EntryCatalogueHostSourceResolution.Unsupported -> EntryCatalogueSourceResolution.Unsupported(sourceId)
        }
    }

    override fun description(sourceId: Long): EntrySourceDescription {
        return host.describe(sourceId).also(graphStateValidator::validate)
    }

    override suspend fun filters(sourceId: Long): EntryCatalogueFiltersResult {
        val source = when (val resolution = source(sourceId)) {
            is EntryCatalogueSourceResolution.Available -> resolution.source
            is EntryCatalogueSourceResolution.Missing -> {
                return EntryCatalogueFiltersResult.Unavailable(EntryCatalogueUnavailableReason.SOURCE_MISSING)
            }
            is EntryCatalogueSourceResolution.Unsupported -> {
                return EntryCatalogueFiltersResult.Unavailable(EntryCatalogueUnavailableReason.CATALOGUE_UNSUPPORTED)
            }
        }
        return try {
            EntryCatalogueFiltersResult.Available(host.filters(source.id))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            EntryCatalogueFiltersResult.Failed(error)
        }
    }

    override fun paging(request: EntryCatalogueBrowseRequest) = EntryCataloguePagingSource(
        request = request,
        host = host,
        sourceResolution = source(request.sourceId),
        networkToLocalEntry = networkToLocalEntry,
    )

    override suspend fun search(request: EntryCatalogueSearchRequest): EntryCatalogueSearchResult {
        val source = when (val resolution = source(request.sourceId)) {
            is EntryCatalogueSourceResolution.Available -> resolution.source
            is EntryCatalogueSourceResolution.Missing -> {
                return EntryCatalogueSearchResult.Unavailable(EntryCatalogueUnavailableReason.SOURCE_MISSING)
            }
            is EntryCatalogueSourceResolution.Unsupported -> {
                return EntryCatalogueSearchResult.Unavailable(EntryCatalogueUnavailableReason.CATALOGUE_UNSUPPORTED)
            }
        }
        return try {
            val entries = host.page(
                sourceId = source.id,
                page = 1,
                listing = EntryCatalogueListing.Search(request.query, filters = host.backgroundFilters(source.id)),
            ).items
                .map { it.toEntry(source.id) }
                .filter { request.requiredType == null || it.type == request.requiredType }
                .distinctBy { it.identity() }
            EntryCatalogueSearchResult.Success(entries)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            EntryCatalogueSearchResult.Failed(error)
        }
    }

    private fun EntryCatalogueHostSource.toInfo(): EntryCatalogueSourceInfo {
        val catalogue = checkNotNull(description.catalogue)
        return EntryCatalogueSourceInfo(
            id = id,
            name = name,
            language = description.language,
            supportedEntryTypes = description.supportedEntryTypes,
            itemOrientation = description.itemOrientation,
            supportsLatest = catalogue.supportsLatest,
            usesAsyncFilters = usesAsyncFilters,
        )
    }
}
