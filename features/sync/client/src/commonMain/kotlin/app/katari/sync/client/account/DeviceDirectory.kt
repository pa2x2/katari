package app.katari.sync.client.account

import app.katari.sync.client.server.SyncFailure
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.LinkedDevice
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.data.DatabaseHandler
import kotlin.concurrent.Volatile

/**
 * The account's devices as the server last listed them, kept on this device so progress from another device, and the
 * History rows read on one, can name it without asking the server. A device not in the list is looked up once more,
 * as it may have joined since; one that still isn't listed then, such as a device that has left the account, isn't
 * asked about again until the list is fetched for another reason, since a pull can bring its progress for many entries
 * on every page.
 */
@Inject
@SingleIn(AppScope::class)
class DeviceDirectory(private val handler: DatabaseHandler) {
    // Null until first read from the database
    @Volatile
    private var known: Map<DeviceId, String>? = null

    @Volatile
    private var unlisted: Set<DeviceId> = emptySet()

    suspend fun remember(devices: List<LinkedDevice>) {
        val names = devices.associate { it.id to it.description.name }
        handler.await(inTransaction = true) {
            sync_devicesQueries.clear()
            names.forEach { (device, name) -> sync_devicesQueries.insert(device.value, name) }
        }
        known = names
        unlisted = emptySet()
    }

    /** The name of [device] as the account last listed it; null when it wasn't listed. */
    suspend fun listedName(device: DeviceId): String? = names()[device]

    /** The name of [device], listing the account's devices again with [client] when it isn't known yet. */
    suspend fun nameOf(device: DeviceId, client: SyncServerClient): String? {
        names()[device]?.let { return it }
        if (device in unlisted) return null
        val listed = try {
            client.devices().devices
        } catch (_: SyncFailure) {
            return null
        }
        remember(listed)
        val found = listed.firstOrNull { it.id == device }
        if (found == null) unlisted = unlisted + device
        return found?.description?.name
    }

    suspend fun forget() {
        handler.await { sync_devicesQueries.clear() }
        known = emptyMap()
        unlisted = emptySet()
    }

    private suspend fun names(): Map<DeviceId, String> = known
        ?: handler.awaitList { sync_devicesQueries.all() }
            .associate { DeviceId(it.device_id) to it.name }
            .also { known = it }
}
