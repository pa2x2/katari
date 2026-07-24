package eu.kanade.tachiyomi.data.entry

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mihon.entry.interactions.EntryChildGroupFilterDataSource
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository

class AppEntryChildGroupFilterDataSource(
    private val entryChapterRepository: EntryChapterRepository,
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : EntryChildGroupFilterDataSource {
    override fun childrenChanged(entryIds: Collection<Long>): Flow<Unit> {
        return entryIds.distinct()
            .map(entryChapterRepository::getChaptersByEntryId)
            .merge()
            .map { Unit }
    }

    override suspend fun children(entryIds: Collection<Long>): List<EntryChapter> {
        return entryIds.distinct().flatMap { entryId ->
            entryChapterRepository.getChaptersByEntryIdAwait(entryId, applyScanlatorFilter = false)
        }
    }

    override fun excludedGroupsChanged(profileId: Long?, entryIds: Collection<Long>): Flow<Unit> {
        return profileIdFlow(profileId).flatMapLatest { resolvedProfileId ->
            entryIds.distinct()
                .map { entryId ->
                    handler.subscribeToList {
                        excluded_scanlatorsQueries.getExcludedScanlatorsByEntryId(resolvedProfileId, entryId)
                    }
                }
                .merge()
                .map { Unit }
        }
    }

    override suspend fun excludedGroups(profileId: Long?, entryIds: Collection<Long>): Map<Long, Set<String>> {
        val resolvedProfileId = profileId ?: profileProvider.activeProfileId
        return entryIds.distinct().associateWith { entryId ->
            handler.awaitList {
                excluded_scanlatorsQueries.getExcludedScanlatorsByEntryId(resolvedProfileId, entryId)
            }.toSet()
        }
    }

    override suspend fun setExcludedGroups(
        profileId: Long?,
        entryIds: Collection<Long>,
        excluded: Set<String>,
    ) {
        val resolvedProfileId = profileId ?: profileProvider.activeProfileId
        handler.await(inTransaction = true) {
            entryIds.distinct().forEach { entryId ->
                val current = handler.awaitList {
                    excluded_scanlatorsQueries.getExcludedScanlatorsByEntryId(resolvedProfileId, entryId)
                }.toSet()
                excluded.minus(current).forEach { group ->
                    excluded_scanlatorsQueries.insert(resolvedProfileId, entryId, group)
                }
                excluded_scanlatorsQueries.remove(resolvedProfileId, entryId, current.minus(excluded))
            }
        }
    }

    private fun profileIdFlow(profileId: Long?): Flow<Long> {
        return profileId?.let(::flowOf) ?: profileProvider.activeProfileIdFlow
    }
}
