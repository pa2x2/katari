package app.katari.sync.client.account

import app.katari.sync.client.capture.SyncCapture
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.handoff.EntryHandOff
import app.katari.sync.client.server.SyncFailure
import app.katari.sync.client.server.SyncServerClients
import dev.zacsweers.metro.Inject
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException

/**
 * Ends this device's part in its account (D030), whether the person disconnected it or it was removed elsewhere:
 * capture stops, the token and the connection are forgotten, along with the sync bookkeeping that only meant
 * something on that server, and every row and preference stays. The app is standalone again; [reason] stays for the
 * screens to say why, until dismissed or the device signs in again.
 *
 * A device the person disconnects leaves at once and tells its server afterwards, so leaving never waits for the
 * server. Until the server hears of it, the device keeps the registration it left by and its token: left there, the
 * registration would stay in the other devices' lists and download pickers, and would hold back the server's clean-up
 * of deletions for months.
 */
@Inject
class Disconnection(
    private val capture: SyncCapture,
    private val connection: SyncConnection,
    private val handOff: EntryHandOff,
    private val devices: DeviceDirectory,
    private val clients: SyncServerClients,
) {
    internal suspend fun leave(reason: SyncStop?) {
        end(reason, tellServerLater = false)
    }

    /** The person disconnected the device: it leaves, and tells its server so now or once it can ([tellServer]). */
    internal suspend fun disconnect() {
        end(reason = null, tellServerLater = true)
        tellServer()
    }

    /**
     * Tells the server of the registration this device left by while it couldn't hear of it, if any. One the server
     * no longer knows, and one this device can no longer sign for, need nothing more; a server that can't be reached,
     * or a token that can't be read now, leave it for the next try. Runs with no sync run going, since a device
     * registering anew replaces the token.
     */
    internal suspend fun tellServer() {
        val state = connection.state()
        val device = state.leftDevice ?: return
        val server = state.leftServer
        val token = try {
            connection.token()
        } catch (e: IOException) {
            logcat(LogPriority.INFO, e) { "Can't read the sync device token to tell the server this device left" }
            return
        }
        if (server != null && token != null) {
            try {
                clients.of(server, token).revoke(device)
            } catch (e: SyncFailure.Refused) {
                // Removed already, or a token the server doesn't take: nothing this device can tell it any more
                logcat(LogPriority.INFO, e) { "The sync server refused to remove this device's registration" }
            } catch (e: SyncFailure) {
                logcat(LogPriority.INFO, e) { "Couldn't tell the sync server this device left; telling it later" }
                return
            }
        }
        connection.forgetLeft()
    }

    private suspend fun end(reason: SyncStop?, tellServerLater: Boolean) {
        capture.stop()
        connection.forget(reason, tellServerLater)
        handOff.forget()
        devices.forget()
    }
}
