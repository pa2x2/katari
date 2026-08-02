package mihon.feature.migration.discovery

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.catalogue.EntryCatalogueSearchRequest
import mihon.entry.interactions.catalogue.EntryCatalogueSearchResult
import mihon.entry.interactions.catalogue.EntryCatalogueSourceResolution
import mihon.entry.interactions.catalogue.EntryCatalogueUnavailableReason
import mihon.feature.migration.discovery.model.SourceMigrationDiscoveryRequest
import mihon.feature.migration.discovery.model.SourceMigrationDiscoveryResult
import mihon.feature.migration.discovery.model.SourceMigrationSourceFailure
import mihon.feature.migration.session.model.SourceMigrationDiscoveryFailureReason
import tachiyomi.domain.entry.model.Entry
import java.util.concurrent.ConcurrentHashMap

class SourceMigrationCandidateDiscovery internal constructor(
    private val catalogue: EntryCatalogueFeature,
    private val queryFactory: SourceMigrationSearchQueryFactory = SourceMigrationSearchQueryFactory(),
    private val ranker: SourceMigrationCandidateRanker = SourceMigrationCandidateRanker(queryFactory),
    maxConcurrentSources: Int = DEFAULT_CONCURRENT_SOURCES,
) {
    private val sourcePermits = Semaphore(maxConcurrentSources)
    private val sourceLocks = ConcurrentHashMap<Long, Mutex>()

    init {
        require(maxConcurrentSources > 0) { "Migration discovery concurrency must be positive" }
    }

    suspend fun discover(request: SourceMigrationDiscoveryRequest): SourceMigrationDiscoveryResult {
        val sourceResults = supervisorScope {
            request.targetSourceIds.mapIndexed { sourcePriority, sourceId ->
                async {
                    discoverSource(request, sourceId, sourcePriority)
                }
            }.awaitAll()
        }
        val entries = sourceResults.flatMap(SourceResult::entries)
        return SourceMigrationDiscoveryResult(
            candidates = ranker.rank(request.sourceTitle, entries),
            failures = sourceResults.mapNotNull(SourceResult::failure),
        )
    }

    private suspend fun discoverSource(
        request: SourceMigrationDiscoveryRequest,
        sourceId: Long,
        sourcePriority: Int,
    ): SourceResult {
        val resolution = catalogue.source(sourceId)
        if (resolution !is EntryCatalogueSourceResolution.Available) {
            return SourceResult(
                failure = SourceMigrationSourceFailure(
                    sourceId = sourceId,
                    reason = when (resolution) {
                        is EntryCatalogueSourceResolution.Missing -> {
                            SourceMigrationDiscoveryFailureReason.SOURCE_MISSING
                        }
                        is EntryCatalogueSourceResolution.Unsupported -> {
                            SourceMigrationDiscoveryFailureReason.CATALOGUE_UNSUPPORTED
                        }
                        is EntryCatalogueSourceResolution.Available -> error("Handled above")
                    },
                    retryable = false,
                ),
            )
        }
        val supportedTypes = resolution.source.supportedEntryTypes
        if (supportedTypes != null && request.entryType !in supportedTypes) {
            return SourceResult(
                failure = SourceMigrationSourceFailure(
                    sourceId = sourceId,
                    reason = SourceMigrationDiscoveryFailureReason.ENTRY_TYPE_UNSUPPORTED,
                    retryable = false,
                ),
            )
        }

        return sourcePermits.withPermit {
            sourceLocks.getOrPut(sourceId) { Mutex() }.withLock {
                searchSource(request, sourceId, sourcePriority)
            }
        }
    }

    private suspend fun searchSource(
        request: SourceMigrationDiscoveryRequest,
        sourceId: Long,
        sourcePriority: Int,
    ): SourceResult {
        val entries = mutableListOf<Pair<Int, Entry>>()
        return try {
            for (query in queryFactory.queries(request.sourceTitle, request.depth)) {
                when (
                    val result = catalogue.search(
                        EntryCatalogueSearchRequest(
                            sourceId = sourceId,
                            query = query,
                            requiredType = request.entryType,
                        ),
                    )
                ) {
                    is EntryCatalogueSearchResult.Success -> {
                        entries += result.entries.map { sourcePriority to it }
                    }
                    is EntryCatalogueSearchResult.Unavailable -> {
                        return SourceResult(entries = entries, failure = result.toFailure(sourceId))
                    }
                    is EntryCatalogueSearchResult.Failed -> {
                        return SourceResult(
                            entries = entries,
                            failure = SourceMigrationSourceFailure(
                                sourceId = sourceId,
                                reason = SourceMigrationDiscoveryFailureReason.SEARCH_FAILED,
                                retryable = true,
                                detail = result.cause.message,
                            ),
                        )
                    }
                }
            }
            SourceResult(entries = entries)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            SourceResult(
                entries = entries,
                failure = SourceMigrationSourceFailure(
                    sourceId = sourceId,
                    reason = SourceMigrationDiscoveryFailureReason.SEARCH_FAILED,
                    retryable = true,
                    detail = error.message,
                ),
            )
        }
    }

    private fun EntryCatalogueSearchResult.Unavailable.toFailure(sourceId: Long): SourceMigrationSourceFailure {
        val failureReason = when (reason) {
            EntryCatalogueUnavailableReason.SOURCE_MISSING -> SourceMigrationDiscoveryFailureReason.SOURCE_MISSING
            EntryCatalogueUnavailableReason.CATALOGUE_UNSUPPORTED,
            EntryCatalogueUnavailableReason.LATEST_UNSUPPORTED,
            -> SourceMigrationDiscoveryFailureReason.CATALOGUE_UNSUPPORTED
        }
        return SourceMigrationSourceFailure(
            sourceId = sourceId,
            reason = failureReason,
            retryable = false,
        )
    }

    private data class SourceResult(
        val entries: List<Pair<Int, Entry>> = emptyList(),
        val failure: SourceMigrationSourceFailure? = null,
    )

    private companion object {
        const val DEFAULT_CONCURRENT_SOURCES = 4
    }
}
