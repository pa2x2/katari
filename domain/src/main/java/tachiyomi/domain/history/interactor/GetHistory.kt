package tachiyomi.domain.history.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.source.service.HiddenSourceIds

class GetHistory(
    private val repository: HistoryRepository,
    private val hiddenSourceIds: HiddenSourceIds,
) {

    suspend fun await(entryId: Long): List<History> {
        return repository.getHistoryByEntryId(entryId)
    }

    fun subscribe(query: String): Flow<List<HistoryWithRelations>> {
        return combine(
            repository.getHistory(query),
            hiddenSourceIds.subscribe(),
            ::filterHiddenSources,
        )
    }

    private fun filterHiddenSources(
        history: List<HistoryWithRelations>,
        hiddenSources: Set<Long>,
    ): List<HistoryWithRelations> {
        return history.filterNot { it.coverData.sourceId in hiddenSources }
    }
}
