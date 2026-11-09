package app.katari.sync.client.libraryupdate

import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.kind.libraryupdate.LibraryUpdateKind
import app.katari.sync.client.record.LocalRef
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.library.update.model.LibraryUpdateRunSummary

/** The newest update of a profile's whole library when another device of the account made it. */
@Inject
class OtherDeviceLibraryUpdates(
    private val handler: DatabaseHandler,
    private val connection: SyncConnection,
    private val devices: DeviceDirectory,
) {
    /** Null while the newest update is this device's, or this device doesn't sync. */
    fun subscribe(profileId: Long): Flow<OtherDeviceLibraryUpdate?> {
        val records = handler.subscribeToOneOrNull {
            sync_recordsQueries.getByLocalRef(LibraryUpdateKind.name, LocalRef.of(profileId).json)
        }
        return combine(connection.states(), records) { state, row ->
            val thisDevice = state.takeIf { it.isLinked }?.device ?: return@combine null
            if (row == null || row.deleted || !LibraryUpdateKind.reads(row.kind_version.toInt())) return@combine null
            val update = LibraryUpdateKind.decode(row.payload).latest.value
            if (update.device == thisDevice) return@combine null
            OtherDeviceLibraryUpdate(
                deviceName = devices.listedName(update.device),
                summary = update.summary() ?: return@combine null,
            )
        }
            .distinctUntilChanged()
    }
}

data class OtherDeviceLibraryUpdate(
    /** As the account last listed the device; null when it wasn't listed, such as after it left the account. */
    val deviceName: String?,
    val summary: LibraryUpdateRunSummary,
)
