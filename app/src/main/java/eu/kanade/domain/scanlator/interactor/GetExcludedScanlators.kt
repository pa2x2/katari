package eu.kanade.domain.scanlator.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler

class GetExcludedScanlators(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) {

    suspend fun await(entryId: Long): Set<String> {
        return handler.awaitList {
            excluded_scanlatorsQueries.getExcludedScanlatorsByEntryId(profileProvider.activeProfileId, entryId)
        }
            .toSet()
    }

    fun subscribe(entryId: Long): Flow<Set<String>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                excluded_scanlatorsQueries.getExcludedScanlatorsByEntryId(profileId, entryId)
            }
        }
            .map { it.toSet() }
    }
}
