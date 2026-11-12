package app.katari.sync.client.downloadrequest

import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.kind.extension.DeviceInstall
import app.katari.sync.client.kind.extension.InstalledExtensionKind
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.LinkedDevice
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.data.DatabaseHandler

/** One of the account's other devices, as a download request could go to it. */
data class DownloadTarget(val device: LinkedDevice, val readiness: DownloadTargetReadiness)

enum class DownloadTargetReadiness {
    READY,

    /** Its app doesn't take download requests yet. */
    NEEDS_NEWER_APP,

    /** It keeps its installs, and the entry's extension isn't among them. */
    MISSING_EXTENSION,
}

/**
 * The account's devices other than this one, in the account's order, as download requests could go to them. Whether a
 * device has the entry's extension is known only while this device syncs installed extensions, and only of devices
 * that keep theirs, which every one of them does that appears in any extension's installs: others count as ready.
 */
@Inject
class DownloadTargets(
    private val connection: SyncConnection,
    private val directory: DeviceDirectory,
    private val handler: DatabaseHandler,
    private val preferences: SyncPreferences,
) {
    /** None while this device isn't linked. [pkgName] is the entry's extension, null when none provides it. */
    fun of(pkgName: String?): Flow<List<DownloadTarget>> = combine(
        connection.states(),
        directory.devices(),
        preferences.choicesChanges(),
        handler.subscribeToList { sync_recordsQueries.byKind(InstalledExtensionKind.name) },
    ) { state, devices, choices, installRows ->
        val thisDevice = state.takeIf { it.isLinked }?.device ?: return@combine emptyList()
        val installs = if (pkgName != null && choices.syncs(DataGroup.INSTALLED_EXTENSIONS)) {
            installRows
                .filter { !it.deleted && InstalledExtensionKind.reads(it.kind_version.toInt()) }
                .map { InstalledExtensionKind.decode(it.payload) }
        } else {
            emptyList()
        }
        val keepers = installs.flatMapTo(mutableSetOf()) { payload -> payload.devices.map { it.device } }
        val withExtension = installs.firstOrNull { it.pkgName == pkgName }?.devices.orEmpty()
            .groupBy { it.device }
            .filterValues { it.maxWith(DeviceInstall.ORDER).installed }
            .keys
        devices.filter { it.id != thisDevice }.map { device ->
            DownloadTarget(device, readiness(device, keepers, withExtension))
        }
    }.distinctUntilChanged()

    private fun readiness(device: LinkedDevice, keepers: Set<DeviceId>, withExtension: Set<DeviceId>) = when {
        DownloadRequestMessage.KIND !in device.description.messageKinds -> DownloadTargetReadiness.NEEDS_NEWER_APP
        device.id in keepers && device.id !in withExtension -> DownloadTargetReadiness.MISSING_EXTENSION
        else -> DownloadTargetReadiness.READY
    }
}
