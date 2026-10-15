package tachiyomi.data.entry

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.DatabaseHandler
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

    override suspend fun isExpanded(entryId: Long): Boolean {
        return handler.awaitOne { expanded_merge_membersQueries.isExpanded(entryId) }
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
