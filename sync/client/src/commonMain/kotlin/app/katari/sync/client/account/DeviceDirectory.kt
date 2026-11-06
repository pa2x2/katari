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
 * the server each time. A device not in the list is looked up once more, as it may have joined since.
 */
@Inject
@SingleIn(AppScope::class)
class DeviceDirectory {
    @Volatile
    private var known: Map<DeviceId, LinkedDevice> = emptyMap()

    fun remember(devices: List<LinkedDevice>) {
        known = devices.associateBy { it.id }
    }

    /** The name of [device], listing the account's devices again with [client] when it isn't known yet. */
    suspend fun nameOf(device: DeviceId, client: SyncServerClient): String? {
        known[device]?.let { return it.description.name }
        return try {
            client.devices().devices.also(::remember).firstOrNull { it.id == device }?.description?.name
        } catch (_: SyncFailure) {
            null
        }
    }

    fun forget() {
        known = emptyMap()
    }
}
