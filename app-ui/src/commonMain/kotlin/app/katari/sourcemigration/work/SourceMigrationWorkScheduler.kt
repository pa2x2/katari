package app.katari.sourcemigration.work

import app.katari.sourcemigration.session.model.SourceMigrationDiscoveryDepth
import app.katari.sourcemigration.session.model.SourceMigrationSessionId

/**
 * Moves a Source Migration session through its background stages: finding candidates for its entries, then migrating
 * them. Each start returns false, or a result other than started, when the session's stage doesn't allow it.
 */
interface SourceMigrationWorkScheduler {
    suspend fun startDiscovery(sessionId: SourceMigrationSessionId): Boolean

    suspend fun pauseDiscovery(sessionId: SourceMigrationSessionId)

    /** Drops a session that hasn't started migrating, with its queued work; false once migration has started. */
    suspend fun discardPreparation(sessionId: SourceMigrationSessionId): Boolean

    suspend fun restartItemDiscovery(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        depth: SourceMigrationDiscoveryDepth,
    ): Boolean

    suspend fun restartUnresolvedDiscovery(
        sessionId: SourceMigrationSessionId,
        depth: SourceMigrationDiscoveryDepth,
    ): Boolean

    suspend fun startExecution(sessionId: SourceMigrationSessionId): SourceMigrationExecutionStartResult

    suspend fun pauseExecution(sessionId: SourceMigrationSessionId)
}
