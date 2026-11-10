package app.katari.sync.client.extension

import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.kind.extension.DeviceInstall
import app.katari.sync.client.kind.extension.InstalledExtensionKind
import app.katari.sync.client.kind.extension.InstalledExtensionPayload
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.data.DatabaseHandler

/**
 * The extensions whose newest install or uninstall, of all the account's devices, was made on another device: what
 * this device is offered to do too. A change this device made since, either way, outweighs it. Devices no longer in
 * the account count for nothing, and nothing is offered while this device doesn't sync installed extensions.
 */
@Inject
class OtherDeviceExtensions(
    private val handler: DatabaseHandler,
    private val connection: SyncConnection,
    private val devices: DeviceDirectory,
    private val preferences: SyncPreferences,
) {
    fun subscribe(): Flow<List<OtherDeviceInstall>> = combine(
        connection.states(),
        preferences.choicesChanges(),
        handler.subscribeToList { sync_recordsQueries.byKind(InstalledExtensionKind.name) },
    ) { state, choices, rows ->
        val thisDevice = state.takeIf { it.isLinked }?.device ?: return@combine emptyList()
        if (!choices.syncs(DataGroup.INSTALLED_EXTENSIONS)) return@combine emptyList()
        val names = devices.listed()
        rows
            .filter { !it.deleted && InstalledExtensionKind.reads(it.kind_version.toInt()) }
            .mapNotNull { row ->
                val payload = InstalledExtensionKind.decode(row.payload)
                val newest = payload.newestElsewhere(thisDevice, names.keys) ?: return@mapNotNull null
                OtherDeviceInstall(
                    pkgName = payload.pkgName,
                    installed = newest.installed,
                    deviceName = names.getValue(newest.device),
                    changedAt = newest.changedAt,
                )
            }
    }
        .distinctUntilChanged()
}

/** The newest install or uninstall of an extension, made on the device named [deviceName]. */
data class OtherDeviceInstall(
    val pkgName: String,
    val installed: Boolean,
    val deviceName: String,
    /** Epoch milliseconds; 0 when the device had the extension before it kept its installs. */
    val changedAt: Long,
)

/** The newest share of this device and of [accountDevices], when it isn't [thisDevice]'s. */
internal fun InstalledExtensionPayload.newestElsewhere(
    thisDevice: DeviceId,
    accountDevices: Set<DeviceId>,
): DeviceInstall? =
    devices
        .filter { it.device == thisDevice || it.device in accountDevices }
        .maxWithOrNull(DeviceInstall.ORDER)
        ?.takeIf { it.device != thisDevice }
