package tachiyomi.data.entry

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.query.chunkedForSqlQuery
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository

@Inject
@SingleIn(AppScope::class)
class MergedMemberExpansionRepositoryImpl(
    private val handler: DatabaseHandler,
) : MergedMemberExpansionRepository {

    override fun subscribeExpanded(entryIds: Collection<Long>): Flow<Set<Long>> {
        return handler.subscribeToList { expanded_merge_membersQueries.getByEntryIds(entryIds) }
            .map(List<Long>::toSet)
    }

    override suspend fun getExpanded(entryIds: Collection<Long>): Set<Long> {
        return entryIds.distinct().chunkedForSqlQuery().flatMapTo(mutableSetOf()) { chunk ->
            handler.awaitList { expanded_merge_membersQueries.getByEntryIds(chunk) }
        }
    }

    override suspend fun setExpanded(entryId: Long, expanded: Boolean) {
        handler.await {
            if (expanded) {
                expanded_merge_membersQueries.insert(entryId)
            } else {
                expanded_merge_membersQueries.delete(entryId)
            }
        }
    }
}
