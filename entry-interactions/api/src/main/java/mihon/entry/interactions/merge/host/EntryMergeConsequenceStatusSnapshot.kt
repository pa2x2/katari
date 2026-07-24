package mihon.entry.interactions.host

data class EntryMergeConsequenceStatusSnapshot(
    val pendingCount: Long,
    val failedCount: Long,
    val lastFailure: String?,
)
