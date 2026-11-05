package app.katari.sync.server.retention

import app.katari.sync.server.storage.ServerStorage
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Drops deletion markers once no device needs them. A device needs the markers after the position it last pulled
 * from, until it is revoked or has been silent for [DEVICE_SILENCE_LIMIT]; a device that has never pulled needs none,
 * since a new device takes the account by a pull that leaves deletions out. With no device holding them, every
 * marker goes.
 *
 * What is dropped is remembered as the account's markers-dropped-through sequence, so a device that comes back from
 * before it is sent to a full resync ([app.katari.sync.server.record.RecordPull]) instead of missing deletions. Applied
 * change ids are forgotten at the same point: every device has pulled the records they made, so a retry of one is
 * answered as stale and merged, which loses nothing.
 */
class DeletionMarkerRetention(
    private val storage: ServerStorage,
    private val clock: Clock,
) {
    fun sweep() {
        storage.accounts.ids().forEach(::sweep)
    }

    fun sweep(accountId: Long) = storage.transaction {
        val seenSince = clock.now().minus(DEVICE_SILENCE_LIMIT).toEpochMilliseconds()
        val horizon = storage.devices.oldestHeldPosition(accountId, seenSince)
            ?: storage.records.sequence(accountId)
        storage.records.dropMarkersThrough(accountId, horizon)
            ?.let { storage.records.raiseMarkersDroppedThrough(accountId, it) }
        storage.records.forgetChangesThrough(accountId, horizon)
    }

    companion object {
        val DEVICE_SILENCE_LIMIT = 180.days
    }
}
