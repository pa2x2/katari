package app.katari.sync.server.live

import app.katari.sync.protocol.device.DeviceId
import java.util.concurrent.ConcurrentHashMap

/** The devices whose live channel is open; a device can hold two for a moment while a new one replaces the old. */
class ConnectedDevices {
    private val channels = ConcurrentHashMap<DeviceId, Int>()

    fun opened(device: DeviceId) {
        channels.merge(device, 1, Int::plus)
    }

    fun closed(device: DeviceId) {
        channels.computeIfPresent(device) { _, count -> (count - 1).takeIf { it > 0 } }
    }

    fun isConnected(device: DeviceId): Boolean = channels.containsKey(device)
}
