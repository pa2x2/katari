package app.katari.sync.ui.handoff

import app.katari.sync.client.handoff.HandOffCheck
import app.katari.sync.client.handoff.ProgressArrival
import kotlin.time.Duration.Companion.minutes

/** What Resume says about the progress it continues with (D052). */
sealed interface HandOffNote {
    /** The entry's progress is being fetched from the other devices; Resume works meanwhile. */
    data object Checking : HandOffNote

    /** Newer progress came from [deviceName] (null when the server couldn't name it), made at [atMillis]. */
    data class From(val deviceName: String?, val atMillis: Long) : HandOffNote

    /** The server couldn't be reached, so the progress is this device's own. */
    data object Own : HandOffNote

    companion object {
        /** How long Resume names the device newer progress came from. */
        val FROM_SHOWN = 1.minutes

        /**
         * The note for [check], or for [arrival] when newer progress came while the entry was open; the device is
         * named only until [FROM_SHOWN] after it was taken in at [arrivedAtMillis].
         */
        fun of(check: HandOffCheck?, arrival: ProgressArrival?, arrivedAtMillis: Long, nowMillis: Long): HandOffNote? {
            val newest = arrival ?: (check as? HandOffCheck.Checked)?.arrival
            return when {
                check == HandOffCheck.Checking -> Checking
                newest != null && nowMillis - arrivedAtMillis < FROM_SHOWN.inWholeMilliseconds ->
                    From(newest.deviceName, newest.editedAt)
                check == HandOffCheck.Unreachable -> Own
                else -> null
            }
        }
    }
}
