package app.katari.desktop.security.fprintd

import app.katari.desktop.dbus.DbusConnection
import app.katari.desktop.dbus.DbusException
import app.katari.desktop.security.signin.FingerprintEvent
import app.katari.desktop.security.signin.FingerprintReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Linux: the system's default fingerprint reader, read through fprintd on the system bus, which desktops' lock screens
 * and PAM's `pam_fprintd` use too. fprintd lets the active session's user read their own finger without a password.
 * The reader stays claimed by the app's connection while it reads, and fprintd lets it go once the connection closes.
 */
internal class FprintdFingerprintReader : FingerprintReader {
    override fun read(): Flow<FingerprintEvent> = callbackFlow {
        val stopped = AtomicBoolean(false)
        val connection = DbusConnection.system()
        if (connection != null) {
            launch(Dispatchers.IO) {
                connection.use { FprintdSession(it, stopped).read { event -> trySendBlocking(event) } }
                channel.close()
            }
        } else {
            channel.close()
        }
        awaitClose {
            stopped.set(true)
            connection?.wakeUp()
        }
    }
}

/** One read of the reader over [connection], until the finger is recognized, the reader fails, or [stopped]. */
private class FprintdSession(private val connection: DbusConnection, private val stopped: AtomicBoolean) {
    fun read(report: (FingerprintEvent) -> Unit) {
        try {
            val device = connection.call(FPRINTD, MANAGER_PATH, MANAGER, "GetDefaultDevice").first() as String
            // fprintd answers its own signals; anyone else on the bus may send the app one too
            val fprintd = connection.call(BUS_NAME, BUS_PATH, BUS_NAME, "GetNameOwner", "s", FPRINTD).first() as String
            // Fails with NoEnrolledPrints while the user has none
            connection.call(FPRINTD, device, DEVICE, "ListEnrolledFingers", "s", CURRENT_USER)
            connection.addMatch(
                "type='signal',sender='$FPRINTD',path='$device',interface='$DEVICE',member='VerifyStatus'",
            )
            connection.call(FPRINTD, device, DEVICE, "Claim", "s", CURRENT_USER)
            try {
                verify(device, fprintd, report)
            } finally {
                connection.call(FPRINTD, device, DEVICE, "Release")
            }
        } catch (e: DbusException) {
            logcat(LogPriority.INFO) { "The fingerprint reader isn't available: ${e.message}" }
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Lost fprintd" }
        }
    }

    private fun verify(device: String, fprintd: String, report: (FingerprintEvent) -> Unit) {
        report(FingerprintEvent.Waiting)
        while (!stopped.get()) {
            connection.call(FPRINTD, device, DEVICE, "VerifyStart", "s", ANY_FINGER)
            val result = awaitResult(device, fprintd, report)
            if (result == DISCONNECTED) return
            connection.call(FPRINTD, device, DEVICE, "VerifyStop")
            when (result) {
                MATCH -> return report(FingerprintEvent.Recognized)
                NO_MATCH -> report(FingerprintEvent.NotRecognized)
                null -> return
                else -> {
                    logcat(LogPriority.WARN) { "The fingerprint reader failed: $result" }
                    return
                }
            }
        }
    }

    /**
     * The result that ends the verification fprintd started, after reporting the ones that ask for the finger again;
     * null once [stopped], with the verification still running.
     */
    private fun awaitResult(device: String, fprintd: String, report: (FingerprintEvent) -> Unit): String? {
        while (!stopped.get()) {
            val signal = connection.nextSignal(SIGNAL_WAIT) ?: continue
            if (signal.sender != fprintd || signal.path != device || signal.member != "VerifyStatus") continue
            val result = signal.body[0] as String
            val done = signal.body[1] as Boolean
            if (done) return result
            report(FingerprintEvent.ReadAgain)
        }
        return null
    }

    private companion object {
        const val FPRINTD = "net.reactivated.Fprint"
        const val MANAGER = "net.reactivated.Fprint.Manager"
        const val MANAGER_PATH = "/net/reactivated/Fprint/Manager"
        const val DEVICE = "net.reactivated.Fprint.Device"
        const val BUS_NAME = DbusConnection.BUS_NAME
        const val BUS_PATH = DbusConnection.BUS_PATH

        // fprintd's names for the user calling it and for whichever enrolled finger touches the reader
        const val CURRENT_USER = ""
        const val ANY_FINGER = "any"

        const val MATCH = "verify-match"
        const val NO_MATCH = "verify-no-match"

        // The reader is gone, and fprintd wants no more calls about it
        const val DISCONNECTED = "verify-disconnected"

        // How long a wait for a signal lasts before it looks at [stopped] again: a wake-up that comes while a call
        // waits for its answer ends that wait instead
        val SIGNAL_WAIT = 5.seconds
    }
}
