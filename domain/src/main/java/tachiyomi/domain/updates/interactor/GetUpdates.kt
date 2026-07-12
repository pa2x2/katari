package tachiyomi.domain.updates.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.repository.UpdatesRepository
import java.time.Instant

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

    fun subscribe(
        instant: Instant,
        unread: Boolean?,
        started: Boolean?,
        bookmarked: Boolean?,
        hideExcludedScanlators: Boolean,
    ): Flow<List<UpdatesWithRelations>> {
        return combine(
            repository.subscribeAll(
                instant.toEpochMilli(),
                limit = 500,
                unread = unread,
                started = started,
                bookmarked = bookmarked,
                hideExcludedScanlators = hideExcludedScanlators,
            ),
            hiddenSourceIds.subscribe(),
            ::filterHiddenSources,
        )
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
