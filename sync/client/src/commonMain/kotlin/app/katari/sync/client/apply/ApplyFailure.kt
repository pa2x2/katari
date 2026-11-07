package app.katari.sync.client.apply

/**
 * Applying the record [key] threw [cause] at [stage]. [SyncWriting.writeHoldingFailures] runs the transaction again
 * with the record held aside (D067).
 */
internal class RecordApplyFailure(
    val key: String,
    val stage: FailedStage,
    cause: Throwable,
) : Exception("Applying sync record $key failed", cause)

/** How far a failed apply got, which says what the next attempt can still do with the record. */
internal enum class FailedStage {
    /** Before the merged copy was stored: the record is held as received and nothing of it is kept. */
    STORING,

    /** Writing its rows: its merged copy is stored, and only the rows wait, as for a missing parent. */
    WRITING,
}
