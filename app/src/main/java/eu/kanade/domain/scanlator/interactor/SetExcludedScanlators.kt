package eu.kanade.domain.scanlator.interactor

import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler

class SetExcludedScanlators(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) {

    suspend fun await(entryId: Long, excludedScanlators: Set<String>) {
        handler.await(inTransaction = true) {
            val currentExcluded = handler.awaitList {
                excluded_scanlatorsQueries.getExcludedScanlatorsByEntryId(profileProvider.activeProfileId, entryId)
            }.toSet()
            val toAdd = excludedScanlators.minus(currentExcluded)
            for (scanlator in toAdd) {
                excluded_scanlatorsQueries.insert(profileProvider.activeProfileId, entryId, scanlator)
            }
            val toRemove = currentExcluded.minus(excludedScanlators)
            excluded_scanlatorsQueries.remove(profileProvider.activeProfileId, entryId, toRemove)
        }
    }
}
