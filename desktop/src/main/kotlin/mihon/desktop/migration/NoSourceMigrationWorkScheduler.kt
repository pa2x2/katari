package mihon.desktop.migration

import dev.zacsweers.metro.Inject
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.work.SourceMigrationExecutionStartResult
import mihon.feature.migration.work.SourceMigrationWorkScheduler

/**
 * Desktop runs no background work for Source Migration sessions yet, so a session never starts finding candidates or
 * migrating. Stands in until desktop runs background work.
 */
@Inject
class NoSourceMigrationWorkScheduler : SourceMigrationWorkScheduler {
    override suspend fun startDiscovery(sessionId: SourceMigrationSessionId): Boolean = false

    override suspend fun pauseDiscovery(sessionId: SourceMigrationSessionId) = Unit

    override suspend fun discardPreparation(sessionId: SourceMigrationSessionId): Boolean = false

    override suspend fun restartItemDiscovery(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        depth: SourceMigrationDiscoveryDepth,
    ): Boolean = false

    override suspend fun restartUnresolvedDiscovery(
        sessionId: SourceMigrationSessionId,
        depth: SourceMigrationDiscoveryDepth,
    ): Boolean = false

    override suspend fun startExecution(sessionId: SourceMigrationSessionId): SourceMigrationExecutionStartResult =
        SourceMigrationExecutionStartResult.Unavailable

    override suspend fun pauseExecution(sessionId: SourceMigrationSessionId) = Unit
}
