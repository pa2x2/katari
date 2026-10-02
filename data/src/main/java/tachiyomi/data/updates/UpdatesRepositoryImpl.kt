package tachiyomi.data.updates

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.updates.model.UpdatesFeedRow
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

    override fun subscribeFeed(profileId: Long, after: Long): Flow<List<UpdatesFeedRow>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.getFeedUpdates(
                profileId = profileId,
                after = after,
                mapper = UpdatesMapper::mapFeedRow,
            )
        }
    }

    override fun subscribeWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): Flow<List<UpdatesWithRelations>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            subscribeWithRead(profileId, read, after, limit)
        }
    }

    override fun subscribeWithRead(
        profileId: Long,
        read: Boolean,
        after: Long,
        limit: Long,
    ): Flow<List<UpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
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
