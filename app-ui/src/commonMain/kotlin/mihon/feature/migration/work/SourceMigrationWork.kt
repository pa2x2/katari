package mihon.feature.migration.work

import mihon.core.common.platform.PlatformFacility
import mihon.feature.migration.session.model.SourceMigrationSessionId
import tachiyomi.domain.work.WorkBackoff
import kotlin.time.Duration.Companion.seconds

/**
 * The platform's background work for a Source Migration session, which [SourceMigrationStages] starts and stops as the
 * session moves through its stages. Each session runs at most one discovery and one execution; starting one replaces
 * the session's earlier one of the same kind.
 */
@PlatformFacility
interface SourceMigrationWork {
    /** Finds candidates for the session's queued entries, once the device is online. */
    fun startDiscovery(sessionId: SourceMigrationSessionId)

    fun stopDiscovery(sessionId: SourceMigrationSessionId)

    /** Migrates the session's planned entries. */
    fun startExecution(sessionId: SourceMigrationSessionId)

    fun stopExecution(sessionId: SourceMigrationSessionId)

    /** Stops all of the session's work and waits until none of it is left. */
    suspend fun stopSession(sessionId: SourceMigrationSessionId)

    companion object {
        /** How long discovery and execution wait before they run again after failing. */
        val BACKOFF = WorkBackoff(initialDelay = 30.seconds)
    }
}
