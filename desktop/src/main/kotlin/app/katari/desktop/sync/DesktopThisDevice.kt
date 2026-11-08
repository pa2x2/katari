package app.katari.desktop.sync

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.sync.client.connection.ThisDevice
import app.katari.ui.buildinfo.AppBuild
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.about.versionText
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * The computer's host name, which is what a Linux desktop names the machine, and the app's version as About
 * names it.
 */
@Inject
class DesktopThisDevice(
    private val build: AppBuild,
    private val operatingSystem: DesktopOperatingSystem,
) : ThisDevice {
    override fun platformName(): String = hostName() ?: platform

    override val platform: String get() = operatingSystem.displayName

    override val appVersion: String get() = build.versionText()

    private fun hostName(): String? {
        System.getenv("HOSTNAME")?.takeIf(String::isNotBlank)?.let { return it }
        return try {
            InetAddress.getLocalHost().hostName.takeIf(String::isNotBlank)
        } catch (_: UnknownHostException) {
            null
        }
    }
}
