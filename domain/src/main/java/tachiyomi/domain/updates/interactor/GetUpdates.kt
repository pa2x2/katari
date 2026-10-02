package tachiyomi.domain.updates.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.updates.model.UpdatesFeed
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.repository.UpdatesRepository
import kotlin.time.Instant

class GetUpdates(
    private val repository: UpdatesRepository,
    private val hiddenSourceIds: HiddenSourceIds,
) {

    suspend fun await(read: Boolean, after: Long): List<UpdatesWithRelations> {
        return filterHiddenSources(
            repository.awaitWithRead(read, after, limit = 500),
            hiddenSourceIds.get(),
        )
    }

    fun subscribeFeed(profileId: Long, after: Instant): Flow<UpdatesFeed> {
        return combine(
            repository.subscribeFeed(profileId, after.toEpochMilliseconds()),
            hiddenSourceIds.subscribe(profileId),
        ) { rows, hiddenSources ->
            val (fromHiddenSources, visible) = rows.partition { it.update.sourceId in hiddenSources }
            UpdatesFeed(rows = visible, fromHiddenSources = fromHiddenSources.size)
        }
    }

    fun subscribe(read: Boolean, after: Long): Flow<List<UpdatesWithRelations>> {
        return combine(
            repository.subscribeWithRead(read, after, limit = 500),
            hiddenSourceIds.subscribe(),
            ::filterHiddenSources,
        )
    }

    private fun filterHiddenSources(
        updates: List<UpdatesWithRelations>,
        hiddenSources: Set<Long>,
    ): List<UpdatesWithRelations> {
        return updates.filterNot { it.sourceId in hiddenSources }
    }
}
