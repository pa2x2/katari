package mihon.entry.interactions.merge

import kotlinx.coroutines.flow.Flow

interface EntryMergeConsequenceStatusFeature {
    fun observeStatus(): Flow<EntryMergeConsequenceStatus>

    suspend fun retryPending(): EntryMergeConsequenceStatus
}

data class EntryMergeConsequenceStatus(
    val pendingCount: Long,
    val failedCount: Long,
    val lastFailure: String?,
)
