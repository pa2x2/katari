package tachiyomi.domain.updates.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.model.UpdatesFeedRow
import tachiyomi.domain.updates.model.UpdatesWithRelations

interface UpdatesRepository {

    suspend fun awaitWithRead(read: Boolean, after: Long, limit: Long): List<UpdatesWithRelations>

    fun subscribeFeed(profileId: Long, after: Long): Flow<List<UpdatesFeedRow>>

    fun subscribeWithRead(read: Boolean, after: Long, limit: Long): Flow<List<UpdatesWithRelations>>

    fun subscribeWithRead(
        profileId: Long,
        read: Boolean,
        after: Long,
        limit: Long,
    ): Flow<List<UpdatesWithRelations>>
}
