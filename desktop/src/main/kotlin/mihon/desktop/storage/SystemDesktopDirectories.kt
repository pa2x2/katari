package mihon.desktop.storage

import mihon.desktop.platform.DesktopOperatingSystem

/** The [DesktopDirectories] of [operatingSystem]. */
fun systemDesktopDirectories(operatingSystem: DesktopOperatingSystem): DesktopDirectories {
    return when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> XdgDesktopDirectories()
    }
}
