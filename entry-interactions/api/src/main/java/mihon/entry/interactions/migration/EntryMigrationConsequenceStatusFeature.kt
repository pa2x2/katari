package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow

interface EntryMigrationConsequenceStatusFeature {
    fun observeStatus(): Flow<EntryMigrationConsequenceStatus>

    suspend fun retryPending(): EntryMigrationConsequenceStatus
}

data class EntryMigrationConsequenceStatus(
    val pendingCount: Long,
    val failedCount: Long,
    val lastFailure: String?,
)
