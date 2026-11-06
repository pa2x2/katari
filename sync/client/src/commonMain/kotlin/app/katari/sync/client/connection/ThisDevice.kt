package app.katari.sync.client.connection

import app.katari.sync.protocol.device.DeviceDescription
import mihon.core.common.platform.PlatformFacility

/** How this device presents itself in its account's device list: its name, its platform and the app's version. */
@PlatformFacility
interface ThisDevice {
    fun description(): DeviceDescription
}
