package app.katari.sync.protocol.clock

import app.katari.sync.protocol.device.DeviceId
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Stamps this device's field edits. Timestamps follow the wall clock but never repeat or go backwards, even when the
 * wall clock steps back, and after [receive] they sort after every timestamp the device has taken in, so an edit made
 * here after seeing another device's edit always wins over it.
 *
 * State lives in memory only. A device seeds [start] with the newest timestamp it has stored, so timestamps issued
 * after a restart still sort after the ones issued before it.
 */
@OptIn(ExperimentalAtomicApi::class)
class HybridLogicalClock(
    private val device: DeviceId,
    private val wallClock: Clock = Clock.System,
    start: HybridTimestamp? = null,
) {
    // The newest (physicalMillis, counter) issued or taken in; its device is never handed out.
    private val latest = AtomicReference(start ?: HybridTimestamp(physicalMillis = 0, device = device))

    fun now(): HybridTimestamp {
        while (true) {
            val last = latest.load()
            val wall = wallClock.now().toEpochMilliseconds()
            val next = if (wall > last.physicalMillis) {
                HybridTimestamp(physicalMillis = wall, device = device)
            } else {
                HybridTimestamp(last.physicalMillis, last.counter + 1, device)
            }
            if (latest.compareAndSet(last, next)) return next
        }
    }

    /** Takes in a timestamp from another device, so this device's next edits sort after it. */
    fun receive(remote: HybridTimestamp): Receipt {
        val ahead = (remote.physicalMillis - wallClock.now().toEpochMilliseconds()).milliseconds
        if (ahead > MAX_AHEAD) return Receipt.TooFarAhead(ahead)
        while (true) {
            val last = latest.load()
            val isNewer = remote.physicalMillis > last.physicalMillis ||
                (remote.physicalMillis == last.physicalMillis && remote.counter > last.counter)
            if (!isNewer) return Receipt.Accepted
            if (latest.compareAndSet(last, remote.copy(device = device))) return Receipt.Accepted
        }
    }

    sealed interface Receipt {
        data object Accepted : Receipt

        /**
         * [ahead] of this device's wall clock by more than [MAX_AHEAD]. The clock did not move. Hold the change that
         * carries the timestamp and offer it again later instead of dropping it: it is accepted once this device's
         * clock is within the bound, so a wrong clock delays its changes rather than losing them.
         */
        data class TooFarAhead(val ahead: Duration) : Receipt
    }

    companion object {
        /**
         * How far past the wall clock a remote timestamp may be. It is also the most a fast clock can win by: a device
         * five minutes fast beats edits made up to five minutes after its own. Devices on automatic time agree to well
         * under a second, so the bound only meets clocks set by hand or left without network time, which are commonly
         * off by a minute or two; a bound of seconds would hold back every change from such a device, and since a held
         * change waits for the receiving clock to catch up, a bound of minutes costs nothing on devices that are right.
         */
        val MAX_AHEAD: Duration = 5.minutes
    }
}
