package tachiyomi.data.updates

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.core.common.util.lang.toLong
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.repository.UpdatesRepository

@OptIn(ExperimentalCoroutinesApi::class)
class UpdatesRepositoryImpl(
    private val databaseHandler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : UpdatesRepository {

    override suspend fun awaitWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): List<UpdatesWithRelations> {
        return databaseHandler.awaitList {
            updatesViewQueries.getUpdatesByReadStatus(
                profileId = profileProvider.activeProfileId,
                read = read,
                after = after,
                limit = limit,
                mapper = UpdatesMapper::mapUpdatesWithRelations,
            )
        }
    }

    override fun subscribeAll(
        after: Long,
        limit: Long,
        unread: Boolean?,
        started: Boolean?,
        bookmarked: Boolean?,
        hideExcludedScanlators: Boolean,
    ): Flow<List<UpdatesWithRelations>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            databaseHandler.subscribeToList {
                updatesViewQueries.getRecentUpdatesWithFilters(
                    profileId = profileId,
                    after = after,
                    limit = limit,
                    read = unread?.let { !it },
                    started = started?.toLong(),
                    bookmarked = bookmarked,
                    hideExcludedScanlators = hideExcludedScanlators.toLong(),
                    mapper = UpdatesMapper::mapUpdatesWithRelations,
                )
            }
        }
    }

    override fun subscribeWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): Flow<List<UpdatesWithRelations>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            databaseHandler.subscribeToList {
                updatesViewQueries.getUpdatesByReadStatus(
                    profileId = profileId,
                    read = read,
                    after = after,
                    limit = limit,
                    mapper = UpdatesMapper::mapUpdatesWithRelations,
                )
            }
        }
    }
}
