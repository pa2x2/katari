package app.katari.sync.client.handoff

import app.katari.sync.protocol.device.DeviceId

/**
 * Progress in an entry another device made that this device took in just now (D052, D053): the reader offers to go
 * there, and Resume names the device for a moment. The progress itself is in the entry's rows already; whether it is
 * further than where the person is, the reader decides.
 */
data class ProgressArrival(
    val entryId: Long,
    val device: DeviceId,
    /** As the account lists the device; null when the server couldn't be asked. */
    val deviceName: String?,
    /** Epoch milliseconds of the other device's edit. */
    val editedAt: Long,
)

/** Where checking an entry for newer progress from other devices stands (D052). */
sealed interface HandOffCheck {
    /** The entry's records are being fetched ahead of everything else. */
    data object Checking : HandOffCheck

    /** The server was asked; [arrival] is the newer progress it had, or null when this device's was the newest. */
    data class Checked(val arrival: ProgressArrival?) : HandOffCheck

    /** The server couldn't be reached, so the progress shown is this device's own. */
    data object Unreachable : HandOffCheck
}
