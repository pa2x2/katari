package app.katari.entry.interactions.merge.host

data class EntryMergeConsequenceStatusSnapshot(
    val pendingCount: Long,
    val failedCount: Long,
    val lastFailure: String?,
)
