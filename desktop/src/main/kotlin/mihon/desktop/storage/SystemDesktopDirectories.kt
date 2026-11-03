package mihon.desktop.storage

import mihon.desktop.application.DesktopAppVariant
import mihon.desktop.platform.DesktopOperatingSystem

/** The [DesktopDirectories] of [operatingSystem], this build's own. */
fun systemDesktopDirectories(operatingSystem: DesktopOperatingSystem): DesktopDirectories {
    return when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> XdgDesktopDirectories(DesktopAppVariant.current.directoryName)
    }
}
