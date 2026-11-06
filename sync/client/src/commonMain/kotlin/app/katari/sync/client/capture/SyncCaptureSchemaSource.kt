package app.katari.sync.client.capture

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import mihon.data.database.TemporarySchemaSource

/**
 * Installs the capture triggers on every connection when the database opens with sync on, after migrations have
 * run; with sync off nothing is installed and writes run exactly as without sync.
 */
@Inject
@ContributesBinding(AppScope::class)
class SyncCaptureSchemaSource : TemporarySchemaSource {
    override suspend fun statementsWhenOpened(driver: SqlDriver): List<String> {
        val enabled = driver.executeQuery(
            identifier = null,
            sql = "SELECT enabled FROM sync_state WHERE id = 0",
            mapper = { cursor -> QueryResult.Value(cursor.apply { next() }.getBoolean(0) == true) },
            parameters = 0,
        ).await()
        return if (enabled) CaptureTriggers.install else emptyList()
    }
}
