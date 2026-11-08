package app.katari.sync.client.account

import app.katari.sync.client.server.SyncFailure
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.LinkedDevice
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The account's devices as the server last listed them, so progress from another device can name it without asking
 * the server each time. A device not in the list is looked up once more, as it may have joined since; one that still
 * isn't listed then, such as a device that has left the account, isn't asked about again until the list is fetched
 * for another reason, since a pull can bring its progress for many entries on every page.
 */
@Inject
@SingleIn(AppScope::class)
class DeviceDirectory {
    @Volatile
    private var known: Map<DeviceId, LinkedDevice> = emptyMap()

    @Volatile
    private var unlisted: Set<DeviceId> = emptySet()

    fun remember(devices: List<LinkedDevice>) {
        known = devices.associateBy { it.id }
        unlisted = emptySet()
    }

    /** The name of [device], listing the account's devices again with [client] when it isn't known yet. */
    suspend fun nameOf(device: DeviceId, client: SyncServerClient): String? {
        known[device]?.let { return it.description.name }
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

    fun forget() {
        known = emptyMap()
        unlisted = emptySet()
    }
}
