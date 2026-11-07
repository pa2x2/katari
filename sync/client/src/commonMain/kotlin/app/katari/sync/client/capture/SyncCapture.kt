package app.katari.sync.client.capture

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.file.capture.FileCapture
import app.katari.sync.client.setting.capture.SettingsCapture
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mihon.data.database.TemporarySchema
import tachiyomi.data.DatabaseHandler

/**
 * Turns change capture on and off with sync, of the database, of preferences ([SettingsCapture]) and of custom covers
 * and fonts ([FileCapture]). The state is kept in `sync_state`, and a process that starts with sync on captures again
 * from [resume].
 */
@Inject
@SingleIn(AppScope::class)
class SyncCapture(
    private val handler: DatabaseHandler,
    private val temporarySchema: TemporarySchema,
    private val settings: SettingsCapture,
    private val files: FileCapture,
) {
    /**
     * Starts capturing. Edits made before now are not in the outbox, and rows a connection still wrote after the last
     * [stop] are dropped: what the device holds when sync starts reaches the server with the first sync instead.
     */
    suspend fun start() {
        handler.await(inTransaction = true) {
            sync_outboxQueries.clear()
            sync_stateQueries.setEnabled(true)
        }
        temporarySchema.publish(CaptureTriggers.install)
        settings.start()
        files.start()
    }

    /**
     * Captures again after a process start when sync is on. It runs once the app's upgrade migrations are done, so
     * their writes to the database and preferences are never edits; writes before it returns are not captured.
     */
    suspend fun resume() {
        if (!handler.await { sync_stateQueries.get().awaitAsOne().enabled }) return
        temporarySchema.publish(CaptureTriggers.install)
        settings.start()
        files.start()
    }

    /** Stops capturing and drops edits not drained yet: with sync off they will never be sent. */
    suspend fun stop() {
        files.stop()
        settings.stop()
        temporarySchema.publish(CaptureTriggers.remove)
        handler.await(inTransaction = true) {
            sync_stateQueries.setEnabled(false)
            sync_outboxQueries.clear()
        }
    }
}
