package app.katari.sync.client.account

import app.katari.sync.client.server.SyncFailure
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.LinkedDevice
import app.katari.sync.protocol.record.SequencedRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import tachiyomi.data.DatabaseHandler
import kotlin.concurrent.Volatile

/**
 * The account's devices as the server last listed them, kept on this device so progress from another device, and the
 * History rows read on one, can name it without asking the server, and the Sync page lists them before it answers.
 * Which of them are connected now is only kept while the app runs, since it soon goes out of date. A device not in the list is looked up once more,
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

    private val connected = MutableStateFlow<Set<DeviceId>>(emptySet())

    suspend fun remember(devices: List<LinkedDevice>) {
        handler.await(inTransaction = true) {
            sync_devicesQueries.clear()
            devices.forEachIndexed { position, device ->
                sync_devicesQueries.insert(
                    deviceId = device.id.value,
                    name = device.description.name,
                    position = position.toLong(),
                    platform = device.description.platform,
                    appVersion = device.description.appVersion,
                    lastSeenAt = device.lastSeenMillis,
                    syncedUpTo = device.syncedUpTo,
                    messageKinds = device.description.messageKinds.joinToString(KINDS_SEPARATOR),
                )
            }
        }
        known = devices.associate { it.id to it.description.name }
        unlisted = emptySet()
        connected.value = devices.filter { it.connected }.mapTo(mutableSetOf()) { it.id }
    }

    /** The account's devices as the account last listed them, in its order, and each change to them. */
    fun devices(): Flow<List<LinkedDevice>> = combine(
        handler.subscribeToList { sync_devicesQueries.all() },
        connected,
    ) { rows, connected ->
        rows.map { row ->
            val id = DeviceId(row.device_id)
            LinkedDevice(
                id = id,
                description = DeviceDescription(
                    name = row.name,
                    platform = row.platform,
                    appVersion = row.app_version,
                    messageKinds = row.message_kinds.split(KINDS_SEPARATOR).filter { it.isNotEmpty() },
                ),
                lastSeenMillis = row.last_seen_at,
                syncedUpTo = row.synced_up_to,
                connected = id in connected,
            )
        }
    }

    /** Gives [device] its new [name] here before the account lists it so. */
    suspend fun rename(device: DeviceId, name: String) {
        handler.await { sync_devicesQueries.rename(name = name, deviceId = device.value) }
        known = known?.plus(device to name)
    }

    /** The account's devices and their names, as the account last listed them. */
    suspend fun listed(): Map<DeviceId, String> = names()

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

    /**
     * Learns the names of the devices other than [thisDevice] that pushed [records], so what they edited can be named
     * as they come, by a device that joined the account after this one too.
     */
    suspend fun learn(records: List<SequencedRecord>, thisDevice: DeviceId, client: SyncServerClient) {
        records.map { it.record.device }.distinct().filter { it != thisDevice }.forEach { nameOf(it, client) }
    }

    suspend fun forget() {
        handler.await { sync_devicesQueries.clear() }
        known = emptyMap()
        unlisted = emptySet()
        connected.value = emptySet()
    }

    private suspend fun names(): Map<DeviceId, String> = known
        ?: handler.awaitList { sync_devicesQueries.all() }
            .associate { DeviceId(it.device_id) to it.name }
            .also { known = it }

    private companion object {
        // The server takes no kind with a line break in it
        const val KINDS_SEPARATOR = "\n"
    }
}
