package app.katari.sync.server.device

import app.katari.sync.protocol.device.DeviceDescription

/**
 * What the server keeps of how a device describes itself, on registering and each time it says so again: the fields
 * trimmed, and the app version [appVersion] in place of the device's own where it is set; null keeps each device's.
 */
class DeviceDescriptions(private val appVersion: String?) {
    fun kept(reported: DeviceDescription): DeviceDescription = reported.copy(
        name = reported.name.trim(),
        platform = reported.platform.trim(),
        appVersion = appVersion ?: reported.appVersion.trim(),
    )
}
