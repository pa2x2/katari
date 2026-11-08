package app.katari.sync.client.outbox

import app.katari.sync.protocol.clock.HybridTimestamp
import app.katari.sync.protocol.device.DeviceId

/**
 * Gives outbox rows their hybrid timestamps, in outbox order. A row is stamped with the time the edit was made, not
 * the time it is drained, so an edit made offline for days still loses to an edit made elsewhere in between. Like
 * [app.katari.sync.protocol.clock.HybridLogicalClock], it never repeats or goes backwards, and a stamp sorts after
 * what the device had seen from other devices when the edit was made (the row's `seen`), so an edit made after seeing
 * another device's edit wins over it. Unlike that clock, it does not move past what the device has seen since the
 * edit: those are later edits than this one.
 */
internal class EditStamper(
    private val device: DeviceId,
    issuedPhysical: Long,
    issuedCounter: Int,
) {
    var issuedPhysical: Long = issuedPhysical
        private set
    var issuedCounter: Int = issuedCounter
        private set

    fun stamp(editedAt: Long, seenPhysical: Long, seenCounter: Int): HybridTimestamp {
        val seenIsLater =
            seenPhysical > issuedPhysical || (seenPhysical == issuedPhysical && seenCounter > issuedCounter)
        val floorPhysical = if (seenIsLater) seenPhysical else issuedPhysical
        val floorCounter = if (seenIsLater) seenCounter else issuedCounter
        if (editedAt > floorPhysical) {
            issuedPhysical = editedAt
            issuedCounter = 0
        } else {
            issuedPhysical = floorPhysical
            issuedCounter = floorCounter + 1
        }
        return HybridTimestamp(issuedPhysical, issuedCounter, device)
    }
}
