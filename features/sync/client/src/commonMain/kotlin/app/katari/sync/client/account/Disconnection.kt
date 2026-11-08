package app.katari.sync.client.account

import app.katari.sync.client.capture.SyncCapture
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.handoff.EntryHandOff
import dev.zacsweers.metro.Inject

/**
 * Ends this device's part in its account (D030), whether the person disconnected it or it was removed elsewhere:
 * capture stops, the token and the connection are forgotten, along with the sync bookkeeping that only meant
 * something on that server, and every row and preference stays. The app is standalone again; [reason] stays for the
 * screens to say why, until dismissed or the device signs in again.
 */
@Inject
class Disconnection(
    private val capture: SyncCapture,
    private val connection: SyncConnection,
    private val handOff: EntryHandOff,
) {
    internal suspend fun leave(reason: SyncStop?) {
        capture.stop()
        connection.forget(reason)
        handOff.forget()
    }
}
