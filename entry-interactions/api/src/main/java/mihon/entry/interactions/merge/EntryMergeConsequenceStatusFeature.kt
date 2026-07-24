package mihon.entry.interactions

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
