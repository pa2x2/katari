package app.katari.sync.client.connection

import mihon.core.common.platform.PlatformFacility

/**
 * What the platform says about this device for its account's device list. The account sees it through
 * [DeviceNaming], which puts the name the person gave the device in place of [platformName].
 */
@PlatformFacility
interface ThisDevice {
    /** What the platform calls the device, such as the phone's name in its settings or the computer's host name. */
    fun platformName(): String

    /** Free text naming the platform, such as "Android". */
    val platform: String

    /** The app's version as About names it; cheap to read, where the device's name may not be. */
    val appVersion: String
}
