package tachiyomi.data.source

import eu.kanade.tachiyomi.source.entry.ConfigurableSource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.SourceHomePage
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.preferenceKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.source.model.SourceWithCount
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.resolvedSupportedEntryTypes
import tachiyomi.domain.source.model.Source as DomainSource

@OptIn(ExperimentalCoroutinesApi::class)
class SourceRepositoryImpl(
    private val sourceManager: SourceManager,
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : SourceRepository {

    override fun getConfigurableSourceIds(): List<Long> {
        return sourceManager.getAll()
            .filterIsInstance<ConfigurableSource>()
            .map { it.id }
            .distinct()
    }

    override fun getConfigurableSourceKeys(): List<String> {
        return sourceManager.getAll()
            .filterIsInstance<ConfigurableSource>()
            .map { it.preferenceKey() }
            .distinct()
    }

    override fun getSources(): Flow<List<DomainSource>> {
        return sourceManager.sources.map { sources ->
            sources.map {
                mapSourceToDomainSource(it).copy(
                    supportsLatest = (it as? EntryCatalogueSource)?.supportsLatest ?: false,
                    supportsImmersiveFeed = (it as? EntryCatalogueSource)?.supportsImmersiveFeed ?: false,
                )
            }
        }
    }

    override fun getOnlineSources(): Flow<List<DomainSource>> {
        return sourceManager.sources.map { sources ->
            sources
                .filterIsInstance<SourceHomePage>()
                .map(::mapSourceToDomainSource)
        }
    }

    override fun getSourcesWithFavoriteCount(): Flow<List<Pair<DomainSource, Long>>> {
        return combine(
            profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
                handler.subscribeToList { entriesQueries.getSourceIdWithFavoriteCount(profileId) }
            },
            sourceManager.sources,
        ) { sourceIdWithFavoriteCount, _ -> sourceIdWithFavoriteCount }
            .map {
                it.map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId)
                    val domainSource = mapSourceToDomainSource(source).copy(
                        isStub = source is UnifiedStubSource,
                    )
                    domainSource to count
                }
            }
    }

    override fun getSourcesWithNonLibraryEntries(): Flow<List<SourceWithCount>> {
        val sourceIdWithNonLibraryEntries = profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList { entriesQueries.getSourceIdsWithNonLibraryEntries(profileId) }
        }
        return sourceIdWithNonLibraryEntries.map { sourceId ->
            sourceId.map { (sourceId, count) ->
                val source = sourceManager.getOrStub(sourceId)
                val domainSource = mapSourceToDomainSource(source).copy(
                    isStub = source is UnifiedStubSource,
                )
                SourceWithCount(domainSource, count)
            }
        }
    }

    private fun mapSourceToDomainSource(source: UnifiedSource): DomainSource = DomainSource(
        id = source.id,
        lang = (source as? EntryCatalogueSource)?.lang
            ?: (source as? UnifiedStubSource)?.lang
            ?: "",
        name = source.name,
        supportsLatest = false,
        supportedEntryTypes = source.resolvedSupportedEntryTypes(),
        isStub = false,
    )
}
