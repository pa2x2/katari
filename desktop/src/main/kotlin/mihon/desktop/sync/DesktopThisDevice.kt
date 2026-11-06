package mihon.desktop.sync

import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.protocol.device.DeviceDescription
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.about.versionText
import mihon.app.ui.buildinfo.AppBuild
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * The computer's host name, which is what a Linux desktop names the machine, and the app's version as About
 * names it.
 */
@Inject
class DesktopThisDevice(private val build: AppBuild) : ThisDevice {
    override fun description(): DeviceDescription = DeviceDescription(
        name = hostName() ?: "Linux",
        platform = "Linux",
        appVersion = build.versionText(),
    )

    private fun hostName(): String? {
        System.getenv("HOSTNAME")?.takeIf(String::isNotBlank)?.let { return it }
        return try {
            InetAddress.getLocalHost().hostName.takeIf(String::isNotBlank)
        } catch (_: UnknownHostException) {
            null
        }
    }
}
