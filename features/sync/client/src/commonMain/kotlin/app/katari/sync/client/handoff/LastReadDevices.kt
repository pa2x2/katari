package app.katari.sync.client.handoff

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.kind.history.HistoryKind
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.record.LocalRef
import dev.zacsweers.metro.Inject
import tachiyomi.data.DatabaseHandler

/**
 * Which other device a chapter was last read on, so History can say where reading happened: the device of the newest
 * edit to the chapter's last read in its history record, as this device last merged it, named as the account last
 * listed it.
 */
@Inject
class LastReadDevices(
    private val handler: DatabaseHandler,
    private val connection: SyncConnection,
    private val devices: DeviceDirectory,
) {
    /**
     * The name of the device [chapterId] was last read on at [lastReadMillis], or null when that was this device, the
     * device isn't listed, or this device doesn't sync. A reading here that sync hasn't taken in yet leaves the record
     * at an older time than [lastReadMillis], which says the reading was this device's.
     */
    suspend fun otherDeviceName(chapterId: Long, lastReadMillis: Long): String? {
        val thisDevice = connection.state().takeIf { it.isLinked }?.device ?: return null
        val row = handler.await {
            sync_recordsQueries.getByLocalRef(HistoryKind.name, LocalRef.of(chapterId).json).awaitAsOneOrNull()
        } ?: return null
        if (row.deleted || !HistoryKind.reads(row.kind_version.toInt())) return null
        val lastRead = HistoryKind.decode(row.payload).lastRead
        val device = lastRead.at.device
        if (lastRead.value != lastReadMillis || lastRead.at == Stamped.UNKNOWN_EDIT || device == thisDevice) return null
        return devices.listedName(device)
    }
}
