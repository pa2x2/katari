package app.katari.sync.client.capture

import app.katari.sync.client.setting.capture.SettingsCapture
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mihon.data.database.TemporarySchema
import tachiyomi.data.DatabaseHandler

/**
 * Turns change capture on and off with sync, of the database and of preferences ([SettingsCapture]). The state is kept
 * in `sync_state`, so database capture comes back on by itself when the database next opens
 * ([SyncCaptureSchemaSource]), and preference capture when the process starts ([SettingsCapture.resume]).
 */
@Inject
@SingleIn(AppScope::class)
class SyncCapture(
    private val handler: DatabaseHandler,
    private val temporarySchema: TemporarySchema,
    private val settings: SettingsCapture,
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
    }

    /** Stops capturing and drops edits not drained yet: with sync off they will never be sent. */
    suspend fun stop() {
        settings.stop()
        temporarySchema.publish(CaptureTriggers.remove)
        handler.await(inTransaction = true) {
            sync_stateQueries.setEnabled(false)
            sync_outboxQueries.clear()
        }
    }
}
