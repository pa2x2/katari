package app.katari.sync.server.record

import app.katari.sync.protocol.pull.GroupRecords
import app.katari.sync.server.storage.ServerStorage

/**
 * Hands out one group's records ahead of a device's position. Unlike a pull it records no position, since the device
 * has not seen what comes before them.
 */
class GroupPull(private val storage: ServerStorage) {
    fun of(accountId: Long, group: String): GroupRecords = storage.transaction {
        GroupRecords(
            records = storage.records.group(accountId, group, MAX_RECORDS),
            server = storage.identityOf(accountId),
        )
    }

    private companion object {
        // An entry's records: itself, its read or bookmarked chapters, their progress and history. A group past this
        // is not one a person is about to look at, and the pull that follows brings the rest.
        const val MAX_RECORDS = 10_000
    }
}
